package com.harnessagent.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.auth.RegisterRequest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "MINIMAX_LIVE_TEST_ENABLED", matches = "true")
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
class MiniMaxLiveSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void minimaxProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.test-mode", () -> "true");
        registry.add("app.ai.mock-responses-enabled", () -> "false");
        registry.add("app.ai.paid-access-enabled", () -> "true");
        registry.add("app.ai.default-model-id", () -> "minimax-chat");
        registry.add("app.ai.response-language", () -> "zh-CN");
        registry.add("app.ai.minimax.enabled", () -> "true");
        registry.add("app.ai.minimax.api-key", () -> System.getenv().getOrDefault("MINIMAX_API_KEY", ""));
    }

    @Test
    void runsRealMiniMaxAnalysisInChineseWhenExplicitlyEnabled() throws Exception {
        String token = register("minimax-live@example.com");
        AiAnalysisRequest request = new AiAnalysisRequest(
                "minimax-chat",
                "AAPL",
                "NASDAQ",
                "USD",
                "请用中文解释这个标的的主要风险、假设和适合继续研究的方向。",
                true
        );

        MvcResult result = mockMvc.perform(post("/api/ai/analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model.id").value("minimax-chat"))
                .andExpect(jsonPath("$.data.model.provider").value("MINIMAX"))
                .andExpect(jsonPath("$.data.tokenUsage.usageSource").value("ACTUAL"))
                .andExpect(jsonPath("$.data.tokenUsage.testMode").value(true))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String summary = JsonPath.read(body, "$.data.investmentSummary");
        List<String> observations = JsonPath.read(body, "$.data.keyObservations");
        List<String> riskWarnings = JsonPath.read(body, "$.data.riskWarnings");

        assertThat(summary + observations + riskWarnings)
                .as("MiniMax live response should contain Chinese natural-language content")
                .matches(".*[\\u4e00-\\u9fff].*");
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                "MiniMax Live User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
