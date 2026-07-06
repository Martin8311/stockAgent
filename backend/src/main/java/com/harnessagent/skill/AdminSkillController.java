package com.harnessagent.skill;

import com.harnessagent.approval.ApprovalResponse;
import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/skills")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSkillController {

    private final SkillService skillService;

    public AdminSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "List all skills for administrators")
    public ApiResponse<List<SkillDefinitionResponse>> listSkills() {
        return ApiResponse.ok(skillService.listAllSkills());
    }

    @PostMapping
    @Operation(summary = "Create a skill with an initial draft version")
    public ApiResponse<SkillDefinitionResponse> createSkill(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateSkillRequest request
    ) {
        return ApiResponse.ok(skillService.createSkill(user.email(), request), "Skill created");
    }

    @PostMapping("/{skillId}/versions")
    @Operation(summary = "Create a new draft skill version")
    public ApiResponse<SkillDefinitionResponse> createVersion(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long skillId,
            @Valid @RequestBody CreateSkillVersionRequest request
    ) {
        return ApiResponse.ok(skillService.createVersion(user.email(), skillId, request), "Skill version created");
    }

    @PostMapping("/versions/{versionId}/test")
    @Operation(summary = "Test a skill version through the governed sandbox")
    public ApiResponse<SkillVersionResponse> testVersion(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long versionId
    ) {
        return ApiResponse.ok(skillService.testVersion(user.id(), user.email(), versionId), "Skill version tested");
    }

    @PostMapping("/versions/{versionId}/submit-approval")
    @Operation(summary = "Submit a tested skill version for approval")
    public ApiResponse<ApprovalResponse> submitApproval(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long versionId,
            @Valid @RequestBody(required = false) SubmitSkillApprovalRequest request
    ) {
        return ApiResponse.ok(skillService.submitApproval(user.email(), versionId, request), "Approval requested");
    }

    @PostMapping("/versions/{versionId}/activate")
    @Operation(summary = "Activate an approved skill version")
    public ApiResponse<SkillDefinitionResponse> activateVersion(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long versionId
    ) {
        return ApiResponse.ok(skillService.activateVersion(user.email(), versionId), "Skill version activated");
    }
}
