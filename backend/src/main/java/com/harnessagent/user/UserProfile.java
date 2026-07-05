package com.harnessagent.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_preference", nullable = false, length = 32)
    private RiskPreference riskPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_horizon", nullable = false, length = 32)
    private InvestmentHorizon investmentHorizon;

    @Enumerated(EnumType.STRING)
    @Column(name = "capital_purpose", nullable = false, length = 64)
    private CapitalPurpose capitalPurpose;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    private UserProfile(AppUser user) {
        this.user = user;
        this.riskPreference = RiskPreference.UNKNOWN;
        this.investmentHorizon = InvestmentHorizon.UNKNOWN;
        this.capitalPurpose = CapitalPurpose.UNKNOWN;
    }

    public static UserProfile createDefault(AppUser user) {
        return new UserProfile(user);
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

    public void update(RiskPreference riskPreference, InvestmentHorizon investmentHorizon, CapitalPurpose capitalPurpose) {
        this.riskPreference = riskPreference == null ? RiskPreference.UNKNOWN : riskPreference;
        this.investmentHorizon = investmentHorizon == null ? InvestmentHorizon.UNKNOWN : investmentHorizon;
        this.capitalPurpose = capitalPurpose == null ? CapitalPurpose.UNKNOWN : capitalPurpose;
    }

    public Long getUserId() {
        return userId;
    }

    public AppUser getUser() {
        return user;
    }

    public RiskPreference getRiskPreference() {
        return riskPreference;
    }

    public InvestmentHorizon getInvestmentHorizon() {
        return investmentHorizon;
    }

    public CapitalPurpose getCapitalPurpose() {
        return capitalPurpose;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

