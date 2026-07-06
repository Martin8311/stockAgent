package com.harnessagent.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.user.CapitalPurpose;
import com.harnessagent.user.InvestmentHorizon;
import com.harnessagent.user.RiskPreference;
import com.harnessagent.user.UpdateUserProfileRequest;
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
                "DELETE FROM app_user"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndManageProfile() throws Exception {
        String token = register("investor@example.com", "StrongPass123", "Investor One");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("investor@example.com"))
                .andExpect(jsonPath("$.data.riskPreference").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.roles").isArray());

        UpdateUserProfileRequest profileRequest = new UpdateUserProfileRequest(
                RiskPreference.BALANCED,
                InvestmentHorizon.LONG_TERM,
                CapitalPurpose.RETIREMENT
        );
        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskPreference").value("BALANCED"))
                .andExpect(jsonPath("$.data.investmentHorizon").value("LONG_TERM"))
                .andExpect(jsonPath("$.data.capitalPurpose").value("RETIREMENT"));

        MvcResult loginResult = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "INVESTOR@example.com",
                                "StrongPass123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        String loginToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("investor@example.com"));
    }

    @Test
    void protectedEndpointRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("duplicate@example.com", "StrongPass123", "First User");

        mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "DUPLICATE@example.com",
                                "StrongPass123",
                                "Second User"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
    }

    @Test
    void firstUserCanAccessAdminEndpointButRegularUserCannot() throws Exception {
        String adminToken = register("admin@example.com", "StrongPass123", "Admin User");
        String userToken = register("regular@example.com", "StrongPass123", "Regular User");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private String register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                password,
                                displayName
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
