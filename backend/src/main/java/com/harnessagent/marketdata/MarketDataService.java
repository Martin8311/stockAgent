package com.harnessagent.marketdata;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.web.ApiRequestException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private final List<MarketDataProvider> providers;
    private final AuditEventService auditEventService;

    public MarketDataService(List<MarketDataProvider> providers, AuditEventService auditEventService) {
        this.providers = providers.stream()
                .sorted(Comparator.comparing(provider -> provider.descriptor().name()))
                .toList();
        this.auditEventService = auditEventService;
    }

    public List<MarketDataProviderDescriptor> providers() {
        return providers.stream()
                .map(MarketDataProvider::descriptor)
                .toList();
    }

    public MarketQuote getQuote(String actor, MarketDataRequest request) {
        MarketDataRequest normalized = normalize(request);
        MarketQuote quote = providers.stream()
                .filter(provider -> provider.descriptor().enabled())
                .filter(provider -> normalized.provider() == null
                        || provider.descriptor().name().equalsIgnoreCase(normalized.provider()))
                .flatMap(provider -> provider.fetchQuote(normalized).stream())
                .findFirst()
                .orElseThrow(() -> new ApiRequestException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No enabled market data provider returned a quote for " + normalized.symbol()
                ));
        auditEventService.record(
                actor,
                "MARKET_DATA_QUOTE_REQUESTED",
                "Requested quote for " + quote.symbol() + " from " + quote.provider() + ".",
                quote.sourceType() == MarketDataSourceType.MOCK ? RiskLevel.LOW : RiskLevel.MEDIUM
        );
        return quote;
    }

    public MarketQuote getQuoteForPortfolioValuation(String actor, String symbol, String exchange, String currency) {
        return getQuote(actor, new MarketDataRequest(symbol, exchange, currency, null));
    }

    private MarketDataRequest normalize(MarketDataRequest request) {
        String symbol = normalizeRequired(request.symbol(), "symbol").toUpperCase(Locale.ROOT);
        String exchange = normalizeDefault(request.exchange(), "GLOBAL").toUpperCase(Locale.ROOT);
        String currency = normalizeDefault(request.currency(), "USD").toUpperCase(Locale.ROOT);
        String provider = normalizeOptional(request.provider());
        return new MarketDataRequest(symbol, exchange, currency, provider);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return normalized;
    }

    private String normalizeDefault(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
