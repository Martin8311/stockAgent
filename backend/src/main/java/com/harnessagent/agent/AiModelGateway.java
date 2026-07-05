package com.harnessagent.agent;

public interface AiModelGateway {

    boolean supports(AiProviderType provider);

    AiGatewayResult generate(AiModelDescriptor model, AiGatewayRequest request);
}
