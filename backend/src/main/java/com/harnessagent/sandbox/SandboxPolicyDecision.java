package com.harnessagent.sandbox;

import com.harnessagent.audit.RiskLevel;

public record SandboxPolicyDecision(
        boolean rejected,
        boolean approvalRequired,
        RiskLevel riskLevel,
        String reason,
        int timeoutMs,
        String normalizedScript
) {
}
