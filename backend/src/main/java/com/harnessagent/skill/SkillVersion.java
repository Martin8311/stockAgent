package com.harnessagent.skill;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "skill_version")
public class SkillVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private SkillDefinition skill;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SkillVersionStatus status;

    @Column(nullable = false, length = 8000)
    private String content;

    @Column(name = "test_script", nullable = false, length = 4000)
    private String testScript;

    @Column(name = "test_result_json", length = 8000)
    private String testResultJson;

    @Column(name = "approval_reason", length = 1000)
    private String approvalReason;

    @Column(name = "created_by", nullable = false, length = 160)
    private String createdBy;

    @Column(name = "reviewed_by", length = 160)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillVersion() {
    }

    private SkillVersion(
            SkillDefinition skill,
            int versionNumber,
            String content,
            String testScript,
            String createdBy
    ) {
        this.skill = skill;
        this.versionNumber = versionNumber;
        this.content = content;
        this.testScript = testScript;
        this.createdBy = createdBy;
        this.status = SkillVersionStatus.DRAFT;
    }

    public static SkillVersion create(
            SkillDefinition skill,
            int versionNumber,
            String content,
            String testScript,
            String createdBy
    ) {
        return new SkillVersion(skill, versionNumber, content, testScript, createdBy);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markTested(String testResultJson) {
        this.status = SkillVersionStatus.TESTED;
        this.testResultJson = truncate(testResultJson, 8000);
        this.approvalReason = null;
    }

    public void recordTestResult(String testResultJson) {
        this.testResultJson = truncate(testResultJson, 8000);
    }

    public void submitApproval(String reason) {
        this.status = SkillVersionStatus.PENDING_APPROVAL;
        this.approvalReason = truncate(reason, 1000);
    }

    public void approve(String reviewer) {
        this.status = SkillVersionStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
    }

    public void reject(String reviewer, String reason) {
        this.status = SkillVersionStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.approvalReason = truncate(reason, 1000);
    }

    public void activate() {
        this.status = SkillVersionStatus.ACTIVE;
    }

    public void deactivateToApproved() {
        if (this.status == SkillVersionStatus.ACTIVE) {
            this.status = SkillVersionStatus.APPROVED;
        }
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

    public SkillDefinition getSkill() {
        return skill;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public SkillVersionStatus getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public String getTestScript() {
        return testScript;
    }

    public String getTestResultJson() {
        return testResultJson;
    }

    public String getApprovalReason() {
        return approvalReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
