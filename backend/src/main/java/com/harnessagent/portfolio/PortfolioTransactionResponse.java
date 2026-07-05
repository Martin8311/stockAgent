package com.harnessagent.portfolio;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioTransactionResponse(
        Long id,
        AssetResponse asset,
        TransactionType transactionType,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal grossAmount,
        Instant tradedAt,
        String note
) {
    public static PortfolioTransactionResponse from(PortfolioTransaction transaction) {
        return new PortfolioTransactionResponse(
                transaction.getId(),
                AssetResponse.from(transaction.getAsset()),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFee(),
                transaction.getQuantity().multiply(transaction.getPrice()),
                transaction.getTradedAt(),
                transaction.getNote()
        );
    }
}
