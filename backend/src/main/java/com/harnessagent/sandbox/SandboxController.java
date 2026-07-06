package com.harnessagent.sandbox;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    private final SandboxTaskService sandboxTaskService;

    public SandboxController(SandboxTaskService sandboxTaskService) {
        this.sandboxTaskService = sandboxTaskService;
    }

    @PostMapping("/tasks")
    @Operation(summary = "Submit a governed sandbox task")
    public ApiResponse<SandboxTaskResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SandboxTaskRequest request
    ) {
        return ApiResponse.ok(sandboxTaskService.submit(user.id(), user.email(), request), "Sandbox task accepted");
    }

    @GetMapping("/tasks")
    @Operation(summary = "List current user's sandbox tasks")
    public ApiResponse<List<SandboxTaskResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(sandboxTaskService.list(user.id()));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get current user's sandbox task")
    public ApiResponse<SandboxTaskResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long taskId
    ) {
        return ApiResponse.ok(sandboxTaskService.get(user.id(), taskId));
    }
}
