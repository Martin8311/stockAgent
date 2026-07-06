package com.harnessagent.sandbox;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SandboxTaskRepository extends JpaRepository<SandboxTask, Long> {

    List<SandboxTask> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<SandboxTask> findByIdAndUser_Id(Long id, Long userId);
}
