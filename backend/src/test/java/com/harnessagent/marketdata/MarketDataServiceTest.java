package com.harnessagent.marketdata;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketDataServiceTest {

    @Test
    void explicitQuoteRequestRecordsAuditEvent() {
        AuditEventService auditEventService = mock(AuditEventService.class);
        MarketDataService service = new MarketDataService(List.of(new FixedProvider()), auditEventService);

        service.getQuote("user@example.com", new MarketDataRequest("aapl", "nasdaq", "usd", null));

        verify(auditEventService).record(
                eq("user@example.com"),
                eq("MARKET_DATA_QUOTE_REQUESTED"),
                eq("Requested quote for AAPL from fixed-test-provider."),
                eq(RiskLevel.LOW)
        );
    }

    @Test
    void portfolioValuationQuoteDoesNotWriteAuditInsideReadOnlySummaryFlow() {
        AuditEventService auditEventService = mock(AuditEventService.class);
        MarketDataService service = new MarketDataService(List.of(new FixedProvider()), auditEventService);

        service.getQuoteForPortfolioValuation("user@example.com", "aapl", "nasdaq", "usd");

        verifyNoInteractions(auditEventService);
    }

    private static class FixedProvider implements MarketDataProvider {

        @Override
        public MarketDataProviderDescriptor descriptor() {
            return new MarketDataProviderDescriptor(
                    "fixed-test-provider",
                    MarketDataSourceType.MOCK,
                    true,
                    false,
                    "Fixed provider for unit tests."
            );
        }

        @Override
        public Optional<MarketQuote> fetchQuote(MarketDataRequest request) {
            return Optional.of(new MarketQuote(
                    request.symbol().toUpperCase(),
                    request.exchange().toUpperCase(),
                    request.currency().toUpperCase(),
                    new BigDecimal("100.000000"),
                    new BigDecimal("99.000000"),
                    new BigDecimal("1.000000"),
                    new BigDecimal("0.010101"),
                    Instant.parse("2026-07-05T00:00:00Z"),
                    descriptor().name(),
                    descriptor().sourceType(),
                    new BigDecimal("0.500000"),
                    List.of("Unit test assumption."),
                    List.of("Unit test risk warning."),
                    "Unit test disclaimer."
            ));
        }
    }
}
