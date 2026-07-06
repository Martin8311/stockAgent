package com.harnessagent.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{2,79}")
        String skillKey,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @NotNull SkillCategory category,
        @NotBlank @Size(max = 8000) String content,
        @NotBlank @Size(max = 4000) String testScript
) {
}
