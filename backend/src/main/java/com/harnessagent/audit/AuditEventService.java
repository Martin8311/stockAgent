package com.harnessagent.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent record(String actor, String eventType, String description, RiskLevel riskLevel) {
        AuditEvent event = AuditEvent.create(eventType, actor, description, riskLevel);
        return auditEventRepository.save(event);
    }
}

