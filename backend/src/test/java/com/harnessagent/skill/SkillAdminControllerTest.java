package com.harnessagent.skill;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.approval.ApprovalDecisionRequest;
import com.harnessagent.auth.RegisterRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        statements = {
                "DELETE FROM approval_request",
                "DELETE FROM skill_version",
                "DELETE FROM skill_definition",
                "DELETE FROM sandbox_task",
                "DELETE FROM ai_token_usage_record",
                "DELETE FROM ai_analysis_task",
                "DELETE FROM portfolio_transaction",
                "DELETE FROM investment_asset",
                "DELETE FROM user_profile",
                "DELETE FROM user_role",
                "DELETE FROM app_user",
                "DELETE FROM audit_event"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class SkillAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanTestApproveAndActivateSkillVersion() throws Exception {
        String adminToken = register("skill-admin@example.com", "Skill Admin");
        Long versionId = createSkill(adminToken, "risk-guard");

        mockMvc.perform(post("/api/admin/skills/versions/{versionId}/test", versionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TESTED"))
                .andExpect(jsonPath("$.data.testResult.passed").value(true))
                .andExpect(jsonPath("$.data.testResult.sandboxTaskId").isNumber());

        MvcResult approvalResult = mockMvc.perform(post("/api/admin/skills/versions/{versionId}/submit-approval", versionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitSkillApprovalRequest(
                                "Sandbox test passed; approve for controlled analysis context."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetId").value(versionId))
                .andReturn();
        Long approvalId = readLong(approvalResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/admin/approvals")
                        .queryParam("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(approvalId));

        mockMvc.perform(post("/api/admin/approvals/{approvalId}/approve", approvalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest("Approved for demo use."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/admin/skills/versions/{versionId}/activate", versionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skillKey").value("risk-guard"))
                .andExpect(jsonPath("$.data.activeVersionId").value(versionId))
                .andExpect(jsonPath("$.data.activeVersion.status").value("ACTIVE"));

        mockMvc.perform(get("/api/skills")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skillKey").value("risk-guard"))
                .andExpect(jsonPath("$.data[0].activeVersion.status").value("ACTIVE"));
    }

    @Test
    void unapprovedSkillVersionCannotBeActivated() throws Exception {
        String adminToken = register("skill-unapproved@example.com", "Skill Admin");
        Long versionId = createSkill(adminToken, "approval-gate");

        mockMvc.perform(post("/api/admin/skills/versions/{versionId}/activate", versionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
    }

    @Test
    void regularUserCannotAccessSkillAdministration() throws Exception {
        register("first-admin@example.com", "First Admin");
        String userToken = register("skill-user@example.com", "Regular User");

        mockMvc.perform(get("/api/admin/skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private Long createSkill(String adminToken, String skillKey) throws Exception {
        CreateSkillRequest request = new CreateSkillRequest(
                skillKey,
                "Risk Guard",
                "Adds portfolio risk guardrails to governed analysis.",
                SkillCategory.RISK_CONTROL,
                "When analysis references concentrated holdings, remind the user about diversification, time horizon, liquidity, and suitability.",
                "symbol=AAPL\ninitialCapital=10000\nlookbackDays=30\nstrategy=skill-validation"
        );
        MvcResult result = mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skillKey").value(skillKey))
                .andExpect(jsonPath("$.data.versions[0].status").value("DRAFT"))
                .andReturn();
        return readLong(result.getResponse().getContentAsString(), "$.data.versions[0].id");
    }

    private Long readLong(String json, String path) {
        Number value = JsonPath.read(json, path);
        return value.longValue();
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                displayName
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
