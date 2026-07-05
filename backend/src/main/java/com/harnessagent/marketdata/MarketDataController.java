package com.harnessagent.marketdata;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/providers")
    @Operation(summary = "List available market data providers")
    public ApiResponse<List<MarketDataProviderDescriptor>> providers() {
        return ApiResponse.ok(marketDataService.providers());
    }

    @GetMapping("/quote")
    @Operation(summary = "Get a structured market quote for an asset")
    public ApiResponse<MarketQuote> quote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "GLOBAL") String exchange,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestParam(required = false) String provider
    ) {
        return ApiResponse.ok(marketDataService.getQuote(
                user.email(),
                new MarketDataRequest(symbol, exchange, currency, provider)
        ));
    }
}
