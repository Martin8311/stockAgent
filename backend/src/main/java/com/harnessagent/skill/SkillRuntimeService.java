package com.harnessagent.skill;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillRuntimeService {

    private final SkillDefinitionRepository skillDefinitionRepository;
    private final SkillVersionRepository skillVersionRepository;

    public SkillRuntimeService(
            SkillDefinitionRepository skillDefinitionRepository,
            SkillVersionRepository skillVersionRepository
    ) {
        this.skillDefinitionRepository = skillDefinitionRepository;
        this.skillVersionRepository = skillVersionRepository;
    }

    @Transactional(readOnly = true)
    public String activeSkillContext() {
        List<String> activeSkills = new ArrayList<>();
        skillDefinitionRepository.findByEnabledTrueOrderByUpdatedAtDescIdDesc().forEach(skill -> {
            if (skill.getActiveVersionId() == null) {
                return;
            }
            skillVersionRepository.findById(skill.getActiveVersionId())
                    .filter(version -> version.getStatus() == SkillVersionStatus.ACTIVE)
                    .ifPresent(version -> activeSkills.add("%s v%s (%s): %s".formatted(
                            skill.getSkillKey(),
                            version.getVersionNumber(),
                            skill.getCategory(),
                            summarize(version.getContent())
                    )));
        });
        if (activeSkills.isEmpty()) {
            return "No active governed skills.";
        }
        return String.join("\n", activeSkills);
    }

    private String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 600 ? normalized : normalized.substring(0, 600);
    }
}
