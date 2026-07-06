package com.harnessagent.approval;

import java.time.Instant;

public record ApprovalResponse(
        Long id,
        ApprovalRequestType requestType,
        ApprovalTargetType targetType,
        Long targetId,
        ApprovalStatus status,
        String reason,
        String decisionComment,
        String requestedBy,
        String reviewedBy,
        Instant createdAt,
        Instant decidedAt
) {
    public static ApprovalResponse from(ApprovalRequest request) {
        return new ApprovalResponse(
                request.getId(),
                request.getRequestType(),
                request.getTargetType(),
                request.getTargetId(),
                request.getStatus(),
                request.getReason(),
                request.getDecisionComment(),
                request.getRequestedBy(),
                request.getReviewedBy(),
                request.getCreatedAt(),
                request.getDecidedAt()
        );
    }
}
