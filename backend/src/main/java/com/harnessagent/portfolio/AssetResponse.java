package com.harnessagent.portfolio;

import java.math.BigDecimal;

public record AssetResponse(
        Long id,
        String symbol,
        String name,
        AssetType assetType,
        String exchange,
        String currency,
        BigDecimal latestPrice
) {
    public static AssetResponse from(InvestmentAsset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getAssetType(),
                asset.getExchange(),
                asset.getCurrency(),
                asset.getLatestPrice()
        );
    }
}
