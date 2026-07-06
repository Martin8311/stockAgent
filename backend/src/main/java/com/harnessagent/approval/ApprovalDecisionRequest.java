package com.harnessagent.approval;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @Size(max = 1000) String comment
) {
}
