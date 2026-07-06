package com.harnessagent.sandbox;

import com.harnessagent.portfolio.PortfolioSummaryResponse;

public record SandboxExecutionRequest(
        SandboxTaskType taskType,
        String script,
        int timeoutMs,
        PortfolioSummaryResponse portfolioSummary
) {
}
