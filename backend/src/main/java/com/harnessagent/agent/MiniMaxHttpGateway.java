package com.harnessagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MiniMaxHttpGateway implements AiModelGateway {

    private final AiProperties properties;
    private final RestClient restClient;

    public MiniMaxHttpGateway(AiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean supports(AiProviderType provider) {
        return provider == AiProviderType.MINIMAX;
    }

    @Override
    public AiGatewayResult generate(AiModelDescriptor model, AiGatewayRequest request) {
        AiProperties.Minimax minimax = properties.minimax();
        if (!minimax.hasApiKey()) {
            throw new IllegalStateException("MiniMax API key is not configured.");
        }
        Map<String, Object> requestBody = Map.of(
                "model", model.modelName(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                ),
                "temperature", 0.2
        );
        JsonNode response = restClient.post()
                .uri(minimax.baseUrl())
                .header("Authorization", "Bearer " + minimax.apiKey())
                .header("Accept-Language", properties.responseLanguage())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("MiniMax returned an empty response.");
        }
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("MiniMax response did not include message content.");
        }
        JsonNode usage = response.path("usage");
        Integer promptTokens = optionalInt(usage.path("prompt_tokens"));
        Integer completionTokens = optionalInt(usage.path("completion_tokens"));
        Integer totalTokens = optionalInt(usage.path("total_tokens"));
        return new AiGatewayResult(content, promptTokens, completionTokens, totalTokens, TokenUsageSource.ACTUAL);
    }

    private Integer optionalInt(JsonNode node) {
        return node.isNumber() ? node.asInt() : null;
    }
}
