package com.harnessagent.portfolio;

import com.harnessagent.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_transaction")
public class PortfolioTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id")
    private InvestmentAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal fee;

    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt;

    @Column(length = 240)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PortfolioTransaction() {
    }

    private PortfolioTransaction(
            AppUser user,
            InvestmentAsset asset,
            TransactionType transactionType,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fee,
            Instant tradedAt,
            String note
    ) {
        this.user = user;
        this.asset = asset;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee;
        this.tradedAt = tradedAt;
        this.note = note;
    }

    public static PortfolioTransaction create(
            AppUser user,
            InvestmentAsset asset,
            TransactionType transactionType,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fee,
            Instant tradedAt,
            String note
    ) {
        return new PortfolioTransaction(user, asset, transactionType, quantity, price, fee, tradedAt, note);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public InvestmentAsset getAsset() {
        return asset;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public Instant getTradedAt() {
        return tradedAt;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
