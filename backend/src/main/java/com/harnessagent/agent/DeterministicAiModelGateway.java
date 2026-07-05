package com.harnessagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeterministicAiModelGateway implements AiModelGateway {

    private final ObjectMapper objectMapper;

    public DeterministicAiModelGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(AiProviderType provider) {
        return true;
    }

    @Override
    public AiGatewayResult generate(AiModelDescriptor model, AiGatewayRequest request) {
        InvestmentAnalysisContent content = new InvestmentAnalysisContent(
                "This deterministic test analysis summarizes the available quote and portfolio context without making a buy or sell recommendation.",
                List.of(
                        "The selected model is " + model.displayName() + " and the response is generated in controlled mock mode.",
                        "Market data and user suitability context are treated as assumptions for educational analysis."
                ),
                List.of(
                        "The quote may be mock or delayed depending on the configured market data provider.",
                        "User risk preference, investment horizon, and capital purpose must be considered before any decision."
                ),
                List.of(
                        "Investment involves risk, including possible loss of principal.",
                        "No analysis from this system guarantees future returns."
                ),
                List.of(
                        "Use this report as auxiliary research material only.",
                        "Review concentration, liquidity, and time-horizon fit before acting."
                ),
                new BigDecimal("0.420000")
        );
        try {
            return new AiGatewayResult(
                    objectMapper.writeValueAsString(content),
                    null,
                    null,
                    null,
                    TokenUsageSource.MOCK
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not render deterministic AI response", ex);
        }
    }
}
