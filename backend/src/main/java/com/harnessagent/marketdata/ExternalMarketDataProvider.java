package com.harnessagent.marketdata;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ExternalMarketDataProvider implements MarketDataProvider {

    private final MarketDataProperties properties;

    public ExternalMarketDataProvider(MarketDataProperties properties) {
        this.properties = properties;
    }

    @Override
    public MarketDataProviderDescriptor descriptor() {
        return new MarketDataProviderDescriptor(
                properties.externalProviderName(),
                MarketDataSourceType.EXTERNAL_PLACEHOLDER,
                properties.externalEnabled(),
                true,
                "Reserved adapter slot for approved external market data vendors. Disabled until API keys, rate limits, audit, and approval workflow are configured."
        );
    }

    @Override
    public Optional<MarketQuote> fetchQuote(MarketDataRequest request) {
        return Optional.empty();
    }
}
