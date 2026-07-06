package com.harnessagent.sandbox;

import com.harnessagent.audit.RiskLevel;
import com.harnessagent.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sandbox_task")
public class SandboxTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 40)
    private SandboxTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SandboxTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 4000)
    private String script;

    @Column(name = "output_json", length = 8000)
    private String outputJson;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "approval_reason", length = 1000)
    private String approvalReason;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SandboxTask() {
    }

    private SandboxTask(
            AppUser user,
            SandboxTaskType taskType,
            RiskLevel riskLevel,
            String script,
            int timeoutMs
    ) {
        this.user = user;
        this.taskType = taskType;
        this.riskLevel = riskLevel;
        this.script = script;
        this.timeoutMs = timeoutMs;
        this.status = SandboxTaskStatus.FAILED;
    }

    public static SandboxTask create(
            AppUser user,
            SandboxTaskType taskType,
            RiskLevel riskLevel,
            String script,
            int timeoutMs
    ) {
        return new SandboxTask(user, taskType, riskLevel, script, timeoutMs);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void complete(String outputJson, long executionTimeMs) {
        this.status = SandboxTaskStatus.COMPLETED;
        this.outputJson = truncate(outputJson, 8000);
        this.errorMessage = null;
        this.approvalReason = null;
        this.executionTimeMs = executionTimeMs;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage, long executionTimeMs) {
        this.status = SandboxTaskStatus.FAILED;
        this.errorMessage = truncate(errorMessage, 2000);
        this.executionTimeMs = executionTimeMs;
        this.completedAt = Instant.now();
    }

    public void reject(String errorMessage) {
        this.status = SandboxTaskStatus.REJECTED;
        this.errorMessage = truncate(errorMessage, 2000);
        this.completedAt = Instant.now();
    }

    public void requireApproval(String approvalReason) {
        this.status = SandboxTaskStatus.PENDING_APPROVAL;
        this.approvalReason = truncate(approvalReason, 1000);
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

    public AppUser getUser() {
        return user;
    }

    public SandboxTaskType getTaskType() {
        return taskType;
    }

    public SandboxTaskStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getScript() {
        return script;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getApprovalReason() {
        return approvalReason;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
