package com.harnessagent.agent;

import com.harnessagent.user.AppUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBillingService {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private final AiProperties properties;
    private final TokenEstimator tokenEstimator;
    private final AiTokenUsageRecordRepository tokenUsageRecordRepository;

    public TokenBillingService(
            AiProperties properties,
            TokenEstimator tokenEstimator,
            AiTokenUsageRecordRepository tokenUsageRecordRepository
    ) {
        this.properties = properties;
        this.tokenEstimator = tokenEstimator;
        this.tokenUsageRecordRepository = tokenUsageRecordRepository;
    }

    @Transactional
    public TokenUsageResponse record(
            AiAnalysisTask task,
            AppUser user,
            AiModelDescriptor model,
            AiGatewayRequest prompt,
            AiGatewayResult result
    ) {
        int promptTokens = positiveOrEstimate(result.promptTokens(), tokenEstimator.estimate(prompt.fullPrompt()));
        int completionTokens = positiveOrEstimate(result.completionTokens(), tokenEstimator.estimate(result.content()));
        int totalTokens = positiveOrEstimate(result.totalTokens(), promptTokens + completionTokens);
        TokenUsageSource source = result.usageSource() == null ? TokenUsageSource.ESTIMATED : result.usageSource();
        BigDecimal estimatedCost = estimateCost(model, promptTokens, completionTokens);
        boolean billable = estimatedCost.compareTo(BigDecimal.ZERO) > 0
                && !model.freeTier()
                && !properties.testModeEnabled();

        tokenUsageRecordRepository.save(AiTokenUsageRecord.create(
                task,
                user,
                model.provider(),
                model.id(),
                promptTokens,
                completionTokens,
                totalTokens,
                source,
                estimatedCost,
                model.currency(),
                billable,
                properties.testModeEnabled()
        ));
        return new TokenUsageResponse(
                promptTokens,
                completionTokens,
                totalTokens,
                source,
                estimatedCost,
                model.currency(),
                billable,
                properties.testModeEnabled()
        );
    }

    private int positiveOrEstimate(Integer actual, int estimate) {
        if (actual != null && actual > 0) {
            return actual;
        }
        return Math.max(0, estimate);
    }

    private BigDecimal estimateCost(AiModelDescriptor model, int promptTokens, int completionTokens) {
        if (model.freeTier() || properties.testModeEnabled()) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        BigDecimal promptCost = BigDecimal.valueOf(promptTokens)
                .multiply(model.promptPricePerMillionTokens())
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal completionCost = BigDecimal.valueOf(completionTokens)
                .multiply(model.completionPricePerMillionTokens())
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        return promptCost.add(completionCost).setScale(8, RoundingMode.HALF_UP);
    }
}
