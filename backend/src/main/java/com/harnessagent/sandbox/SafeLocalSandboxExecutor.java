package com.harnessagent.sandbox;

import com.harnessagent.portfolio.HoldingResponse;
import com.harnessagent.portfolio.PortfolioSummaryResponse;
import com.harnessagent.web.ApiRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SafeLocalSandboxExecutor implements SandboxExecutor {

    private static final String DISCLAIMER =
            "Sandbox results are deterministic educational simulations only. They are not investment advice and do not predict future returns.";

    @Override
    public SandboxExecutionOutput execute(SandboxExecutionRequest request) {
        Map<String, String> params = parseKeyValueScript(request.script());
        return switch (request.taskType()) {
            case MOCK_BACKTEST -> executeMockBacktest(params);
            case PORTFOLIO_STRESS_TEST -> executePortfolioStressTest(params, request.portfolioSummary());
        };
    }

    private SandboxExecutionOutput executeMockBacktest(Map<String, String> params) {
        String symbol = params.getOrDefault("symbol", "AAPL").trim().toUpperCase(Locale.ROOT);
        BigDecimal initialCapital = positiveDecimal(params.get("initialCapital"), new BigDecimal("10000"), "initialCapital");
        int lookbackDays = boundedInt(params.get("lookbackDays"), 60, 5, 365, "lookbackDays");
        String strategy = params.getOrDefault("strategy", "moving-average-cross");

        BigDecimal drift = new BigDecimal(Math.abs(symbol.hashCode() % 17) - 6)
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal volatilityPenalty = new BigDecimal(lookbackDays)
                .divide(new BigDecimal("36500"), 6, RoundingMode.HALF_UP);
        BigDecimal estimatedReturn = drift.subtract(volatilityPenalty).setScale(6, RoundingMode.HALF_UP);
        BigDecimal endingCapital = initialCapital.multiply(BigDecimal.ONE.add(estimatedReturn))
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal maxDrawdown = volatilityPenalty.multiply(new BigDecimal("2.5")).negate()
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal winRate = new BigDecimal("0.480000").add(drift.multiply(new BigDecimal("4")))
                .max(new BigDecimal("0.050000"))
                .min(new BigDecimal("0.950000"))
                .setScale(6, RoundingMode.HALF_UP);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("symbol", symbol);
        metrics.put("strategy", strategy);
        metrics.put("initialCapital", initialCapital);
        metrics.put("endingCapital", endingCapital);
        metrics.put("estimatedReturn", estimatedReturn);
        metrics.put("maxDrawdown", maxDrawdown);
        metrics.put("winRate", winRate);
        metrics.put("lookbackDays", lookbackDays);

        return new SandboxExecutionOutput(
                "Completed deterministic mock backtest for " + symbol + " using " + strategy + ".",
                metrics,
                List.of(
                        "Execution used a deterministic local formula, not live market data.",
                        "No external network, file, shell, or environment access was performed.",
                        "The result is suitable for validating workflow and reporting, not for trading."
                ),
                List.of(
                        "Mock backtest does not represent historical or future performance.",
                        "Backtest results can be distorted by assumptions, overfitting, fees, liquidity, and slippage.",
                        "Do not use this output as a buy, sell, or hold recommendation."
                ),
                DISCLAIMER
        );
    }

    private SandboxExecutionOutput executePortfolioStressTest(
            Map<String, String> params,
            PortfolioSummaryResponse portfolioSummary
    ) {
        if (portfolioSummary == null || portfolioSummary.holdings().isEmpty()) {
            return new SandboxExecutionOutput(
                    "Portfolio stress test could not find open holdings.",
                    Map.of("holdingCount", 0),
                    List.of("No holdings were available for stress testing."),
                    List.of("Portfolio stress testing requires recorded holdings and current valuation data."),
                    DISCLAIMER
            );
        }

        BigDecimal defaultShock = decimal(params.get("shockPercent"), new BigDecimal("-0.100000"), "shockPercent");
        if (defaultShock.compareTo(new BigDecimal("-0.950000")) < 0 || defaultShock.compareTo(new BigDecimal("0.950000")) > 0) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, "shockPercent must be between -0.95 and 0.95");
        }

        BigDecimal baseline = portfolioSummary.totalMarketValue();
        BigDecimal stressed = BigDecimal.ZERO;
        Map<String, Object> perHolding = new LinkedHashMap<>();
        for (HoldingResponse holding : portfolioSummary.holdings()) {
            String symbol = holding.asset().symbol();
            BigDecimal shock = decimal(params.get("shock." + symbol), defaultShock, "shock." + symbol);
            BigDecimal stressedValue = holding.marketValue().multiply(BigDecimal.ONE.add(shock))
                    .setScale(6, RoundingMode.HALF_UP);
            stressed = stressed.add(stressedValue);
            perHolding.put(symbol, Map.of(
                    "baselineMarketValue", holding.marketValue(),
                    "shockPercent", shock,
                    "stressedMarketValue", stressedValue,
                    "estimatedImpact", stressedValue.subtract(holding.marketValue()).setScale(6, RoundingMode.HALF_UP)
            ));
        }
        BigDecimal estimatedImpact = stressed.subtract(baseline).setScale(6, RoundingMode.HALF_UP);
        BigDecimal impactRatio = baseline.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : estimatedImpact.divide(baseline, 6, RoundingMode.HALF_UP);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("baselineMarketValue", baseline);
        metrics.put("stressedMarketValue", stressed.setScale(6, RoundingMode.HALF_UP));
        metrics.put("estimatedImpact", estimatedImpact);
        metrics.put("estimatedImpactRatio", impactRatio);
        metrics.put("defaultShockPercent", defaultShock);
        metrics.put("holdingCount", portfolioSummary.holdingCount());
        metrics.put("perHolding", perHolding);

        return new SandboxExecutionOutput(
                "Completed portfolio stress test with default shock " + defaultShock + ".",
                metrics,
                List.of(
                        "Stress test used current portfolio summary and deterministic shock parameters.",
                        "No external data source or generated script was executed.",
                        "Per-holding shocks can be supplied with keys such as shock.AAPL=-0.20."
                ),
                List.of(
                        "Stress tests are scenario analyses, not forecasts.",
                        "Real losses can differ due to liquidity, correlation, gaps, fees, and market conditions.",
                        "Review concentration and personal risk tolerance before taking action."
                ),
                DISCLAIMER
        );
    }

    private Map<String, String> parseKeyValueScript(String script) {
        Map<String, String> values = new LinkedHashMap<>();
        String[] entries = script.split("[\\r\\n;,]+");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex < 1 || equalsIndex == trimmed.length() - 1) {
                throw new ApiRequestException(HttpStatus.BAD_REQUEST, "Sandbox DSL must use key=value entries.");
            }
            values.put(trimmed.substring(0, equalsIndex).trim(), trimmed.substring(equalsIndex + 1).trim());
        }
        return values;
    }

    private BigDecimal positiveDecimal(String value, BigDecimal fallback, String field) {
        BigDecimal decimal = decimal(value, fallback, field);
        if (decimal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " must be greater than zero");
        }
        return decimal;
    }

    private BigDecimal decimal(String value, BigDecimal fallback, String field) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim()).setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " must be a decimal number");
        }
    }

    private int boundedInt(String value, int fallback, int min, int max, String field) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " must be between " + min + " and " + max);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " must be an integer");
        }
    }
}
