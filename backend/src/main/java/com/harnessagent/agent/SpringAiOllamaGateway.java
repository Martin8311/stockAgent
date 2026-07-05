package com.harnessagent.agent;

import java.lang.reflect.Method;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SpringAiOllamaGateway implements AiModelGateway {

    private final ObjectProvider<ChatModel> chatModelProvider;

    public SpringAiOllamaGateway(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public boolean supports(AiProviderType provider) {
        return provider == AiProviderType.OLLAMA;
    }

    @Override
    public AiGatewayResult generate(AiModelDescriptor model, AiGatewayRequest request) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException("Spring AI Ollama ChatModel is not available.");
        }
        ChatResponse response = chatModel.call(new Prompt(List.of(
                new SystemMessage(request.systemPrompt()),
                new UserMessage(request.userPrompt())
        )));
        String content = extractContent(response);
        TokenCounts tokenCounts = extractTokenCounts(response);
        return new AiGatewayResult(
                content,
                tokenCounts.promptTokens(),
                tokenCounts.completionTokens(),
                tokenCounts.totalTokens(),
                tokenCounts.hasActualUsage() ? TokenUsageSource.ACTUAL : TokenUsageSource.ESTIMATED
        );
    }

    private String extractContent(ChatResponse response) {
        Object result = invoke(response, "getResult");
        Object output = invoke(result, "getOutput");
        Object text = invoke(output, "getText");
        if (text == null) {
            text = invoke(output, "getContent");
        }
        if (text == null) {
            throw new IllegalStateException("Spring AI response did not include text content.");
        }
        return text.toString();
    }

    private TokenCounts extractTokenCounts(ChatResponse response) {
        Object metadata = invoke(response, "getMetadata");
        Object usage = invoke(metadata, "getUsage");
        Integer promptTokens = invokeInteger(usage, "getPromptTokens", "getInputTokens");
        Integer completionTokens = invokeInteger(usage, "getCompletionTokens", "getGenerationTokens", "getOutputTokens");
        Integer totalTokens = invokeInteger(usage, "getTotalTokens");
        return new TokenCounts(promptTokens, completionTokens, totalTokens);
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Integer invokeInteger(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invoke(target, methodName);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }

    private record TokenCounts(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        boolean hasActualUsage() {
            return promptTokens != null || completionTokens != null || totalTokens != null;
        }
    }
}
