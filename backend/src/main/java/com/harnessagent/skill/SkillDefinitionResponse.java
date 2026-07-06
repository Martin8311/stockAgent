package com.harnessagent.skill;

import java.time.Instant;
import java.util.List;

public record SkillDefinitionResponse(
        Long id,
        String skillKey,
        String name,
        String description,
        SkillCategory category,
        boolean enabled,
        Long activeVersionId,
        SkillVersionResponse activeVersion,
        List<SkillVersionResponse> versions,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
