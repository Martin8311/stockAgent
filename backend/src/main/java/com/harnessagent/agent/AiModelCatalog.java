package com.harnessagent.agent;

import com.harnessagent.web.ApiRequestException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiModelCatalog {

    private final AiProperties properties;

    public AiModelCatalog(AiProperties properties) {
        this.properties = properties;
    }

    public List<AiModelDescriptor> models() {
        return List.of(ollamaDescriptor(), minimaxDescriptor());
    }

    public AiModelDescriptor resolve(String modelId) {
        String effectiveModelId = modelId == null || modelId.isBlank()
                ? properties.defaultModelId()
                : modelId.trim();
        return models().stream()
                .filter(model -> model.id().equalsIgnoreCase(effectiveModelId))
                .findFirst()
                .orElseThrow(() -> new ApiRequestException(HttpStatus.BAD_REQUEST, "Unknown AI model: " + effectiveModelId));
    }

    private AiModelDescriptor ollamaDescriptor() {
        AiProperties.Ollama ollama = properties.ollama();
        return new AiModelDescriptor(
                ollama.modelId(),
                AiProviderType.OLLAMA,
                ollama.displayName(),
                ollama.model(),
                ollama.enabledFlag(),
                true,
                true,
                false,
                true,
                false,
                "FREE_LOCAL_TOKEN_ACCOUNTING",
                ollama.promptPricePerMillionTokens(),
                ollama.completionPricePerMillionTokens(),
                ollama.currency(),
                ollama.enabledFlag()
                        ? "Local Ollama model. Tokens are recorded for observability, cost is zero."
                        : "Local Ollama model is disabled by configuration."
        );
    }

    private AiModelDescriptor minimaxDescriptor() {
        AiProperties.Minimax minimax = properties.minimax();
        boolean accessAllowed = properties.testModeEnabled() || properties.paidAccessEnabledFlag();
        boolean enabled = minimax.enabledFlag() && minimax.hasApiKey() && accessAllowed;
        String statusNote = minimaxStatusNote(minimax, accessAllowed);
        return new AiModelDescriptor(
                minimax.modelId(),
                AiProviderType.MINIMAX,
                minimax.displayName(),
                minimax.model(),
                enabled,
                false,
                false,
                true,
                properties.testModeEnabled(),
                true,
                properties.testModeEnabled() ? "TEST_FREE_TOKEN_ACCOUNTING" : "PAID_TOKEN_ACCOUNTING",
                minimax.promptPricePerMillionTokens(),
                minimax.completionPricePerMillionTokens(),
                minimax.currency(),
                statusNote
        );
    }

    private String minimaxStatusNote(AiProperties.Minimax minimax, boolean accessAllowed) {
        if (!minimax.enabledFlag()) {
            return "MiniMax provider is disabled by configuration.";
        }
        if (!minimax.hasApiKey()) {
            return "MiniMax API key is not configured. Set MINIMAX_API_KEY to enable this provider.";
        }
        if (!accessAllowed) {
            return "MiniMax is reserved for paid users. Enable AI_PAID_ACCESS_ENABLED or AI_TEST_MODE for testing.";
        }
        if (properties.testModeEnabled()) {
            return "MiniMax is enabled in test mode. Token usage is recorded with zero cost.";
        }
        return "MiniMax is enabled for paid token accounting.";
    }
}
