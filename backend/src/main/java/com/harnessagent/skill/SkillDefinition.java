package com.harnessagent.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "skill_definition")
public class SkillDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_key", nullable = false, length = 80, unique = true)
    private String skillKey;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SkillCategory category;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "active_version_id")
    private Long activeVersionId;

    @Column(name = "created_by", nullable = false, length = 160)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillDefinition() {
    }

    private SkillDefinition(
            String skillKey,
            String name,
            String description,
            SkillCategory category,
            String createdBy
    ) {
        this.skillKey = skillKey;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = true;
        this.createdBy = createdBy;
    }

    public static SkillDefinition create(
            String skillKey,
            String name,
            String description,
            SkillCategory category,
            String createdBy
    ) {
        return new SkillDefinition(skillKey, name, description, category, createdBy);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void activateVersion(Long versionId) {
        this.activeVersionId = versionId;
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getSkillKey() {
        return skillKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getActiveVersionId() {
        return activeVersionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
