package com.harnessagent.portfolio;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/transactions")
    @Operation(summary = "List current user's portfolio transactions")
    public ApiResponse<List<PortfolioTransactionResponse>> transactions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(portfolioService.listTransactions(user.id()));
    }

    @PostMapping("/transactions")
    @Operation(summary = "Record a buy or sell transaction for current user")
    public ApiResponse<PortfolioTransactionResponse> recordTransaction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PortfolioTransactionRequest request
    ) {
        return ApiResponse.ok(
                portfolioService.recordTransaction(user.id(), user.email(), request),
                "Transaction recorded"
        );
    }

    @DeleteMapping("/transactions/{transactionId}")
    @Operation(summary = "Delete current user's portfolio transaction")
    public ApiResponse<Void> deleteTransaction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long transactionId
    ) {
        portfolioService.deleteTransaction(user.id(), user.email(), transactionId);
        return ApiResponse.ok(null, "Transaction deleted");
    }

    @GetMapping("/summary")
    @Operation(summary = "Get current user's portfolio holdings and risk summary")
    public ApiResponse<PortfolioSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(portfolioService.getSummary(user.id(), user.email()));
    }
}
