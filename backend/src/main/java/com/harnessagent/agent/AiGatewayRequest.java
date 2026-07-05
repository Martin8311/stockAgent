package com.harnessagent.agent;

public record AiGatewayRequest(
        String systemPrompt,
        String userPrompt
) {
    public String fullPrompt() {
        return systemPrompt + "\n\n" + userPrompt;
    }
}
