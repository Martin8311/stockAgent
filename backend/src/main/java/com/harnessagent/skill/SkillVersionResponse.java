package com.harnessagent.skill;

import java.time.Instant;

public record SkillVersionResponse(
        Long id,
        Long skillId,
        int versionNumber,
        SkillVersionStatus status,
        String content,
        String testScript,
        SkillTestResult testResult,
        String approvalReason,
        String createdBy,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
