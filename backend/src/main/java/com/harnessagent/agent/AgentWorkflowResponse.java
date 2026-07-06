package com.harnessagent.agent;

import java.util.List;

public record AgentWorkflowResponse(
        String workflowId,
        AgentWorkflowStatus status,
        boolean humanApprovalRequired,
        List<String> approvalReasons,
        List<AgentStepResponse> steps
) {
}
