package com.harnessagent.portfolio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioTransactionRequest(
        @NotBlank @Size(max = 32) String symbol,
        @NotBlank @Size(max = 120) String name,
        @NotNull AssetType assetType,
        @Size(max = 40) String exchange,
        @Size(max = 8) String currency,
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal price,
        @DecimalMin(value = "0.000000") BigDecimal fee,
        Instant tradedAt,
        @Size(max = 240) String note
) {
}
