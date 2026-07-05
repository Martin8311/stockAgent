package com.harnessagent.web;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.compliance.ComplianceNotice;
import com.harnessagent.compliance.ComplianceNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicSystemController {

    private final ComplianceNoticeService complianceNoticeService;
    private final AuditEventService auditEventService;

    public PublicSystemController(ComplianceNoticeService complianceNoticeService, AuditEventService auditEventService) {
        this.complianceNoticeService = complianceNoticeService;
        this.auditEventService = auditEventService;
    }

    @GetMapping("/system/health")
    @Operation(summary = "Get public system readiness status")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.ok(new SystemHealthResponse(
                "harness-engineering-intelligent-assistant",
                "PHASE_0",
                "UP",
                true,
                Instant.now()
        ));
    }

    @GetMapping("/compliance/disclaimer")
    @Operation(summary = "Get required investment risk disclosure")
    public ApiResponse<ComplianceNotice> disclaimer() {
        auditEventService.record(
                "anonymous",
                "COMPLIANCE_NOTICE_VIEWED",
                "Public compliance disclaimer was requested.",
                RiskLevel.LOW
        );
        return ApiResponse.ok(complianceNoticeService.getDefaultNotice(), "Compliance notice loaded");
    }
}

