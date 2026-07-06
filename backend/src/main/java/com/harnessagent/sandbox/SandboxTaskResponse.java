package com.harnessagent.sandbox;

import com.harnessagent.audit.RiskLevel;
import java.time.Instant;

public record SandboxTaskResponse(
        Long id,
        SandboxTaskType taskType,
        SandboxTaskStatus status,
        RiskLevel riskLevel,
        String script,
        SandboxExecutionOutput output,
        String errorMessage,
        String approvalReason,
        int timeoutMs,
        Long executionTimeMs,
        Instant createdAt,
        Instant completedAt
) {
    public static SandboxTaskResponse from(SandboxTask task, SandboxExecutionOutput output) {
        return new SandboxTaskResponse(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getRiskLevel(),
                task.getScript(),
                output,
                task.getErrorMessage(),
                task.getApprovalReason(),
                task.getTimeoutMs(),
                task.getExecutionTimeMs(),
                task.getCreatedAt(),
                task.getCompletedAt()
        );
    }
}
