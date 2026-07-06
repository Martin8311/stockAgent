package com.harnessagent.sandbox;

import java.util.List;
import java.util.Map;

public record SandboxExecutionOutput(
        String summary,
        Map<String, Object> metrics,
        List<String> observations,
        List<String> riskWarnings,
        String disclaimer
) {
}
