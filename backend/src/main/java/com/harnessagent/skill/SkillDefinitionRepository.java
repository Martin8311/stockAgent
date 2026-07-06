package com.harnessagent.skill;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinition, Long> {

    boolean existsBySkillKeyIgnoreCase(String skillKey);

    Optional<SkillDefinition> findBySkillKeyIgnoreCase(String skillKey);

    List<SkillDefinition> findAllByOrderByUpdatedAtDescIdDesc();

    List<SkillDefinition> findByEnabledTrueOrderByUpdatedAtDescIdDesc();
}
