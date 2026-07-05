package com.harnessagent.agent;

import java.math.BigDecimal;

public record AiModelDescriptor(
        String id,
        AiProviderType provider,
        String displayName,
        String modelName,
        boolean enabled,
        boolean local,
        boolean freeTier,
        boolean paidTier,
        boolean testModeFree,
        boolean requiresApiKey,
        String billingMode,
        BigDecimal promptPricePerMillionTokens,
        BigDecimal completionPricePerMillionTokens,
        String currency,
        String statusNote
) {
}
