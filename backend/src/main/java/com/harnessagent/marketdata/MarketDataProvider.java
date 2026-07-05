package com.harnessagent.marketdata;

import java.util.Optional;

public interface MarketDataProvider {

    MarketDataProviderDescriptor descriptor();

    Optional<MarketQuote> fetchQuote(MarketDataRequest request);
}
