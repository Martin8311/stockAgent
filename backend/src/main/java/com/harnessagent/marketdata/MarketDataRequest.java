package com.harnessagent.marketdata;

public record MarketDataRequest(
        String symbol,
        String exchange,
        String currency,
        String provider
) {
}
