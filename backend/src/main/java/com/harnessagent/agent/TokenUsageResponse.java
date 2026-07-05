package com.harnessagent.agent;

import java.math.BigDecimal;

public record TokenUsageResponse(
        int promptTokens,
        int completionTokens,
        int totalTokens,
        TokenUsageSource usageSource,
        BigDecimal estimatedCost,
        String currency,
        boolean billable,
        boolean testMode
) {
}
