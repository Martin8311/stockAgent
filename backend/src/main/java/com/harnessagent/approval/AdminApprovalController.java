package com.harnessagent.approval;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.skill.SkillService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/approvals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    private final SkillService skillService;

    public AdminApprovalController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "List approval requests")
    public ApiResponse<List<ApprovalResponse>> listApprovals(@RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.ok(skillService.listApprovals(status));
    }

    @PostMapping("/{approvalId}/approve")
    @Operation(summary = "Approve an approval request")
    public ApiResponse<ApprovalResponse> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long approvalId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        String comment = request == null ? null : request.comment();
        return ApiResponse.ok(skillService.approve(user.email(), approvalId, comment), "Approval accepted");
    }

    @PostMapping("/{approvalId}/reject")
    @Operation(summary = "Reject an approval request")
    public ApiResponse<ApprovalResponse> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long approvalId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        String comment = request == null ? null : request.comment();
        return ApiResponse.ok(skillService.reject(user.email(), approvalId, comment), "Approval rejected");
    }
}
