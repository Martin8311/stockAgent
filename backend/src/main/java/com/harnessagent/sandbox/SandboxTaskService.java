package com.harnessagent.sandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.portfolio.PortfolioService;
import com.harnessagent.portfolio.PortfolioSummaryResponse;
import com.harnessagent.user.AppUser;
import com.harnessagent.user.UserRepository;
import com.harnessagent.web.ApiRequestException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SandboxTaskService {

    private final UserRepository userRepository;
    private final SandboxTaskRepository sandboxTaskRepository;
    private final SandboxPolicyService sandboxPolicyService;
    private final SandboxExecutor sandboxExecutor;
    private final PortfolioService portfolioService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public SandboxTaskService(
            UserRepository userRepository,
            SandboxTaskRepository sandboxTaskRepository,
            SandboxPolicyService sandboxPolicyService,
            SandboxExecutor sandboxExecutor,
            PortfolioService portfolioService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.sandboxTaskRepository = sandboxTaskRepository;
        this.sandboxPolicyService = sandboxPolicyService;
        this.sandboxExecutor = sandboxExecutor;
        this.portfolioService = portfolioService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SandboxTaskResponse submit(Long userId, String actor, SandboxTaskRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "User not found"));
        SandboxPolicyDecision decision = sandboxPolicyService.evaluate(request);
        SandboxTask task = SandboxTask.create(
                user,
                request.taskType(),
                decision.riskLevel(),
                decision.normalizedScript(),
                decision.timeoutMs()
        );

        if (decision.rejected()) {
            task.reject(decision.reason());
            SandboxTask saved = sandboxTaskRepository.save(task);
            auditEventService.record(
                    actor,
                    "SANDBOX_TASK_REJECTED",
                    "Rejected sandbox task " + saved.getId() + ": " + decision.reason(),
                    RiskLevel.CRITICAL
            );
            return toResponse(saved);
        }

        if (decision.approvalRequired()) {
            task.requireApproval(decision.reason());
            SandboxTask saved = sandboxTaskRepository.save(task);
            auditEventService.record(
                    actor,
                    "SANDBOX_TASK_REQUIRES_APPROVAL",
                    "Sandbox task " + saved.getId() + " requires approval: " + decision.reason(),
                    RiskLevel.HIGH
            );
            return toResponse(saved);
        }

        SandboxTask saved = sandboxTaskRepository.save(task);
        auditEventService.record(
                actor,
                "SANDBOX_TASK_SUBMITTED",
                "Submitted sandbox task " + saved.getId() + " of type " + saved.getTaskType() + ".",
                saved.getRiskLevel()
        );
        long startedAt = System.nanoTime();
        try {
            SandboxExecutionOutput output = sandboxExecutor.execute(new SandboxExecutionRequest(
                    saved.getTaskType(),
                    saved.getScript(),
                    saved.getTimeoutMs(),
                    portfolioContext(userId, actor, saved.getTaskType())
            ));
            long elapsedMs = elapsedMs(startedAt);
            if (elapsedMs > saved.getTimeoutMs()) {
                saved.fail("Sandbox task exceeded timeout of " + saved.getTimeoutMs() + " ms.", elapsedMs);
                auditEventService.record(
                        actor,
                        "SANDBOX_TASK_FAILED",
                        "Sandbox task " + saved.getId() + " exceeded timeout.",
                        RiskLevel.HIGH
                );
                return toResponse(saved);
            }
            saved.complete(objectMapper.writeValueAsString(output), elapsedMs);
            auditEventService.record(
                    actor,
                    "SANDBOX_TASK_COMPLETED",
                    "Completed sandbox task " + saved.getId() + " in " + elapsedMs + " ms.",
                    saved.getRiskLevel()
            );
            return toResponse(saved);
        } catch (RuntimeException | JsonProcessingException ex) {
            long elapsedMs = elapsedMs(startedAt);
            saved.fail(ex.getMessage(), elapsedMs);
            auditEventService.record(
                    actor,
                    "SANDBOX_TASK_FAILED",
                    "Sandbox task " + saved.getId() + " failed: " + ex.getMessage(),
                    RiskLevel.HIGH
            );
            return toResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public SandboxTaskResponse get(Long userId, Long taskId) {
        SandboxTask task = sandboxTaskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "Sandbox task not found"));
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<SandboxTaskResponse> list(Long userId) {
        return sandboxTaskRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PortfolioSummaryResponse portfolioContext(Long userId, String actor, SandboxTaskType taskType) {
        if (taskType != SandboxTaskType.PORTFOLIO_STRESS_TEST) {
            return null;
        }
        return portfolioService.getSummary(userId, actor);
    }

    private SandboxTaskResponse toResponse(SandboxTask task) {
        return SandboxTaskResponse.from(task, readOutput(task.getOutputJson()));
    }

    private SandboxExecutionOutput readOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(outputJson, SandboxExecutionOutput.class);
        } catch (JsonProcessingException ex) {
            return new SandboxExecutionOutput(
                    "Sandbox output could not be parsed.",
                    java.util.Map.of(),
                    List.of("Stored output was not valid JSON."),
                    List.of("Review task audit logs before using this result."),
                    "Sandbox results are educational simulations only."
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return java.time.Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
