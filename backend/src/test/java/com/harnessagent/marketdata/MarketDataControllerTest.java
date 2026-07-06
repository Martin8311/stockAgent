package com.harnessagent.marketdata;

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
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsProvidersAndReturnsMockQuoteWithRiskContext() throws Exception {
        String token = register("market-data@example.com");

        mockMvc.perform(get("/api/market-data/providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sourceType").value("EXTERNAL_PLACEHOLDER"))
                .andExpect(jsonPath("$.data[0].enabled").value(false))
                .andExpect(jsonPath("$.data[1].sourceType").value("MOCK"))
                .andExpect(jsonPath("$.data[1].enabled").value(true));

        mockMvc.perform(get("/api/market-data/quote")
                        .param("symbol", "AAPL")
                        .param("exchange", "NASDAQ")
                        .param("currency", "USD")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.symbol").value("AAPL"))
                .andExpect(jsonPath("$.data.sourceType").value("MOCK"))
                .andExpect(jsonPath("$.data.latestPrice").value(189.32))
                .andExpect(jsonPath("$.data.confidence").value(0.35))
                .andExpect(jsonPath("$.data.assumptions").isArray())
                .andExpect(jsonPath("$.data.riskWarnings").isArray())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void quoteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/market-data/quote").param("symbol", "AAPL"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                "Market Data User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
