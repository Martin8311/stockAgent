package com.harnessagent.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(
        BigDecimal totalCostBasis,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalUnrealizedPnlRatio,
        BigDecimal totalRealizedPnl,
        int holdingCount,
        List<HoldingResponse> holdings,
        List<String> riskWarnings,
        String disclaimer
) {
}
