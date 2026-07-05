package com.harnessagent.agent;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        Boolean testMode,
        Boolean mockResponsesEnabled,
        Boolean paidAccessEnabled,
        String defaultModelId,
        Ollama ollama,
        Minimax minimax
) {
    public AiProperties {
        if (testMode == null) {
            testMode = false;
        }
        if (mockResponsesEnabled == null) {
            mockResponsesEnabled = false;
        }
        if (paidAccessEnabled == null) {
            paidAccessEnabled = false;
        }
        if (defaultModelId == null || defaultModelId.isBlank()) {
            defaultModelId = "ollama-qwen2.5-3b";
        }
        if (ollama == null) {
            ollama = new Ollama(null, null, null, null, null, null, null, null);
        }
        if (minimax == null) {
            minimax = new Minimax(null, null, null, null, null, null, null, null, null);
        }
    }

    public boolean testModeEnabled() {
        return Boolean.TRUE.equals(testMode);
    }

    public boolean mockResponsesEnabledFlag() {
        return Boolean.TRUE.equals(mockResponsesEnabled);
    }

    public boolean paidAccessEnabledFlag() {
        return Boolean.TRUE.equals(paidAccessEnabled);
    }

    public record Ollama(
            Boolean enabled,
            String modelId,
            String displayName,
            String model,
            String baseUrl,
            String currency,
            BigDecimal promptPricePerMillionTokens,
            BigDecimal completionPricePerMillionTokens
    ) {
        public Ollama {
            if (enabled == null) {
                enabled = true;
            }
            if (modelId == null || modelId.isBlank()) {
                modelId = "ollama-qwen2.5-3b";
            }
            if (displayName == null || displayName.isBlank()) {
                displayName = "Local Ollama Qwen2.5 3B";
            }
            if (model == null || model.isBlank()) {
                model = "qwen2.5:3b";
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "http://localhost:11434";
            }
            if (currency == null || currency.isBlank()) {
                currency = "USD";
            }
            if (promptPricePerMillionTokens == null) {
                promptPricePerMillionTokens = BigDecimal.ZERO;
            }
            if (completionPricePerMillionTokens == null) {
                completionPricePerMillionTokens = BigDecimal.ZERO;
            }
        }

        public boolean enabledFlag() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    public record Minimax(
            Boolean enabled,
            String modelId,
            String displayName,
            String model,
            String baseUrl,
            String apiKey,
            String currency,
            BigDecimal promptPricePerMillionTokens,
            BigDecimal completionPricePerMillionTokens
    ) {
        public Minimax {
            if (enabled == null) {
                enabled = true;
            }
            if (modelId == null || modelId.isBlank()) {
                modelId = "minimax-chat";
            }
            if (displayName == null || displayName.isBlank()) {
                displayName = "MiniMax paid model";
            }
            if (model == null || model.isBlank()) {
                model = "MiniMax-Text-01";
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.minimax.chat/v1/chat/completions";
            }
            if (apiKey == null) {
                apiKey = "";
            }
            if (currency == null || currency.isBlank()) {
                currency = "USD";
            }
            if (promptPricePerMillionTokens == null) {
                promptPricePerMillionTokens = BigDecimal.ZERO;
            }
            if (completionPricePerMillionTokens == null) {
                completionPricePerMillionTokens = BigDecimal.ZERO;
            }
        }

        public boolean enabledFlag() {
            return Boolean.TRUE.equals(enabled);
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
