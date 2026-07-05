package com.harnessagent.portfolio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentAssetRepository extends JpaRepository<InvestmentAsset, Long> {

    Optional<InvestmentAsset> findBySymbolIgnoreCaseAndExchangeIgnoreCaseAndCurrencyIgnoreCase(
            String symbol,
            String exchange,
            String currency
    );
}
