package com.harnessagent.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.auth.RegisterRequest;
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
                "DELETE FROM portfolio_transaction",
                "DELETE FROM investment_asset",
                "DELETE FROM user_profile",
                "DELETE FROM user_role",
                "DELETE FROM app_user",
                "DELETE FROM audit_event"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recordsTransactionsAndCalculatesPortfolioSummary() throws Exception {
        String token = register("portfolio@example.com");

        record(token, new PortfolioTransactionRequest(
                "aapl",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                "USD",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("1"),
                Instant.parse("2026-01-01T10:00:00Z"),
                "Initial buy"
        ));
        record(token, new PortfolioTransactionRequest(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                "USD",
                TransactionType.SELL,
                new BigDecimal("2"),
                new BigDecimal("120"),
                new BigDecimal("1"),
                Instant.parse("2026-01-02T10:00:00Z"),
                "Trim position"
        ));

        mockMvc.perform(get("/api/portfolio/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].transactionType").value("SELL"));

        mockMvc.perform(get("/api/portfolio/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdingCount").value(1))
                .andExpect(jsonPath("$.data.holdings[0].asset.symbol").value("AAPL"))
                .andExpect(jsonPath("$.data.holdings[0].quantity").value(8.0))
                .andExpect(jsonPath("$.data.holdings[0].latestPrice").value(189.32))
                .andExpect(jsonPath("$.data.totalMarketValue").value(1514.56))
                .andExpect(jsonPath("$.data.totalRealizedPnl").value(38.8))
                .andExpect(jsonPath("$.data.riskWarnings").isArray())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void rejectsSellQuantityGreaterThanAvailableHolding() throws Exception {
        String token = register("oversell@example.com");

        record(token, new PortfolioTransactionRequest(
                "MSFT",
                "Microsoft",
                AssetType.STOCK,
                "NASDAQ",
                "USD",
                TransactionType.BUY,
                new BigDecimal("1"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                Instant.parse("2026-01-01T10:00:00Z"),
                null
        ));

        mockMvc.perform(post("/api/portfolio/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PortfolioTransactionRequest(
                                "MSFT",
                                "Microsoft",
                                AssetType.STOCK,
                                "NASDAQ",
                                "USD",
                                TransactionType.SELL,
                                new BigDecimal("2"),
                                new BigDecimal("100"),
                                BigDecimal.ZERO,
                                Instant.parse("2026-01-02T10:00:00Z"),
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
    }

    @Test
    void deletesOwnTransactionAndRecomputesSummary() throws Exception {
        String token = register("delete-tx@example.com");
        Long transactionId = record(token, new PortfolioTransactionRequest(
                "VOO",
                "Vanguard S&P 500 ETF",
                AssetType.ETF,
                "NYSE",
                "USD",
                TransactionType.BUY,
                new BigDecimal("3"),
                new BigDecimal("400"),
                BigDecimal.ZERO,
                Instant.parse("2026-01-01T10:00:00Z"),
                null
        ));

        mockMvc.perform(delete("/api/portfolio/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdingCount").value(0));
    }

    private Long record(String token, PortfolioTransactionRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolio/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "StrongPass123",
                                "Portfolio User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
