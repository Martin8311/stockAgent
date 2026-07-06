package com.harnessagent.skill;

import jakarta.validation.constraints.Size;

public record SubmitSkillApprovalRequest(
        @Size(max = 1000) String reason
) {
}
