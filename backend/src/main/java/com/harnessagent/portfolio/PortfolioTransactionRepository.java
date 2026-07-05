package com.harnessagent.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    List<PortfolioTransaction> findByUser_IdOrderByTradedAtAscIdAsc(Long userId);

    List<PortfolioTransaction> findByUser_IdOrderByTradedAtDescIdDesc(Long userId);

    Optional<PortfolioTransaction> findByIdAndUser_Id(Long id, Long userId);
}
