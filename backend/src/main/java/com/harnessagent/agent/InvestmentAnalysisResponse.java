package com.harnessagent.agent;

import com.harnessagent.marketdata.MarketQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvestmentAnalysisResponse(
        Long analysisId,
        String symbol,
        String exchange,
        String currency,
        AiModelDescriptor model,
        MarketQuote quote,
        String investmentSummary,
        List<String> keyObservations,
        List<String> assumptions,
        List<String> riskWarnings,
        List<String> educationalNotes,
        BigDecimal confidence,
        TokenUsageResponse tokenUsage,
        String disclaimer,
        Instant createdAt
) {
}
