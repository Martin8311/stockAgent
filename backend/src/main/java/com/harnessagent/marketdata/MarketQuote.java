package com.harnessagent.marketdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketQuote(
        String symbol,
        String exchange,
        String currency,
        BigDecimal latestPrice,
        BigDecimal previousClose,
        BigDecimal changeAmount,
        BigDecimal changePercent,
        Instant asOf,
        String provider,
        MarketDataSourceType sourceType,
        BigDecimal confidence,
        List<String> assumptions,
        List<String> riskWarnings,
        String disclaimer
) {
}
