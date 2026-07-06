package com.harnessagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.compliance.ComplianceProperties;
import com.harnessagent.marketdata.MarketDataRequest;
import com.harnessagent.marketdata.MarketDataService;
import com.harnessagent.marketdata.MarketQuote;
import com.harnessagent.portfolio.PortfolioService;
import com.harnessagent.portfolio.PortfolioSummaryResponse;
import com.harnessagent.skill.SkillRuntimeService;
import com.harnessagent.user.AppUser;
import com.harnessagent.user.UserProfile;
import com.harnessagent.user.UserProfileRepository;
import com.harnessagent.user.UserRepository;
import com.harnessagent.web.ApiRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InvestmentAnalysisService {

    private static final Set<String> PROHIBITED_PHRASES = Set.of(
            "guaranteed profit",
            "guaranteed return",
            "must buy",
            "sure win",
            "稳赚",
            "必买",
            "保证上涨",
            "保证收益"
    );

    private final AiProperties properties;
    private final AiModelCatalog modelCatalog;
    private final List<AiModelGateway> gateways;
    private final DeterministicAiModelGateway deterministicGateway;
    private final TokenBillingService tokenBillingService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final ComplianceProperties complianceProperties;
    private final AuditEventService auditEventService;
    private final AiAnalysisTaskRepository analysisTaskRepository;
    private final SupervisorAgentService supervisorAgentService;
    private final SkillRuntimeService skillRuntimeService;
    private final ObjectMapper objectMapper;

    public InvestmentAnalysisService(
            AiProperties properties,
            AiModelCatalog modelCatalog,
            List<AiModelGateway> gateways,
            DeterministicAiModelGateway deterministicGateway,
            TokenBillingService tokenBillingService,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            MarketDataService marketDataService,
            PortfolioService portfolioService,
            ComplianceProperties complianceProperties,
            AuditEventService auditEventService,
            AiAnalysisTaskRepository analysisTaskRepository,
            SupervisorAgentService supervisorAgentService,
            SkillRuntimeService skillRuntimeService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.modelCatalog = modelCatalog;
        this.gateways = gateways;
        this.deterministicGateway = deterministicGateway;
        this.tokenBillingService = tokenBillingService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.complianceProperties = complianceProperties;
        this.auditEventService = auditEventService;
        this.analysisTaskRepository = analysisTaskRepository;
        this.supervisorAgentService = supervisorAgentService;
        this.skillRuntimeService = skillRuntimeService;
        this.objectMapper = objectMapper;
    }

    public InvestmentAnalysisResponse analyze(Long userId, String actor, AiAnalysisRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "User not found"));
        AiModelDescriptor model = modelCatalog.resolve(request.modelId());
        ensureModelUsable(model);

        String symbol = normalizeRequired(request.symbol(), "symbol").toUpperCase(Locale.ROOT);
        String exchange = normalizeDefault(request.exchange(), "GLOBAL").toUpperCase(Locale.ROOT);
        String currency = normalizeDefault(request.currency(), "USD").toUpperCase(Locale.ROOT);
        MarketQuote quote = marketDataService.getQuote(actor, new MarketDataRequest(symbol, exchange, currency, null));
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        PortfolioSummaryResponse portfolioSummary = request.shouldIncludePortfolioContext()
                ? portfolioService.getSummary(userId, actor)
                : null;
        AiAnalysisTask task = analysisTaskRepository.save(AiAnalysisTask.create(
                user,
                symbol,
                exchange,
                currency,
                model.id(),
                model.provider(),
                request.question().trim(),
                RiskLevel.MEDIUM
        ));
        AiGatewayRequest prompt = buildPrompt(request, model, quote, profile, portfolioSummary);

        try {
            AiGatewayResult gatewayResult = generate(model, prompt);
            InvestmentAnalysisContent rawContent = parseContent(gatewayResult.content(), quote);
            InvestmentAnalysisContent content = enforceCompliance(rawContent, quote, profile);
            TokenUsageResponse tokenUsage = tokenBillingService.record(task, user, model, prompt, gatewayResult);
            AgentWorkflowResponse agentWorkflow = supervisorAgentService.orchestrate(
                    "ai-analysis-" + task.getId(),
                    request,
                    model,
                    quote,
                    portfolioSummary,
                    profile,
                    content,
                    complianceProperties.defaultDisclaimer()
            );
            task.complete(content.investmentSummary());
            analysisTaskRepository.save(task);
            auditEventService.record(
                    actor,
                    "AI_INVESTMENT_ANALYSIS_COMPLETED",
                    "Completed AI analysis " + task.getId() + " with model " + model.id() + ".",
                    RiskLevel.MEDIUM
            );
            auditEventService.record(
                    actor,
                    "AI_AGENT_WORKFLOW_COMPLETED",
                    "Agent workflow " + agentWorkflow.workflowId()
                            + " completed with status " + agentWorkflow.status()
                            + " and " + agentWorkflow.steps().size() + " steps.",
                    agentWorkflow.humanApprovalRequired() ? RiskLevel.HIGH : RiskLevel.MEDIUM
            );
            return new InvestmentAnalysisResponse(
                    task.getId(),
                    symbol,
                    exchange,
                    currency,
                    model,
                    quote,
                    content.investmentSummary(),
                    content.keyObservations(),
                    content.assumptions(),
                    content.riskWarnings(),
                    content.educationalNotes(),
                    normalizeConfidence(content.confidence()),
                    tokenUsage,
                    agentWorkflow,
                    complianceProperties.defaultDisclaimer(),
                    task.getCreatedAt() == null ? Instant.now() : task.getCreatedAt()
            );
        } catch (RuntimeException ex) {
            task.fail(ex.getMessage());
            analysisTaskRepository.save(task);
            auditEventService.record(
                    actor,
                    "AI_INVESTMENT_ANALYSIS_FAILED",
                    "AI analysis " + task.getId() + " failed: " + ex.getMessage(),
                    RiskLevel.HIGH
            );
            throw new ApiRequestException(HttpStatus.SERVICE_UNAVAILABLE, "AI analysis failed: " + ex.getMessage());
        }
    }

    private void ensureModelUsable(AiModelDescriptor model) {
        if (model.enabled()) {
            return;
        }
        HttpStatus status = model.paidTier() ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.BAD_REQUEST;
        throw new ApiRequestException(status, "Selected model is not enabled. " + model.statusNote());
    }

    private AiGatewayResult generate(AiModelDescriptor model, AiGatewayRequest prompt) {
        if (properties.mockResponsesEnabledFlag()) {
            return deterministicGateway.generate(model, prompt);
        }
        return gateways.stream()
                .filter(gateway -> !(gateway instanceof DeterministicAiModelGateway))
                .filter(gateway -> gateway.supports(model.provider()))
                .findFirst()
                .orElseThrow(() -> new ApiRequestException(HttpStatus.BAD_REQUEST, "No gateway supports model provider " + model.provider()))
                .generate(model, prompt);
    }

    private AiGatewayRequest buildPrompt(
            AiAnalysisRequest request,
            AiModelDescriptor model,
            MarketQuote quote,
            UserProfile profile,
            PortfolioSummaryResponse portfolioSummary
    ) {
        String responseLanguage = properties == null ? "zh-CN" : properties.responseLanguage();
        String activeSkills = skillRuntimeService == null
                ? "No active governed skills."
                : skillRuntimeService.activeSkillContext();
        String systemPrompt = """
                You are InvestmentAnalysisAgent inside a governed portfolio research assistant.
                You provide educational explanations, auxiliary analysis, and risk reminders only.
                Do not promise returns. Do not use phrases such as guaranteed profit, must buy, sure win, or any deterministic recommendation.
                Return only one compact JSON object, no markdown, with exactly these fields and types:
                {
                  "investmentSummary": "one short plain string, never an object or array",
                  "keyObservations": ["plain string"],
                  "assumptions": ["plain string"],
                  "riskWarnings": ["plain string"],
                  "educationalNotes": ["plain string"],
                  "confidence": 0.35
                }
                Treat symbol, exchange, and currency as separate fields. The exchange is a trading venue, not another asset.
                Do not describe an exchange as a stock, holding, or second analyzed asset unless the user explicitly asks for multiple symbols.
                confidence must be a number between 0 and 1.
                Response language: %s.
                Keep JSON field names exactly as specified in English. Write every natural-language JSON value in Simplified Chinese when response language is zh-CN.
                """.formatted(responseLanguage);
        String userPrompt = """
                Selected model: %s (%s)
                User question: %s
                Asset symbol: %s
                Exchange/venue: %s (venue only, not another analyzed asset)
                Currency: %s
                Market quote: latest=%s, previousClose=%s, changePercent=%s, source=%s, confidence=%s
                Quote assumptions: %s
                Quote risk warnings: %s
                User profile: %s
                Portfolio context: %s
                Active governed skills:
                %s
                Required disclaimer: %s
                """.formatted(
                model.displayName(),
                model.provider(),
                request.question().trim(),
                quote.symbol(),
                quote.exchange(),
                quote.currency(),
                quote.latestPrice(),
                quote.previousClose(),
                quote.changePercent(),
                quote.provider(),
                quote.confidence(),
                quote.assumptions(),
                quote.riskWarnings(),
                describeProfile(profile),
                describePortfolio(portfolioSummary),
                activeSkills,
                complianceProperties.defaultDisclaimer()
        );
        return new AiGatewayRequest(systemPrompt, userPrompt);
    }

    InvestmentAnalysisContent parseContent(String rawContent, MarketQuote quote) {
        String json = extractJson(rawContent);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return fallbackContent("The model response was not a JSON object.");
            }
            return new InvestmentAnalysisContent(
                    readInvestmentSummary(root.path("investmentSummary"), quote),
                    readStringList(root.path("keyObservations")),
                    readStringList(root.path("assumptions")),
                    readStringList(root.path("riskWarnings")),
                    readStringList(root.path("educationalNotes")),
                    readConfidence(root.path("confidence"))
            );
        } catch (JsonProcessingException ex) {
            return fallbackContent("The response parser could not validate strict JSON from the model output.");
        }
    }

    private InvestmentAnalysisContent fallbackContent(String reason) {
        return new InvestmentAnalysisContent(
                "The model did not return a valid structured analysis. Treat this result as unavailable and retry after review.",
                List.of("The model response was repaired by the service because it did not match the required schema."),
                List.of(reason),
                List.of("Model output should be reviewed before relying on it for any investment research workflow."),
                List.of("Use structured analysis as educational support only."),
                new BigDecimal("0.250000")
        );
    }

    private String readInvestmentSummary(JsonNode node, MarketQuote quote) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultSummary(quote);
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            return "Analysis for %s on %s in %s: latest price %s, previous close %s, changePercent %s. Treat this as auxiliary educational analysis, not a recommendation."
                    .formatted(
                            quote.symbol(),
                            quote.exchange(),
                            quote.currency(),
                            formatQuoteValue(quote.latestPrice()),
                            formatQuoteValue(quote.previousClose()),
                            formatQuoteValue(quote.changePercent())
                    );
        }
        List<String> values = readStringList(node);
        if (values.isEmpty()) {
            return defaultSummary(quote);
        }
        return String.join(" ", values);
    }

    private String defaultSummary(MarketQuote quote) {
        return "Analysis for %s on %s in %s could not be summarized by the model. Treat this as auxiliary educational analysis only."
                .formatted(quote.symbol(), quote.exchange(), quote.currency());
    }

    private String formatQuoteValue(BigDecimal value) {
        if (value == null) {
            return "unknown";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String text = compactNode(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
            return values;
        }
        String text = compactNode(node);
        if (!text.isBlank()) {
            values.add(text);
        }
        return values;
    }

    private String compactNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                String text = compactNode(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
            return String.join(", ", values);
        }
        List<String> pairs = new ArrayList<>();
        var fields = node.fields();
        int count = 0;
        while (fields.hasNext() && count < 6) {
            var field = fields.next();
            String value = compactNode(field.getValue());
            if (!value.isBlank()) {
                pairs.add(field.getKey() + "=" + value);
            }
            count++;
        }
        return String.join(", ", pairs);
    }

    private BigDecimal readConfidence(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new BigDecimal("0.300000");
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            if (node.isTextual()) {
                return new BigDecimal(node.asText().trim());
            }
        } catch (NumberFormatException ex) {
            return new BigDecimal("0.300000");
        }
        return new BigDecimal("0.300000");
    }

    private String extractJson(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "{}";
        }
        int start = rawContent.indexOf('{');
        int end = rawContent.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawContent.substring(start, end + 1);
        }
        return rawContent;
    }

    private InvestmentAnalysisContent enforceCompliance(
            InvestmentAnalysisContent content,
            MarketQuote quote,
            UserProfile profile
    ) {
        List<String> riskWarnings = deduplicate(content.riskWarnings());
        riskWarnings.addAll(quote.riskWarnings());
        riskWarnings.addAll(complianceProperties.requiredWarnings());
        if (profile == null || profile.getRiskPreference().name().equals("UNKNOWN")) {
            riskWarnings.add("Personalized suggestions require completed risk preference, investment horizon, and capital purpose.");
        }
        return new InvestmentAnalysisContent(
                sanitizeSummary(content.investmentSummary()),
                deduplicate(content.keyObservations()),
                deduplicate(content.assumptions()),
                deduplicate(riskWarnings),
                deduplicate(content.educationalNotes()),
                normalizeConfidence(content.confidence())
        );
    }

    private String sanitizeSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "The model did not return a usable summary. Treat this analysis as unavailable and retry later.";
        }
        String sanitized = summary;
        for (String phrase : PROHIBITED_PHRASES) {
            sanitized = sanitized.replaceAll("(?i)" + java.util.regex.Pattern.quote(phrase), "not guaranteed");
        }
        return sanitized;
    }

    private List<String> deduplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList()));
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return new BigDecimal("0.300000");
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return confidence;
    }

    private String describeProfile(UserProfile profile) {
        if (profile == null) {
            return "No profile available.";
        }
        return "riskPreference=%s, investmentHorizon=%s, capitalPurpose=%s".formatted(
                profile.getRiskPreference(),
                profile.getInvestmentHorizon(),
                profile.getCapitalPurpose()
        );
    }

    private String describePortfolio(PortfolioSummaryResponse portfolioSummary) {
        if (portfolioSummary == null) {
            return "Portfolio context was not requested.";
        }
        return "holdingCount=%s, totalMarketValue=%s, totalUnrealizedPnl=%s, riskWarnings=%s".formatted(
                portfolioSummary.holdingCount(),
                portfolioSummary.totalMarketValue(),
                portfolioSummary.totalUnrealizedPnl(),
                portfolioSummary.riskWarnings()
        );
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ApiRequestException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return normalized;
    }

    private String normalizeDefault(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
