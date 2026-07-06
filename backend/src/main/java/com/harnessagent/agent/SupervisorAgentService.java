package com.harnessagent.agent;

import com.harnessagent.marketdata.MarketDataSourceType;
import com.harnessagent.marketdata.MarketQuote;
import com.harnessagent.portfolio.PortfolioSummaryResponse;
import com.harnessagent.user.UserProfile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SupervisorAgentService {

    private static final List<String> HIGH_RISK_INTENT_KEYWORDS = List.of(
            "buy",
            "sell",
            "recommend",
            "recommendation",
            "rebalance",
            "should i",
            "must",
            "\u4e70",
            "\u5356",
            "\u63a8\u8350",
            "\u5efa\u8bae",
            "\u518d\u5e73\u8861",
            "\u5fc5\u987b"
    );

    private static final List<String> PROHIBITED_OUTPUT_KEYWORDS = List.of(
            "guaranteed profit",
            "guaranteed return",
            "must buy",
            "sure win",
            "\u7a33\u8d5a",
            "\u5fc5\u4e70",
            "\u4fdd\u8bc1\u4e0a\u6da8",
            "\u4fdd\u8bc1\u6536\u76ca"
    );

    public AgentWorkflowResponse orchestrate(
            String workflowId,
            AiAnalysisRequest request,
            AiModelDescriptor model,
            MarketQuote quote,
            PortfolioSummaryResponse portfolioSummary,
            UserProfile profile,
            InvestmentAnalysisContent content,
            String disclaimer
    ) {
        boolean highRiskIntent = hasHighRiskIntent(request.question());
        List<AgentStepResponse> steps = List.of(
                marketDataStep(quote),
                portfolioStep(request, portfolioSummary),
                riskStep(highRiskIntent, quote, portfolioSummary, profile, content),
                strategyStep(highRiskIntent, model, content),
                complianceStep(highRiskIntent, content, disclaimer)
        );
        List<String> approvalReasons = deduplicate(steps.stream()
                .filter(AgentStepResponse::requiresHumanApproval)
                .map(AgentStepResponse::approvalReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .toList());
        return new AgentWorkflowResponse(
                workflowId,
                approvalReasons.isEmpty() ? AgentWorkflowStatus.COMPLETED : AgentWorkflowStatus.HUMAN_REVIEW_REQUIRED,
                !approvalReasons.isEmpty(),
                approvalReasons,
                steps
        );
    }

    private AgentStepResponse marketDataStep(MarketQuote quote) {
        List<String> observations = List.of(
                "Quote provider=" + quote.provider() + ", sourceType=" + quote.sourceType(),
                "Latest=" + format(quote.latestPrice()) + ", previousClose=" + format(quote.previousClose()),
                "Quote confidence=" + formatPercent(quote.confidence())
        );
        boolean reviewRequired = quote.sourceType() != MarketDataSourceType.MOCK
                || quote.confidence().compareTo(new BigDecimal("0.300000")) < 0;
        return step(
                "MarketDataAgent",
                AgentRole.MARKET_DATA,
                reviewRequired ? AgentStepStatus.REVIEW_REQUIRED : AgentStepStatus.COMPLETED,
                "Validated market quote for " + quote.symbol() + " on " + quote.exchange() + ".",
                observations,
                quote.riskWarnings(),
                reviewRequired,
                reviewRequired ? "Market data source is external or too low confidence, so human review is required before relying on it." : null
        );
    }

    private AgentStepResponse portfolioStep(AiAnalysisRequest request, PortfolioSummaryResponse portfolioSummary) {
        if (!request.shouldIncludePortfolioContext()) {
            return step(
                    "PortfolioAgent",
                    AgentRole.PORTFOLIO,
                    AgentStepStatus.SKIPPED,
                    "Portfolio context was not requested for this analysis.",
                    List.of("The user disabled portfolio context for this run."),
                    List.of(),
                    false,
                    null
            );
        }
        if (portfolioSummary == null) {
            return step(
                    "PortfolioAgent",
                    AgentRole.PORTFOLIO,
                    AgentStepStatus.REVIEW_REQUIRED,
                    "Portfolio context was requested but no summary was available.",
                    List.of(),
                    List.of("Portfolio risk cannot be evaluated without holdings and transaction context."),
                    true,
                    "Requested portfolio context is unavailable."
            );
        }
        List<String> observations = List.of(
                "Holding count=" + portfolioSummary.holdingCount(),
                "Total market value=" + format(portfolioSummary.totalMarketValue()),
                "Total unrealized P/L=" + format(portfolioSummary.totalUnrealizedPnl())
        );
        boolean emptyPortfolio = portfolioSummary.holdingCount() == 0;
        return step(
                "PortfolioAgent",
                AgentRole.PORTFOLIO,
                emptyPortfolio ? AgentStepStatus.REVIEW_REQUIRED : AgentStepStatus.COMPLETED,
                "Reviewed portfolio context and current holding risk signals.",
                observations,
                portfolioSummary.riskWarnings(),
                emptyPortfolio,
                emptyPortfolio ? "Personalized portfolio analysis requires at least one recorded holding." : null
        );
    }

    private AgentStepResponse riskStep(
            boolean highRiskIntent,
            MarketQuote quote,
            PortfolioSummaryResponse portfolioSummary,
            UserProfile profile,
            InvestmentAnalysisContent content
    ) {
        List<String> riskWarnings = new ArrayList<>();
        riskWarnings.addAll(quote.riskWarnings());
        if (portfolioSummary != null) {
            riskWarnings.addAll(portfolioSummary.riskWarnings());
        }
        riskWarnings.addAll(content.riskWarnings());
        boolean missingProfile = profile == null || profile.getRiskPreference().name().equals("UNKNOWN");
        if (missingProfile) {
            riskWarnings.add("User risk preference is incomplete, so personalized analysis should remain conservative.");
        }
        boolean reviewRequired = highRiskIntent || missingProfile;
        List<String> observations = List.of(
                "High-risk intent detected=" + highRiskIntent,
                "Risk profile complete=" + !missingProfile,
                "Aggregated risk warning count=" + deduplicate(riskWarnings).size()
        );
        return step(
                "RiskAgent",
                AgentRole.RISK,
                reviewRequired ? AgentStepStatus.REVIEW_REQUIRED : AgentStepStatus.COMPLETED,
                "Aggregated market, portfolio, profile, and model risk signals.",
                observations,
                deduplicate(riskWarnings),
                reviewRequired,
                reviewRequired ? "High-risk intent or incomplete suitability profile requires human review." : null
        );
    }

    private AgentStepResponse strategyStep(
            boolean highRiskIntent,
            AiModelDescriptor model,
            InvestmentAnalysisContent content
    ) {
        List<String> observations = new ArrayList<>();
        observations.add("Model=" + model.displayName() + ", provider=" + model.provider());
        observations.addAll(content.keyObservations());
        boolean reviewRequired = highRiskIntent && model.paidTier();
        return step(
                "StrategyAgent",
                AgentRole.STRATEGY,
                reviewRequired ? AgentStepStatus.REVIEW_REQUIRED : AgentStepStatus.COMPLETED,
                "Generated educational strategy explanation from the selected model output.",
                deduplicate(observations),
                content.riskWarnings(),
                reviewRequired,
                reviewRequired ? "Paid-tier model output with high-risk intent should be reviewed before user-facing use." : null
        );
    }

    private AgentStepResponse complianceStep(
            boolean highRiskIntent,
            InvestmentAnalysisContent content,
            String disclaimer
    ) {
        boolean hasRiskWarnings = content.riskWarnings() != null && !content.riskWarnings().isEmpty();
        boolean hasAssumptions = content.assumptions() != null && !content.assumptions().isEmpty();
        boolean hasDisclaimer = disclaimer != null && !disclaimer.isBlank();
        boolean containsProhibitedPhrase = containsProhibitedPhrase(content);
        boolean reviewRequired = highRiskIntent || !hasRiskWarnings || !hasAssumptions || !hasDisclaimer || containsProhibitedPhrase;
        List<String> observations = List.of(
                "Risk warnings present=" + hasRiskWarnings,
                "Assumptions present=" + hasAssumptions,
                "Disclaimer present=" + hasDisclaimer,
                "Prohibited phrase detected=" + containsProhibitedPhrase
        );
        List<String> riskWarnings = new ArrayList<>();
        if (!hasRiskWarnings) {
            riskWarnings.add("Model output did not include risk warnings.");
        }
        if (!hasAssumptions) {
            riskWarnings.add("Model output did not include assumptions.");
        }
        if (!hasDisclaimer) {
            riskWarnings.add("Compliance disclaimer is missing.");
        }
        if (containsProhibitedPhrase) {
            riskWarnings.add("Model output contains prohibited deterministic investment language.");
        }
        return step(
                "ComplianceAgent",
                AgentRole.COMPLIANCE,
                reviewRequired ? AgentStepStatus.REVIEW_REQUIRED : AgentStepStatus.COMPLETED,
                "Checked disclaimers, assumptions, risk warnings, and prohibited investment language.",
                observations,
                riskWarnings,
                reviewRequired,
                reviewRequired ? "Investment-related output or compliance gaps require human review before stronger recommendations." : null
        );
    }

    private AgentStepResponse step(
            String agentName,
            AgentRole role,
            AgentStepStatus status,
            String summary,
            List<String> observations,
            List<String> riskWarnings,
            boolean requiresHumanApproval,
            String approvalReason
    ) {
        return new AgentStepResponse(
                agentName,
                role,
                status,
                summary,
                deduplicate(observations),
                deduplicate(riskWarnings),
                requiresHumanApproval,
                approvalReason
        );
    }

    private boolean hasHighRiskIntent(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return HIGH_RISK_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean containsProhibitedPhrase(InvestmentAnalysisContent content) {
        String combined = String.join(
                "\n",
                List.of(
                        safe(content.investmentSummary()),
                        String.join("\n", safeList(content.keyObservations())),
                        String.join("\n", safeList(content.assumptions())),
                        String.join("\n", safeList(content.riskWarnings())),
                        String.join("\n", safeList(content.educationalNotes()))
                )
        ).toLowerCase(Locale.ROOT);
        return PROHIBITED_OUTPUT_KEYWORDS.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(combined::contains);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> deduplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList()));
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "unknown";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "unknown";
        }
        return value.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }
}
