package com.harnessagent.sandbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SandboxTaskRequest(
        @NotNull SandboxTaskType taskType,
        @NotBlank @Size(max = 4000) String script,
        Integer timeoutMs
) {
}
