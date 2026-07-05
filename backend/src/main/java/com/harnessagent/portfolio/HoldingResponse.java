package com.harnessagent.portfolio;

import java.math.BigDecimal;

public record HoldingResponse(
        AssetResponse asset,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal costBasis,
        BigDecimal latestPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlRatio,
        BigDecimal realizedPnl
) {
}
