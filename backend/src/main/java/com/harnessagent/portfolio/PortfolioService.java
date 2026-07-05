package com.harnessagent.portfolio;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.user.AppUser;
import com.harnessagent.user.UserRepository;
import com.harnessagent.web.ApiRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private static final int SCALE = 6;
    private static final String DEFAULT_EXCHANGE = "GLOBAL";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String PORTFOLIO_DISCLAIMER =
            "Portfolio analytics are educational and risk-oriented only. They do not promise returns and do not replace advice from a licensed financial advisor.";

    private final UserRepository userRepository;
    private final InvestmentAssetRepository assetRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final AuditEventService auditEventService;

    public PortfolioService(
            UserRepository userRepository,
            InvestmentAssetRepository assetRepository,
            PortfolioTransactionRepository transactionRepository,
            AuditEventService auditEventService
    ) {
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.transactionRepository = transactionRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public List<PortfolioTransactionResponse> listTransactions(Long userId) {
        return transactionRepository.findByUser_IdOrderByTradedAtDescIdDesc(userId).stream()
                .map(PortfolioTransactionResponse::from)
                .toList();
    }

    @Transactional
    public PortfolioTransactionResponse recordTransaction(Long userId, String actor, PortfolioTransactionRequest request) {
        AppUser user = loadUser(userId);
        InvestmentAsset asset = upsertAsset(request);
        PortfolioTransaction transaction = PortfolioTransaction.create(
                user,
                asset,
                request.transactionType(),
                normalizePositive(request.quantity(), "quantity"),
                normalizePositive(request.price(), "price"),
                normalizeFee(request.fee()),
                request.tradedAt() == null ? Instant.now() : request.tradedAt(),
                normalizeOptional(request.note())
        );
        PortfolioTransaction saved = transactionRepository.save(transaction);
        validateNoNegativePosition(userId);
        auditEventService.record(
                actor,
                "PORTFOLIO_TRANSACTION_RECORDED",
                "Recorded " + request.transactionType() + " transaction for " + asset.getSymbol() + ".",
                request.transactionType() == TransactionType.SELL ? RiskLevel.MEDIUM : RiskLevel.LOW
        );
        return PortfolioTransactionResponse.from(saved);
    }

    @Transactional
    public void deleteTransaction(Long userId, String actor, Long transactionId) {
        PortfolioTransaction transaction = transactionRepository.findByIdAndUser_Id(transactionId, userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "Transaction not found"));
        String symbol = transaction.getAsset().getSymbol();
        transactionRepository.delete(transaction);
        validateNoNegativePosition(userId);
        auditEventService.record(
                actor,
                "PORTFOLIO_TRANSACTION_DELETED",
                "Deleted portfolio transaction for " + symbol + ".",
                RiskLevel.MEDIUM
        );
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary(Long userId) {
        List<HoldingResponse> holdings = buildAccumulators(userId).values().stream()
                .filter(HoldingAccumulator::hasOpenPosition)
                .map(HoldingAccumulator::toResponse)
                .sorted(Comparator.comparing((HoldingResponse holding) -> holding.asset().symbol()))
                .toList();

        BigDecimal totalCostBasis = sum(holdings, HoldingResponse::costBasis);
        BigDecimal totalMarketValue = sum(holdings, HoldingResponse::marketValue);
        BigDecimal totalUnrealizedPnl = sum(holdings, HoldingResponse::unrealizedPnl);
        BigDecimal totalRealizedPnl = buildAccumulators(userId).values().stream()
                .map(HoldingAccumulator::realizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRatio = totalCostBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalUnrealizedPnl.divide(totalCostBasis, SCALE, RoundingMode.HALF_UP);

        return new PortfolioSummaryResponse(
                totalCostBasis,
                totalMarketValue,
                totalUnrealizedPnl,
                totalRatio,
                totalRealizedPnl,
                holdings.size(),
                holdings,
                buildRiskWarnings(holdings, totalMarketValue, totalRatio),
                PORTFOLIO_DISCLAIMER
        );
    }

    private InvestmentAsset upsertAsset(PortfolioTransactionRequest request) {
        String symbol = normalizeRequired(request.symbol(), "symbol").toUpperCase(Locale.ROOT);
        String exchange = normalizeDefault(request.exchange(), DEFAULT_EXCHANGE).toUpperCase(Locale.ROOT);
        String currency = normalizeDefault(request.currency(), DEFAULT_CURRENCY).toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.name(), "name");
        AssetType assetType = request.assetType() == null ? AssetType.OTHER : request.assetType();
        BigDecimal latestPrice = normalizePositive(request.price(), "price");

        InvestmentAsset asset = assetRepository
                .findBySymbolIgnoreCaseAndExchangeIgnoreCaseAndCurrencyIgnoreCase(symbol, exchange, currency)
                .orElseGet(() -> InvestmentAsset.create(symbol, name, assetType, exchange, currency, latestPrice));
        asset.updateMetadata(name, assetType, latestPrice);
        return assetRepository.save(asset);
    }

    private void validateNoNegativePosition(Long userId) {
        buildAccumulators(userId);
    }

    private Map<Long, HoldingAccumulator> buildAccumulators(Long userId) {
        Map<Long, HoldingAccumulator> accumulators = new LinkedHashMap<>();
        for (PortfolioTransaction transaction : transactionRepository.findByUser_IdOrderByTradedAtAscIdAsc(userId)) {
            HoldingAccumulator accumulator = accumulators.computeIfAbsent(
                    transaction.getAsset().getId(),
                    ignored -> new HoldingAccumulator(transaction.getAsset())
            );
            if (accumulator.wouldGoNegative(transaction)) {
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "Sell quantity exceeds available holding for " + transaction.getAsset().getSymbol()
                );
            }
            accumulator.apply(transaction);
        }
        return accumulators;
    }

    private List<String> buildRiskWarnings(
            List<HoldingResponse> holdings,
            BigDecimal totalMarketValue,
            BigDecimal totalRatio
    ) {
        if (holdings.isEmpty()) {
            return List.of("No holdings recorded yet. Portfolio risk cannot be assessed without position data.");
        }

        List<String> warnings = new java.util.ArrayList<>();
        if (holdings.size() == 1) {
            warnings.add("Single-position portfolio detected. Concentration risk may be high.");
        }
        holdings.stream()
                .filter(holding -> totalMarketValue.compareTo(BigDecimal.ZERO) > 0)
                .filter(holding -> holding.marketValue()
                        .divide(totalMarketValue, SCALE, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal("0.50")) > 0)
                .findFirst()
                .ifPresent(holding -> warnings.add(
                        holding.asset().symbol() + " is more than 50% of portfolio market value. Review concentration risk."
                ));
        if (totalRatio.compareTo(new BigDecimal("-0.10")) < 0) {
            warnings.add("Portfolio unrealized loss is greater than 10% of cost basis. Review risk tolerance and liquidity needs.");
        }
        warnings.add(PORTFOLIO_DISCLAIMER);
        return warnings;
    }

    private AppUser loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private BigDecimal sum(List<HoldingResponse> holdings, java.util.function.Function<HoldingResponse, BigDecimal> mapper) {
        return holdings.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return normalized;
    }

    private String normalizeDefault(String value, String defaultValue) {
        String normalized = normalizeOptional(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal normalizePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " must be greater than zero");
        }
        return value;
    }

    private BigDecimal normalizeFee(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "fee cannot be negative");
        }
        return value;
    }
}
