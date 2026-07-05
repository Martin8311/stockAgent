package com.harnessagent.agent;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentAnalysisContent(
        String investmentSummary,
        List<String> keyObservations,
        List<String> assumptions,
        List<String> riskWarnings,
        List<String> educationalNotes,
        BigDecimal confidence
) {
}
