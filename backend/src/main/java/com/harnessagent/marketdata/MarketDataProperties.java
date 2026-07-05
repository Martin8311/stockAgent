package com.harnessagent.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data")
public record MarketDataProperties(
        Boolean mockEnabled,
        Boolean externalEnabled,
        String mockProviderName,
        String externalProviderName
) {
    public MarketDataProperties {
        if (mockEnabled == null) {
            mockEnabled = true;
        }
        if (externalEnabled == null) {
            externalEnabled = false;
        }
        if (mockProviderName == null || mockProviderName.isBlank()) {
            mockProviderName = "local-mock-market-data";
        }
        if (externalProviderName == null || externalProviderName.isBlank()) {
            externalProviderName = "external-adapter-placeholder";
        }
    }
}
