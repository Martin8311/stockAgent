package com.harnessagent.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approval_request")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 60)
    private ApprovalRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 60)
    private ApprovalTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApprovalStatus status;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    @Column(name = "requested_by", nullable = false, length = 160)
    private String requestedBy;

    @Column(name = "reviewed_by", length = 160)
    private String reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequest() {
    }

    private ApprovalRequest(
            ApprovalRequestType requestType,
            ApprovalTargetType targetType,
            Long targetId,
            String reason,
            String requestedBy
    ) {
        this.requestType = requestType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.status = ApprovalStatus.PENDING;
    }

    public static ApprovalRequest create(
            ApprovalRequestType requestType,
            ApprovalTargetType targetType,
            Long targetId,
            String reason,
            String requestedBy
    ) {
        return new ApprovalRequest(requestType, targetType, targetId, reason, requestedBy);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void approve(String reviewer, String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.decisionComment = truncate(comment, 1000);
        this.decidedAt = Instant.now();
    }

    public void reject(String reviewer, String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.decisionComment = truncate(comment, 1000);
        this.decidedAt = Instant.now();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public ApprovalRequestType getRequestType() {
        return requestType;
    }

    public ApprovalTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
