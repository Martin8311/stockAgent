package com.harnessagent.agent;

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
@Table(name = "ai_token_usage_record")
public class AiTokenUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_task_id")
    private AiAnalysisTask analysisTask;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiProviderType provider;

    @Column(name = "model_id", nullable = false, length = 80)
    private String modelId;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_source", nullable = false, length = 32)
    private TokenUsageSource usageSource;

    @Column(name = "estimated_cost", nullable = false, precision = 19, scale = 8)
    private BigDecimal estimatedCost;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private boolean billable;

    @Column(name = "test_mode", nullable = false)
    private boolean testMode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiTokenUsageRecord() {
    }

    private AiTokenUsageRecord(
            AiAnalysisTask analysisTask,
            AppUser user,
            AiProviderType provider,
            String modelId,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            TokenUsageSource usageSource,
            BigDecimal estimatedCost,
            String currency,
            boolean billable,
            boolean testMode
    ) {
        this.analysisTask = analysisTask;
        this.user = user;
        this.provider = provider;
        this.modelId = modelId;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.usageSource = usageSource;
        this.estimatedCost = estimatedCost;
        this.currency = currency;
        this.billable = billable;
        this.testMode = testMode;
    }

    public static AiTokenUsageRecord create(
            AiAnalysisTask analysisTask,
            AppUser user,
            AiProviderType provider,
            String modelId,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            TokenUsageSource usageSource,
            BigDecimal estimatedCost,
            String currency,
            boolean billable,
            boolean testMode
    ) {
        return new AiTokenUsageRecord(
                analysisTask,
                user,
                provider,
                modelId,
                promptTokens,
                completionTokens,
                totalTokens,
                usageSource,
                estimatedCost,
                currency,
                billable,
                testMode
        );
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
