package com.harnessagent.agent;

import java.util.List;

public record AgentStepResponse(
        String agentName,
        AgentRole role,
        AgentStepStatus status,
        String summary,
        List<String> observations,
        List<String> riskWarnings,
        boolean requiresHumanApproval,
        String approvalReason
) {
}
