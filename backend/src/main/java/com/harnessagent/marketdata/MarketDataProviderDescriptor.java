package com.harnessagent.marketdata;

public record MarketDataProviderDescriptor(
        String name,
        MarketDataSourceType sourceType,
        boolean enabled,
        boolean requiresApproval,
        String description
) {
}
