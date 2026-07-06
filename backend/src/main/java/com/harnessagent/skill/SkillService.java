package com.harnessagent.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.approval.ApprovalRequest;
import com.harnessagent.approval.ApprovalRequestRepository;
import com.harnessagent.approval.ApprovalRequestType;
import com.harnessagent.approval.ApprovalResponse;
import com.harnessagent.approval.ApprovalStatus;
import com.harnessagent.approval.ApprovalTargetType;
import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.sandbox.SandboxTaskRequest;
import com.harnessagent.sandbox.SandboxTaskResponse;
import com.harnessagent.sandbox.SandboxTaskService;
import com.harnessagent.sandbox.SandboxTaskStatus;
import com.harnessagent.sandbox.SandboxTaskType;
import com.harnessagent.web.ApiRequestException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillDefinitionRepository skillDefinitionRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final SandboxTaskService sandboxTaskService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public SkillService(
            SkillDefinitionRepository skillDefinitionRepository,
            SkillVersionRepository skillVersionRepository,
            ApprovalRequestRepository approvalRequestRepository,
            SandboxTaskService sandboxTaskService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.skillDefinitionRepository = skillDefinitionRepository;
        this.skillVersionRepository = skillVersionRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.sandboxTaskService = sandboxTaskService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SkillDefinitionResponse> listActiveSkills() {
        return skillDefinitionRepository.findByEnabledTrueOrderByUpdatedAtDescIdDesc().stream()
                .filter(skill -> skill.getActiveVersionId() != null)
                .map(skill -> toResponse(skill, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillDefinitionResponse> listAllSkills() {
        return skillDefinitionRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .map(skill -> toResponse(skill, false))
                .toList();
    }

    @Transactional
    public SkillDefinitionResponse createSkill(String actor, CreateSkillRequest request) {
        String skillKey = normalizeSkillKey(request.skillKey());
        if (skillDefinitionRepository.existsBySkillKeyIgnoreCase(skillKey)) {
            throw new ApiRequestException(HttpStatus.CONFLICT, "Skill key already exists: " + skillKey);
        }
        SkillDefinition skill = skillDefinitionRepository.save(SkillDefinition.create(
                skillKey,
                request.name().trim(),
                normalizeOptional(request.description()),
                request.category(),
                actor
        ));
        SkillVersion version = skillVersionRepository.save(SkillVersion.create(
                skill,
                1,
                request.content().trim(),
                request.testScript().trim(),
                actor
        ));
        auditEventService.record(
                actor,
                "SKILL_CREATED",
                "Created skill " + skillKey + " with draft version " + version.getVersionNumber() + ".",
                RiskLevel.MEDIUM
        );
        return toResponse(skill, false);
    }

    @Transactional
    public SkillDefinitionResponse createVersion(String actor, Long skillId, CreateSkillVersionRequest request) {
        SkillDefinition skill = findSkill(skillId);
        int nextVersion = skillVersionRepository.findTopBySkill_IdOrderByVersionNumberDesc(skillId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        SkillVersion version = skillVersionRepository.save(SkillVersion.create(
                skill,
                nextVersion,
                request.content().trim(),
                request.testScript().trim(),
                actor
        ));
        auditEventService.record(
                actor,
                "SKILL_VERSION_CREATED",
                "Created draft version " + version.getVersionNumber() + " for skill " + skill.getSkillKey() + ".",
                RiskLevel.MEDIUM
        );
        return toResponse(skill, false);
    }

    @Transactional
    public SkillVersionResponse testVersion(Long userId, String actor, Long versionId) {
        SkillVersion version = findVersion(versionId);
        if (version.getStatus() == SkillVersionStatus.PENDING_APPROVAL
                || version.getStatus() == SkillVersionStatus.APPROVED
                || version.getStatus() == SkillVersionStatus.ACTIVE) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Only draft, tested, rejected, or archived versions can be re-tested.");
        }
        SandboxTaskResponse sandboxTask = sandboxTaskService.submit(
                userId,
                actor,
                new SandboxTaskRequest(SandboxTaskType.MOCK_BACKTEST, version.getTestScript(), 1200)
        );
        boolean passed = sandboxTask.status() == SandboxTaskStatus.COMPLETED && sandboxTask.output() != null;
        SkillTestResult testResult = new SkillTestResult(
                passed,
                sandboxTask.id(),
                sandboxTask.status().name(),
                passed ? sandboxTask.output().summary() : failureSummary(sandboxTask),
                List.of(
                        "Sandbox DSL accepted by governed executor.",
                        "Skill content stored as versioned metadata.",
                        "No unapproved skill version is activated by this test."
                ),
                sandboxTask.output() == null ? List.of("Sandbox task did not produce executable output.") : sandboxTask.output().riskWarnings(),
                Instant.now()
        );
        String resultJson = writeTestResult(testResult);
        if (passed) {
            version.markTested(resultJson);
            auditEventService.record(
                    actor,
                    "SKILL_VERSION_TESTED",
                    "Tested skill version " + version.getId() + " through sandbox task " + sandboxTask.id() + ".",
                    RiskLevel.MEDIUM
            );
        } else {
            version.recordTestResult(resultJson);
            auditEventService.record(
                    actor,
                    "SKILL_VERSION_TEST_FAILED",
                    "Skill version " + version.getId() + " failed sandbox test with status " + sandboxTask.status() + ".",
                    RiskLevel.HIGH
            );
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Skill version test did not complete: " + sandboxTask.status());
        }
        return toVersionResponse(version);
    }

    @Transactional
    public ApprovalResponse submitApproval(String actor, Long versionId, SubmitSkillApprovalRequest request) {
        SkillVersion version = findVersion(versionId);
        if (version.getStatus() != SkillVersionStatus.TESTED) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Only tested skill versions can be submitted for approval.");
        }
        approvalRequestRepository.findByTargetTypeAndTargetIdAndStatus(
                        ApprovalTargetType.SKILL_VERSION,
                        versionId,
                        ApprovalStatus.PENDING
                )
                .ifPresent(existing -> {
                    throw new ApiRequestException(HttpStatus.CONFLICT, "Skill version already has a pending approval request.");
                });
        String reason = normalizeDefault(
                request == null ? null : request.reason(),
                "Approve skill " + version.getSkill().getSkillKey() + " v" + version.getVersionNumber() + " for controlled activation."
        );
        version.submitApproval(reason);
        ApprovalRequest approval = approvalRequestRepository.save(ApprovalRequest.create(
                ApprovalRequestType.SKILL_VERSION_APPROVAL,
                ApprovalTargetType.SKILL_VERSION,
                versionId,
                reason,
                actor
        ));
        auditEventService.record(
                actor,
                "SKILL_VERSION_APPROVAL_REQUESTED",
                "Submitted skill version " + versionId + " for approval.",
                RiskLevel.HIGH
        );
        return ApprovalResponse.from(approval);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> listApprovals(ApprovalStatus status) {
        List<ApprovalRequest> approvals = status == null
                ? approvalRequestRepository.findAllByOrderByCreatedAtDescIdDesc()
                : approvalRequestRepository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return approvals.stream().map(ApprovalResponse::from).toList();
    }

    @Transactional
    public ApprovalResponse approve(String actor, Long approvalId, String comment) {
        ApprovalRequest approval = findPendingApproval(approvalId);
        SkillVersion version = findVersion(approval.getTargetId());
        if (version.getStatus() != SkillVersionStatus.PENDING_APPROVAL) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Target skill version is not pending approval.");
        }
        version.approve(actor);
        approval.approve(actor, normalizeOptional(comment));
        auditEventService.record(
                actor,
                "SKILL_VERSION_APPROVED",
                "Approved skill version " + version.getId() + ".",
                RiskLevel.HIGH
        );
        return ApprovalResponse.from(approval);
    }

    @Transactional
    public ApprovalResponse reject(String actor, Long approvalId, String comment) {
        ApprovalRequest approval = findPendingApproval(approvalId);
        SkillVersion version = findVersion(approval.getTargetId());
        String decision = normalizeDefault(comment, "Rejected by reviewer.");
        version.reject(actor, decision);
        approval.reject(actor, decision);
        auditEventService.record(
                actor,
                "SKILL_VERSION_REJECTED",
                "Rejected skill version " + version.getId() + ".",
                RiskLevel.HIGH
        );
        return ApprovalResponse.from(approval);
    }

    @Transactional
    public SkillDefinitionResponse activateVersion(String actor, Long versionId) {
        SkillVersion version = findVersion(versionId);
        if (version.getStatus() != SkillVersionStatus.APPROVED && version.getStatus() != SkillVersionStatus.ACTIVE) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Only approved skill versions can be activated.");
        }
        SkillDefinition skill = version.getSkill();
        Long previousActiveVersionId = skill.getActiveVersionId();
        if (previousActiveVersionId != null && !previousActiveVersionId.equals(versionId)) {
            skillVersionRepository.findById(previousActiveVersionId)
                    .ifPresent(SkillVersion::deactivateToApproved);
        }
        version.activate();
        skill.activateVersion(version.getId());
        auditEventService.record(
                actor,
                "SKILL_VERSION_ACTIVATED",
                "Activated skill " + skill.getSkillKey() + " v" + version.getVersionNumber() + ".",
                RiskLevel.HIGH
        );
        return toResponse(skill, false);
    }

    private SkillDefinition findSkill(Long skillId) {
        return skillDefinitionRepository.findById(skillId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "Skill not found"));
    }

    private SkillVersion findVersion(Long versionId) {
        return skillVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "Skill version not found"));
    }

    private ApprovalRequest findPendingApproval(Long approvalId) {
        ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "Approval request not found"));
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Approval request is not pending.");
        }
        if (approval.getRequestType() != ApprovalRequestType.SKILL_VERSION_APPROVAL
                || approval.getTargetType() != ApprovalTargetType.SKILL_VERSION) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Unsupported approval target.");
        }
        return approval;
    }

    private SkillDefinitionResponse toResponse(SkillDefinition skill, boolean activeOnly) {
        List<SkillVersionResponse> versions = skillVersionRepository.findBySkill_IdOrderByVersionNumberDesc(skill.getId()).stream()
                .filter(version -> !activeOnly || version.getId().equals(skill.getActiveVersionId()))
                .map(this::toVersionResponse)
                .toList();
        SkillVersionResponse activeVersion = versions.stream()
                .filter(version -> version.id().equals(skill.getActiveVersionId()))
                .findFirst()
                .orElseGet(() -> skill.getActiveVersionId() == null
                        ? null
                        : skillVersionRepository.findById(skill.getActiveVersionId()).map(this::toVersionResponse).orElse(null));
        return new SkillDefinitionResponse(
                skill.getId(),
                skill.getSkillKey(),
                skill.getName(),
                skill.getDescription(),
                skill.getCategory(),
                skill.isEnabled(),
                skill.getActiveVersionId(),
                activeVersion,
                versions,
                skill.getCreatedBy(),
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }

    private SkillVersionResponse toVersionResponse(SkillVersion version) {
        return new SkillVersionResponse(
                version.getId(),
                version.getSkill().getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getContent(),
                version.getTestScript(),
                readTestResult(version.getTestResultJson()),
                version.getApprovalReason(),
                version.getCreatedBy(),
                version.getReviewedBy(),
                version.getReviewedAt(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    private String writeTestResult(SkillTestResult testResult) {
        try {
            return objectMapper.writeValueAsString(testResult);
        } catch (JsonProcessingException ex) {
            throw new ApiRequestException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize skill test result.");
        }
    }

    private SkillTestResult readTestResult(String testResultJson) {
        if (testResultJson == null || testResultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(testResultJson, SkillTestResult.class);
        } catch (JsonProcessingException ex) {
            return new SkillTestResult(
                    false,
                    null,
                    "UNREADABLE",
                    "Skill test result could not be parsed.",
                    List.of("Stored test result JSON is invalid."),
                    List.of("Review the skill version before activation."),
                    Instant.now()
            );
        }
    }

    private String failureSummary(SandboxTaskResponse sandboxTask) {
        if (sandboxTask.errorMessage() != null && !sandboxTask.errorMessage().isBlank()) {
            return sandboxTask.errorMessage();
        }
        if (sandboxTask.approvalReason() != null && !sandboxTask.approvalReason().isBlank()) {
            return sandboxTask.approvalReason();
        }
        return "Sandbox task did not complete.";
    }

    private String normalizeSkillKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeDefault(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }
}
