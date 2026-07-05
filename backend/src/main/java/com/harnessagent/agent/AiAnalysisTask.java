package com.harnessagent.agent;

import com.harnessagent.audit.RiskLevel;
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
import java.time.Instant;

@Entity
@Table(name = "ai_analysis_task")
public class AiAnalysisTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, length = 40)
    private String exchange;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "model_id", nullable = false, length = 80)
    private String modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiProviderType provider;

    @Column(nullable = false, length = 600)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiAnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(length = 2000)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AiAnalysisTask() {
    }

    private AiAnalysisTask(
            AppUser user,
            String symbol,
            String exchange,
            String currency,
            String modelId,
            AiProviderType provider,
            String question,
            RiskLevel riskLevel
    ) {
        this.user = user;
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.modelId = modelId;
        this.provider = provider;
        this.question = question;
        this.status = AiAnalysisStatus.REQUESTED;
        this.riskLevel = riskLevel;
    }

    public static AiAnalysisTask create(
            AppUser user,
            String symbol,
            String exchange,
            String currency,
            String modelId,
            AiProviderType provider,
            String question,
            RiskLevel riskLevel
    ) {
        return new AiAnalysisTask(user, symbol, exchange, currency, modelId, provider, question, riskLevel);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void complete(String summary) {
        this.status = AiAnalysisStatus.COMPLETED;
        this.summary = truncate(summary, 2000);
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = AiAnalysisStatus.FAILED;
        this.summary = truncate(reason, 2000);
        this.completedAt = Instant.now();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public String getModelId() {
        return modelId;
    }

    public AiProviderType getProvider() {
        return provider;
    }

    public String getQuestion() {
        return question;
    }

    public AiAnalysisStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
