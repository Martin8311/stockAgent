package com.harnessagent.sandbox;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.auth.RegisterRequest;
import com.harnessagent.portfolio.AssetType;
import com.harnessagent.portfolio.PortfolioTransactionRequest;
import com.harnessagent.portfolio.TransactionType;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
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
class SandboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void executesSafeMockBacktestAndListsTask() throws Exception {
        String token = register("sandbox-backtest@example.com");
        SandboxTaskRequest request = new SandboxTaskRequest(
                SandboxTaskType.MOCK_BACKTEST,
                "symbol=AAPL\ninitialCapital=10000\nlookbackDays=60\nstrategy=moving-average-cross",
                1200
        );

        MvcResult result = mockMvc.perform(post("/api/sandbox/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.taskType").value("MOCK_BACKTEST"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.output.metrics.symbol").value("AAPL"))
                .andExpect(jsonPath("$.data.output.riskWarnings").isArray())
                .andReturn();

        Number taskId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(get("/api/sandbox/tasks/" + taskId.longValue())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/sandbox/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void rejectsDangerousSandboxScript() throws Exception {
        String token = register("sandbox-reject@example.com");
        SandboxTaskRequest request = new SandboxTaskRequest(
                SandboxTaskType.MOCK_BACKTEST,
                "symbol=AAPL\nexec=powershell",
                1200
        );

        mockMvc.perform(post("/api/sandbox/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.data.errorMessage").isNotEmpty());
    }

    @Test
    void executesPortfolioStressTestWithCurrentHoldings() throws Exception {
        String token = register("sandbox-stress@example.com");
        mockMvc.perform(post("/api/portfolio/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PortfolioTransactionRequest(
                                "AAPL",
                                "Apple Inc.",
                                AssetType.STOCK,
                                "NASDAQ",
                                "USD",
                                TransactionType.BUY,
                                new BigDecimal("2"),
                                new BigDecimal("100"),
                                BigDecimal.ZERO,
                                Instant.parse("2026-01-01T10:00:00Z"),
                                null
                        ))))
                .andExpect(status().isOk());

        SandboxTaskRequest request = new SandboxTaskRequest(
                SandboxTaskType.PORTFOLIO_STRESS_TEST,
                "shockPercent=-0.10\nshock.AAPL=-0.20",
                1200
        );

        mockMvc.perform(post("/api/sandbox/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.output.metrics.holdingCount").value(1))
                .andExpect(jsonPath("$.data.output.metrics.perHolding.AAPL.shockPercent").value(-0.2));
    }

    @Test
    void marksHighRiskSandboxScriptForApproval() throws Exception {
        String token = register("sandbox-approval@example.com");
        SandboxTaskRequest request = new SandboxTaskRequest(
                SandboxTaskType.MOCK_BACKTEST,
                "symbol=AAPL\nleverage=2",
                1200
        );

        mockMvc.perform(post("/api/sandbox/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.approvalReason").isNotEmpty());
    }

    @Test
    void sandboxRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/sandbox/tasks"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                "Sandbox User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
