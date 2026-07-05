package com.harnessagent.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

class HoldingAccumulator {

    private static final int SCALE = 6;

    private final InvestmentAsset asset;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal costBasis = BigDecimal.ZERO;
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    HoldingAccumulator(InvestmentAsset asset) {
        this.asset = asset;
    }

    void apply(PortfolioTransaction transaction) {
        if (transaction.getTransactionType() == TransactionType.BUY) {
            BigDecimal buyCost = transaction.getQuantity().multiply(transaction.getPrice()).add(transaction.getFee());
            quantity = quantity.add(transaction.getQuantity());
            costBasis = costBasis.add(buyCost);
            return;
        }

        BigDecimal averageCost = averageCost();
        BigDecimal reducedCost = averageCost.multiply(transaction.getQuantity());
        BigDecimal proceeds = transaction.getQuantity().multiply(transaction.getPrice()).subtract(transaction.getFee());
        quantity = quantity.subtract(transaction.getQuantity());
        costBasis = costBasis.subtract(reducedCost);
        realizedPnl = realizedPnl.add(proceeds.subtract(reducedCost));

        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            costBasis = BigDecimal.ZERO;
        }
    }

    boolean wouldGoNegative(PortfolioTransaction transaction) {
        return transaction.getTransactionType() == TransactionType.SELL
                && quantity.compareTo(transaction.getQuantity()) < 0;
    }

    boolean hasOpenPosition() {
        return quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    HoldingResponse toResponse() {
        return toResponse(asset.getLatestPrice() == null ? averageCost() : asset.getLatestPrice());
    }

    HoldingResponse toResponse(BigDecimal latestPrice) {
        BigDecimal marketValue = quantity.multiply(latestPrice);
        BigDecimal unrealizedPnl = marketValue.subtract(costBasis);
        BigDecimal ratio = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : unrealizedPnl.divide(costBasis, SCALE, RoundingMode.HALF_UP);
        return new HoldingResponse(
                AssetResponse.from(asset),
                quantity,
                averageCost(),
                costBasis,
                latestPrice,
                marketValue,
                unrealizedPnl,
                ratio,
                realizedPnl
        );
    }

    InvestmentAsset asset() {
        return asset;
    }

    BigDecimal costBasis() {
        return costBasis;
    }

    BigDecimal marketValue() {
        return toResponse().marketValue();
    }

    BigDecimal unrealizedPnl() {
        return marketValue().subtract(costBasis);
    }

    BigDecimal realizedPnl() {
        return realizedPnl;
    }

    private BigDecimal averageCost() {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return costBasis.divide(quantity, SCALE, RoundingMode.HALF_UP);
    }
}
