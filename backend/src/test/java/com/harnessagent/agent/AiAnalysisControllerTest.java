package com.harnessagent.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AiAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsSelectableModelsWithBillingMetadata() throws Exception {
        String token = register("ai-models@example.com");

        mockMvc.perform(get("/api/ai/models")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("ollama-qwen2.5-3b"))
                .andExpect(jsonPath("$.data[0].provider").value("OLLAMA"))
                .andExpect(jsonPath("$.data[0].freeTier").value(true))
                .andExpect(jsonPath("$.data[1].id").value("minimax-chat"))
                .andExpect(jsonPath("$.data[1].provider").value("MINIMAX"))
                .andExpect(jsonPath("$.data[1].testModeFree").value(true));
    }

    @Test
    void runsStructuredInvestmentAnalysisAndRecordsTokenUsage() throws Exception {
        String token = register("ai-analysis@example.com");

        AiAnalysisRequest request = new AiAnalysisRequest(
                "ollama-qwen2.5-3b",
                "AAPL",
                "NASDAQ",
                "USD",
                "Explain the main risks and assumptions for this holding.",
                true
        );

        mockMvc.perform(post("/api/ai/analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisId").isNumber())
                .andExpect(jsonPath("$.data.model.id").value("ollama-qwen2.5-3b"))
                .andExpect(jsonPath("$.data.quote.symbol").value("AAPL"))
                .andExpect(jsonPath("$.data.investmentSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.riskWarnings").isArray())
                .andExpect(jsonPath("$.data.tokenUsage.totalTokens").isNumber())
                .andExpect(jsonPath("$.data.tokenUsage.usageSource").value("MOCK"))
                .andExpect(jsonPath("$.data.tokenUsage.testMode").value(true))
                .andExpect(jsonPath("$.data.agentWorkflow.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.data.agentWorkflow.status").value("HUMAN_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.agentWorkflow.humanApprovalRequired").value(true))
                .andExpect(jsonPath("$.data.agentWorkflow.steps.length()").value(5))
                .andExpect(jsonPath("$.data.agentWorkflow.steps[0].agentName").value("MarketDataAgent"))
                .andExpect(jsonPath("$.data.agentWorkflow.steps[1].agentName").value("PortfolioAgent"))
                .andExpect(jsonPath("$.data.agentWorkflow.steps[2].agentName").value("RiskAgent"))
                .andExpect(jsonPath("$.data.agentWorkflow.steps[3].agentName").value("StrategyAgent"))
                .andExpect(jsonPath("$.data.agentWorkflow.steps[4].agentName").value("ComplianceAgent"))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void rejectsUnknownModel() throws Exception {
        String token = register("ai-unknown@example.com");

        AiAnalysisRequest request = new AiAnalysisRequest(
                "unknown-model",
                "AAPL",
                "NASDAQ",
                "USD",
                "Analyze risk.",
                true
        );

        mockMvc.perform(post("/api/ai/analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
    }

    @Test
    void analysisRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/ai/models"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                "AI User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
