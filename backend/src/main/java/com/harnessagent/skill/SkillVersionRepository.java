package com.harnessagent.skill;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillVersionRepository extends JpaRepository<SkillVersion, Long> {

    List<SkillVersion> findBySkill_IdOrderByVersionNumberDesc(Long skillId);

    Optional<SkillVersion> findTopBySkill_IdOrderByVersionNumberDesc(Long skillId);

    Optional<SkillVersion> findByIdAndSkill_Id(Long id, Long skillId);
}
