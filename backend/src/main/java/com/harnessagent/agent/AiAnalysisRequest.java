package com.harnessagent.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAnalysisRequest(
        @Size(max = 80) String modelId,
        @NotBlank @Size(max = 32) String symbol,
        @Size(max = 40) String exchange,
        @Size(max = 8) String currency,
        @NotBlank @Size(max = 600) String question,
        Boolean includePortfolioContext
) {
    public boolean shouldIncludePortfolioContext() {
        return includePortfolioContext == null || includePortfolioContext;
    }
}
