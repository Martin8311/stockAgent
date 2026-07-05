package com.harnessagent.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "investment_asset")
public class InvestmentAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetType assetType;

    @Column(nullable = false, length = 40)
    private String exchange;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "latest_price", precision = 19, scale = 6)
    private BigDecimal latestPrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvestmentAsset() {
    }

    private InvestmentAsset(
            String symbol,
            String name,
            AssetType assetType,
            String exchange,
            String currency,
            BigDecimal latestPrice
    ) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.currency = currency;
        this.latestPrice = latestPrice;
    }

    public static InvestmentAsset create(
            String symbol,
            String name,
            AssetType assetType,
            String exchange,
            String currency,
            BigDecimal latestPrice
    ) {
        return new InvestmentAsset(symbol, name, assetType, exchange, currency, latestPrice);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void updateMetadata(String name, AssetType assetType, BigDecimal latestPrice) {
        this.name = name;
        this.assetType = assetType;
        if (latestPrice != null) {
            this.latestPrice = latestPrice;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
