package com.harnessagent.approval;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findAllByOrderByCreatedAtDescIdDesc();

    List<ApprovalRequest> findByStatusOrderByCreatedAtDescIdDesc(ApprovalStatus status);

    Optional<ApprovalRequest> findByTargetTypeAndTargetIdAndStatus(
            ApprovalTargetType targetType,
            Long targetId,
            ApprovalStatus status
    );
}
