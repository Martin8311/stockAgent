package com.harnessagent.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillVersionRequest(
        @NotBlank @Size(max = 8000) String content,
        @NotBlank @Size(max = 4000) String testScript
) {
}
