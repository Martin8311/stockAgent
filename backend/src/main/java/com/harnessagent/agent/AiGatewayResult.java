package com.harnessagent.agent;

public record AiGatewayResult(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        TokenUsageSource usageSource
) {
}
