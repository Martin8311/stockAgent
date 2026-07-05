package com.harnessagent.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(nullable = false, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    private AuditEvent(String eventType, String actor, String description, RiskLevel riskLevel, Instant createdAt) {
        this.eventType = eventType;
        this.actor = actor;
        this.description = description;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
    }

    public static AuditEvent create(String eventType, String actor, String description, RiskLevel riskLevel) {
        return new AuditEvent(eventType, actor, description, riskLevel, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getDescription() {
        return description;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

