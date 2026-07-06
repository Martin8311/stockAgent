import { FormEvent, useEffect, useMemo, useState } from "react";
import "./App.css";
import {
  activateSkillVersion,
  approveApproval,
  createSkill,
  createSkillVersion,
  deletePortfolioTransaction,
  getAiModels,
  getAdminSkills,
  getApprovals,
  getComplianceNotice,
  getCurrentUser,
  getMarketDataProviders,
  getMarketQuote,
  getPortfolioSummary,
  getPortfolioTransactions,
  getSandboxTasks,
  getSkills,
  getSystemHealth,
  login,
  recordPortfolioTransaction,
  register,
  rejectApproval,
  requestInvestmentAnalysis,
  submitSandboxTask,
  submitSkillVersionApproval,
  testSkillVersion,
  updateCurrentUserProfile
} from "./api";
import {
  getAiProviderLabel,
  getAssetTypeLabel,
  getCapitalPurposeLabel,
  getInvestmentHorizonLabel,
  getLocalizedComplianceNotice,
  getMarketDataSourceLabel,
  getRiskPreferenceLabel,
  getTokenUsageSourceLabel,
  getTransactionTypeLabel,
  LANGUAGE_STORAGE_KEY,
  localizeMarketDataText,
  localizePortfolioRiskWarnings,
  messages,
  normalizeLanguage,
  type Language
} from "./i18n";
import type {
  AiModelDescriptor,
  ApprovalRequest,
  AuthResponse,
  CapitalPurpose,
  ComplianceNotice,
  InvestmentAnalysisResponse,
  InvestmentHorizon,
  MarketDataProvider,
  MarketQuote,
  PortfolioSummary,
  PortfolioTransaction,
  SandboxTask,
  SandboxTaskType,
  SkillCategory,
  SkillDefinition,
  AssetType,
  TransactionType,
  RiskPreference,
  SystemHealth,
  UserProfile
} from "./types";

type LoadState = "loading" | "ready" | "error";
type AuthMode = "login" | "register";
type AppView = "business" | "admin";

const TOKEN_STORAGE_KEY = "harness_agent_token";
const assetTypes: AssetType[] = ["STOCK", "FUND", "ETF", "BOND", "CASH", "CRYPTO", "OTHER"];
const transactionTypes: TransactionType[] = ["BUY", "SELL"];
const sandboxTaskTypes: SandboxTaskType[] = ["MOCK_BACKTEST", "PORTFOLIO_STRESS_TEST"];
const skillCategories: SkillCategory[] = [
  "PORTFOLIO_ANALYSIS",
  "RISK_CONTROL",
  "STRATEGY_EXPLANATION",
  "COMPLIANCE_REVIEW",
  "SANDBOX_TEMPLATE"
];

function defaultTradeTime() {
  return new Date().toISOString().slice(0, 16);
}

function defaultSandboxScript(taskType: SandboxTaskType) {
  if (taskType === "PORTFOLIO_STRESS_TEST") {
    return "shockPercent=-0.10\nshock.AAPL=-0.20";
  }
  return "symbol=AAPL\ninitialCapital=10000\nlookbackDays=60\nstrategy=moving-average-cross";
}

function App() {
  const [language, setLanguage] = useState<Language>(() =>
    normalizeLanguage(localStorage.getItem(LANGUAGE_STORAGE_KEY))
  );
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [notice, setNotice] = useState<ComplianceNotice | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>("register");
  const [activeView, setActiveView] = useState<AppView>("business");
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) ?? "");
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [email, setEmail] = useState("demo@example.com");
  const [password, setPassword] = useState("StrongPass123");
  const [displayName, setDisplayName] = useState("Demo Investor");
  const [authMessage, setAuthMessage] = useState<string | null>(null);
  const [riskPreference, setRiskPreference] = useState<RiskPreference>("UNKNOWN");
  const [investmentHorizon, setInvestmentHorizon] = useState<InvestmentHorizon>("UNKNOWN");
  const [capitalPurpose, setCapitalPurpose] = useState<CapitalPurpose>("UNKNOWN");
  const [portfolioSummary, setPortfolioSummary] = useState<PortfolioSummary | null>(null);
  const [transactions, setTransactions] = useState<PortfolioTransaction[]>([]);
  const [portfolioMessage, setPortfolioMessage] = useState<string | null>(null);
  const [marketProviders, setMarketProviders] = useState<MarketDataProvider[]>([]);
  const [marketQuote, setMarketQuote] = useState<MarketQuote | null>(null);
  const [marketMessage, setMarketMessage] = useState<string | null>(null);
  const [quoteSymbol, setQuoteSymbol] = useState("AAPL");
  const [quoteExchange, setQuoteExchange] = useState("NASDAQ");
  const [quoteCurrency, setQuoteCurrency] = useState("USD");
  const [aiModels, setAiModels] = useState<AiModelDescriptor[]>([]);
  const [selectedModelId, setSelectedModelId] = useState("");
  const [analysisResult, setAnalysisResult] = useState<InvestmentAnalysisResponse | null>(null);
  const [analysisMessage, setAnalysisMessage] = useState<string | null>(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysisSymbol, setAnalysisSymbol] = useState("AAPL");
  const [analysisExchange, setAnalysisExchange] = useState("NASDAQ");
  const [analysisCurrency, setAnalysisCurrency] = useState("USD");
  const [analysisQuestion, setAnalysisQuestion] = useState(
    "Explain the key assumptions, risks, and portfolio considerations for this asset."
  );
  const [includePortfolioContext, setIncludePortfolioContext] = useState(true);
  const [sandboxTasks, setSandboxTasks] = useState<SandboxTask[]>([]);
  const [sandboxTaskType, setSandboxTaskType] = useState<SandboxTaskType>("MOCK_BACKTEST");
  const [sandboxScript, setSandboxScript] = useState(defaultSandboxScript("MOCK_BACKTEST"));
  const [sandboxTimeoutMs, setSandboxTimeoutMs] = useState("1200");
  const [sandboxLoading, setSandboxLoading] = useState(false);
  const [sandboxMessage, setSandboxMessage] = useState<string | null>(null);
  const [activeSkills, setActiveSkills] = useState<SkillDefinition[]>([]);
  const [adminSkills, setAdminSkills] = useState<SkillDefinition[]>([]);
  const [approvalRequests, setApprovalRequests] = useState<ApprovalRequest[]>([]);
  const [skillLoading, setSkillLoading] = useState(false);
  const [skillMessage, setSkillMessage] = useState<string | null>(null);
  const [newSkillKey, setNewSkillKey] = useState("risk-guard");
  const [newSkillName, setNewSkillName] = useState("Risk Guard");
  const [newSkillDescription, setNewSkillDescription] = useState("Adds governed portfolio risk guardrails.");
  const [newSkillCategory, setNewSkillCategory] = useState<SkillCategory>("RISK_CONTROL");
  const [newSkillContent, setNewSkillContent] = useState(
    "When analysis references concentrated holdings, remind the user about diversification, time horizon, liquidity, and suitability."
  );
  const [newSkillTestScript, setNewSkillTestScript] = useState(
    "symbol=AAPL\ninitialCapital=10000\nlookbackDays=30\nstrategy=skill-validation"
  );
  const [approvalComment, setApprovalComment] = useState("Approved for controlled demo use.");
  const [symbol, setSymbol] = useState("AAPL");
  const [assetName, setAssetName] = useState("Apple Inc.");
  const [assetType, setAssetType] = useState<AssetType>("STOCK");
  const [exchange, setExchange] = useState("NASDAQ");
  const [currency, setCurrency] = useState("USD");
  const [transactionType, setTransactionType] = useState<TransactionType>("BUY");
  const [quantity, setQuantity] = useState("10");
  const [price, setPrice] = useState("100");
  const [fee, setFee] = useState("0");
  const [tradedAt, setTradedAt] = useState(defaultTradeTime);
  const [note, setNote] = useState("");

  const t = messages[language];
  const pipeline = useMemo(() => t.pipeline.steps, [t]);
  const localizedNotice = useMemo(() => getLocalizedComplianceNotice(language, notice), [language, notice]);
  const portfolioWarnings = useMemo(
    () => localizePortfolioRiskWarnings(language, portfolioSummary?.riskWarnings ?? []),
    [language, portfolioSummary]
  );
  const marketAssumptions = useMemo(
    () => localizeMarketDataText(language, marketQuote?.assumptions ?? []),
    [language, marketQuote]
  );
  const marketWarnings = useMemo(
    () => localizeMarketDataText(language, marketQuote?.riskWarnings ?? []),
    [language, marketQuote]
  );
  const selectedModel = useMemo(
    () => aiModels.find((model) => model.id === selectedModelId) ?? null,
    [aiModels, selectedModelId]
  );
  const isAdmin = profile?.roles.includes("ADMIN") ?? false;
  const isAdminView = activeView === "admin" && isAdmin;
  const isBusinessView = activeView === "business";
  const profileCompletionRatio =
    [riskPreference, investmentHorizon, capitalPurpose].filter((value) => value !== "UNKNOWN").length / 3;
  const largestHolding = useMemo(() => {
    const holdings = portfolioSummary?.holdings ?? [];
    if (holdings.length === 0) {
      return null;
    }
    return holdings.reduce((largest, holding) =>
      holding.marketValue > largest.marketValue ? holding : largest
    );
  }, [portfolioSummary]);
  const largestHoldingRatio =
    largestHolding && portfolioSummary && portfolioSummary.totalMarketValue > 0
      ? largestHolding.marketValue / portfolioSummary.totalMarketValue
      : 0;
  const enabledAiModelCount = aiModels.filter((model) => model.enabled).length;
  const enabledMarketProviderCount = marketProviders.filter((provider) => provider.enabled).length;
  const pendingSandboxTaskCount = sandboxTasks.filter((task) => task.status === "PENDING_APPROVAL").length;
  const sandboxAttentionCount = sandboxTasks.filter(
    (task) => task.status === "FAILED" || task.status === "REJECTED" || task.status === "PENDING_APPROVAL"
  ).length;
  const draftSkillVersionCount = adminSkills.reduce(
    (count, skill) =>
      count + skill.versions.filter((version) => version.status === "DRAFT" || version.status === "TESTED").length,
    0
  );
  const pendingSkillVersionCount = adminSkills.reduce(
    (count, skill) => count + skill.versions.filter((version) => version.status === "PENDING_APPROVAL").length,
    0
  );

  useEffect(() => {
    let cancelled = false;

    Promise.all([getSystemHealth(), getComplianceNotice()])
      .then(([healthResponse, noticeResponse]) => {
        if (cancelled) {
          return;
        }
        setHealth(healthResponse.data);
        setNotice(noticeResponse.data);
        setLoadState("ready");
      })
      .catch((requestError: unknown) => {
        if (cancelled) {
          return;
        }
        setError(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
        setLoadState("error");
      });

    return () => {
      cancelled = true;
    };
  }, [t.errors.unknownApi]);

  useEffect(() => {
    if (!token) {
      setProfile(null);
      return;
    }

    getCurrentUser(token)
      .then((response) => {
        setProfile(response.data);
        setActiveView(response.data.roles.includes("ADMIN") ? "admin" : "business");
        setRiskPreference(response.data.riskPreference);
        setInvestmentHorizon(response.data.investmentHorizon);
        setCapitalPurpose(response.data.capitalPurpose);
        loadPortfolio(token);
        loadMarketProviders(token);
        loadAiModels(token);
        loadSandboxTasks(token);
        loadSkills(token);
        if (response.data.roles.includes("ADMIN")) {
          loadAdminSkillWorkspace(token);
        }
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken("");
        setProfile(null);
        setPortfolioSummary(null);
        setTransactions([]);
        setMarketProviders([]);
        setMarketQuote(null);
        setAiModels([]);
        setAnalysisResult(null);
        setSandboxTasks([]);
        setActiveSkills([]);
        setAdminSkills([]);
        setApprovalRequests([]);
      });
  }, [token]);

  useEffect(() => {
    if (profile && !isAdmin && activeView === "admin") {
      setActiveView("business");
    }
  }, [activeView, isAdmin, profile]);

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthMessage(null);
    setError(null);

    try {
      const response =
        authMode === "register"
          ? await register({ email, password, displayName })
          : await login({ email, password });
      applyAuth(response.data);
      setAuthMessage(authMode === "register" ? t.auth.registeredAndSignedIn : t.auth.signedInMessage);
    } catch (requestError) {
      setAuthMessage(requestError instanceof Error ? requestError.message : t.auth.failed);
    }
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    const response = await updateCurrentUserProfile(token, {
      riskPreference,
      investmentHorizon,
      capitalPurpose
    });
    setProfile(response.data);
    setAuthMessage(t.profile.updated);
  }

  async function handleTransactionSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setPortfolioMessage(null);
    try {
      await recordPortfolioTransaction(token, {
        symbol,
        name: assetName,
        assetType,
        exchange,
        currency,
        transactionType,
        quantity: Number(quantity),
        price: Number(price),
        fee: Number(fee || "0"),
        tradedAt: new Date(tradedAt).toISOString(),
        note
      });
      setPortfolioMessage(t.portfolio.recorded);
      await loadPortfolio(token);
    } catch (requestError) {
      setPortfolioMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    }
  }

  async function handleDeleteTransaction(transactionId: number) {
    if (!token) {
      return;
    }
    setPortfolioMessage(null);
    try {
      await deletePortfolioTransaction(token, transactionId);
      setPortfolioMessage(t.portfolio.deleted);
      await loadPortfolio(token);
    } catch (requestError) {
      setPortfolioMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    }
  }

  async function loadPortfolio(activeToken: string) {
    const [summaryResponse, transactionResponse] = await Promise.all([
      getPortfolioSummary(activeToken),
      getPortfolioTransactions(activeToken)
    ]);
    setPortfolioSummary(summaryResponse.data);
    setTransactions(transactionResponse.data);
  }

  async function loadMarketProviders(activeToken: string) {
    const response = await getMarketDataProviders(activeToken);
    setMarketProviders(response.data);
  }

  async function loadAiModels(activeToken: string) {
    const response = await getAiModels(activeToken);
    setAiModels(response.data);
    setSelectedModelId((currentModelId) => {
      if (currentModelId && response.data.some((model) => model.id === currentModelId)) {
        return currentModelId;
      }
      return response.data.find((model) => model.enabled)?.id ?? response.data[0]?.id ?? "";
    });
  }

  async function loadSandboxTasks(activeToken: string) {
    const response = await getSandboxTasks(activeToken);
    setSandboxTasks(response.data);
  }

  async function loadSkills(activeToken: string) {
    const response = await getSkills(activeToken);
    setActiveSkills(response.data);
  }

  async function loadAdminSkillWorkspace(activeToken: string) {
    const [skillsResponse, approvalsResponse] = await Promise.all([
      getAdminSkills(activeToken),
      getApprovals(activeToken, "PENDING")
    ]);
    setAdminSkills(skillsResponse.data);
    setApprovalRequests(approvalsResponse.data);
  }

  async function handleQuoteSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setMarketMessage(null);
    try {
      const response = await getMarketQuote(token, quoteSymbol, quoteExchange, quoteCurrency);
      setMarketQuote(response.data);
      setMarketMessage(t.marketData.fetched);
    } catch (requestError) {
      setMarketMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    }
  }

  async function handleAnalysisSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setAnalysisMessage(null);
    setAnalysisLoading(true);
    try {
      const response = await requestInvestmentAnalysis(token, {
        modelId: selectedModelId,
        symbol: analysisSymbol,
        exchange: analysisExchange,
        currency: analysisCurrency,
        question: analysisQuestion,
        includePortfolioContext
      });
      setAnalysisResult(response.data);
      setAnalysisMessage(t.ai.completed);
    } catch (requestError) {
      setAnalysisMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setAnalysisLoading(false);
    }
  }

  async function handleSandboxSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setSandboxMessage(null);
    setSandboxLoading(true);
    try {
      const response = await submitSandboxTask(token, {
        taskType: sandboxTaskType,
        script: sandboxScript,
        timeoutMs: Number(sandboxTimeoutMs || "1200")
      });
      setSandboxTasks((currentTasks) => [response.data, ...currentTasks.filter((task) => task.id !== response.data.id)]);
      setSandboxMessage(t.sandbox.completed);
    } catch (requestError) {
      setSandboxMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSandboxLoading(false);
    }
  }

  function handleSandboxTaskTypeChange(nextTaskType: SandboxTaskType) {
    setSandboxTaskType(nextTaskType);
    setSandboxScript(defaultSandboxScript(nextTaskType));
  }

  async function refreshSkillWorkspace(activeToken: string) {
    await loadSkills(activeToken);
    if (isAdmin) {
      await loadAdminSkillWorkspace(activeToken);
    }
  }

  async function handleCreateSkill(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      await createSkill(token, {
        skillKey: newSkillKey,
        name: newSkillName,
        description: newSkillDescription,
        category: newSkillCategory,
        content: newSkillContent,
        testScript: newSkillTestScript
      });
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  async function handleCreateSkillVersion(skillId: number) {
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      await createSkillVersion(token, skillId, {
        content: newSkillContent,
        testScript: newSkillTestScript
      });
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  async function handleTestSkillVersion(versionId: number) {
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      await testSkillVersion(token, versionId);
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  async function handleSubmitSkillApproval(versionId: number) {
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      await submitSkillVersionApproval(token, versionId, {
        reason: "Sandbox test passed; request human approval before activation."
      });
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  async function handleActivateSkillVersion(versionId: number) {
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      await activateSkillVersion(token, versionId);
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  async function handleApprovalDecision(approvalId: number, decision: "approve" | "reject") {
    if (!token) {
      return;
    }
    setSkillMessage(null);
    setSkillLoading(true);
    try {
      if (decision === "approve") {
        await approveApproval(token, approvalId, approvalComment);
      } else {
        await rejectApproval(token, approvalId, approvalComment || "Rejected by reviewer.");
      }
      setSkillMessage(t.skill.saved);
      await refreshSkillWorkspace(token);
    } catch (requestError) {
      setSkillMessage(requestError instanceof Error ? requestError.message : t.errors.unknownApi);
    } finally {
      setSkillLoading(false);
    }
  }

  function applyAuth(auth: AuthResponse) {
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.accessToken);
    setToken(auth.accessToken);
    setActiveView(auth.user.roles.includes("ADMIN") ? "admin" : "business");
  }

  function logout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken("");
    setProfile(null);
    setPortfolioSummary(null);
    setTransactions([]);
    setMarketProviders([]);
    setMarketQuote(null);
    setAiModels([]);
    setAnalysisResult(null);
    setSandboxTasks([]);
    setActiveSkills([]);
    setAdminSkills([]);
    setApprovalRequests([]);
    setActiveView("business");
    setAuthMessage(t.auth.signedOut);
  }

  function updateLanguage(nextLanguage: Language) {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, nextLanguage);
    setLanguage(nextLanguage);
  }

  function formatMoney(value: number | undefined, activeCurrency = "USD") {
    return new Intl.NumberFormat(language === "zh" ? "zh-CN" : "en-US", {
      style: "currency",
      currency: activeCurrency,
      maximumFractionDigits: 2
    }).format(value ?? 0);
  }

  function formatNumber(value: number | undefined) {
    return new Intl.NumberFormat(language === "zh" ? "zh-CN" : "en-US", {
      maximumFractionDigits: 6
    }).format(value ?? 0);
  }

  function formatPercent(value: number | undefined) {
    return new Intl.NumberFormat(language === "zh" ? "zh-CN" : "en-US", {
      style: "percent",
      maximumFractionDigits: 2
    }).format(value ?? 0);
  }

  if (!profile) {
    return (
      <div className="auth-page">
        <header className="auth-top-bar">
          <div className="brand">
            <strong>{t.brand.title}</strong>
            <span>{t.brand.subtitle}</span>
          </div>
          <div className="top-actions">
            <div className="language-toggle" aria-label={t.language.label}>
              <button
                type="button"
                className={language === "en" ? "active" : ""}
                onClick={() => updateLanguage("en")}
              >
                {t.language.english}
              </button>
              <button
                type="button"
                className={language === "zh" ? "active" : ""}
                onClick={() => updateLanguage("zh")}
              >
                {t.language.chinese}
              </button>
            </div>
            <div className="status-pill" aria-live="polite">
              <span className="status-dot" />
              {health ? `${health.phase} ${health.status}` : t.status.connecting}
            </div>
          </div>
        </header>

        <main className="auth-layout">
          <section className="auth-intro">
            <p className="eyebrow">{t.hero.eyebrow}</p>
            <h1>{t.workspace.authTitle}</h1>
            <p>{t.workspace.authSubtitle}</p>
            <div className="auth-feature-grid">
              <div>
                <strong>{t.workspace.businessTitle}</strong>
                <span>{t.workspace.businessDefault}</span>
              </div>
              <div>
                <strong>{t.workspace.adminTitle}</strong>
                <span>{t.workspace.adminDefault}</span>
              </div>
            </div>
            <div className="actions">
              <a className="primary-action" href="http://localhost:8080/swagger-ui.html">
                {t.hero.apiDocs}
              </a>
              <a className="secondary-action" href="http://localhost:8080/actuator/health">
                {t.hero.serviceHealth}
              </a>
            </div>
            {loadState === "error" && (
              <div className="error-banner">
                {t.hero.backendUnavailable}: {error}
              </div>
            )}
          </section>

          <aside className="auth-card" aria-label={t.auth.panelLabel}>
            <div className="section-heading compact">
              <div>
                <h2>{t.auth.accountAccess}</h2>
                <span>{t.workspace.authAccessHint}</span>
              </div>
            </div>

            {token ? (
              <div className="empty-state">{t.status.connecting}</div>
            ) : (
              <form className="auth-form" onSubmit={handleAuthSubmit}>
                <div className="mode-toggle">
                  <button
                    type="button"
                    className={authMode === "register" ? "active" : ""}
                    onClick={() => setAuthMode("register")}
                  >
                    {t.auth.register}
                  </button>
                  <button
                    type="button"
                    className={authMode === "login" ? "active" : ""}
                    onClick={() => setAuthMode("login")}
                  >
                    {t.auth.login}
                  </button>
                </div>
                {authMode === "register" && (
                  <label>
                    {t.auth.displayName}
                    <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
                  </label>
                )}
                <label>
                  {t.auth.email}
                  <input value={email} onChange={(event) => setEmail(event.target.value)} />
                </label>
                <label>
                  {t.auth.password}
                  <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
                </label>
                <button className="primary-action" type="submit">
                  {authMode === "register" ? t.auth.createAccount : t.auth.signIn}
                </button>
              </form>
            )}

            {authMessage && <div className="inline-message">{authMessage}</div>}
            <div className="risk-note-list">
              <strong>{localizedNotice?.title ?? t.compliance.fallbackTitle}</strong>
              <ul>
                {(localizedNotice?.requiredDisclosures ?? [t.compliance.loading]).map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </div>
          </aside>
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div className="brand">
          <strong>{t.brand.title}</strong>
          <span>{t.brand.subtitle}</span>
        </div>
        <div className="top-actions">
          <div className="workspace-switch" aria-label={t.workspace.switchLabel}>
            <button
              type="button"
              className={activeView === "business" ? "active" : ""}
              onClick={() => setActiveView("business")}
            >
              {t.workspace.businessTab}
            </button>
            {isAdmin && (
              <button
                type="button"
                className={activeView === "admin" ? "active" : ""}
                onClick={() => setActiveView("admin")}
              >
                {t.workspace.adminTab}
              </button>
            )}
          </div>
          <div className="language-toggle" aria-label={t.language.label}>
            <button
              type="button"
              className={language === "en" ? "active" : ""}
              onClick={() => updateLanguage("en")}
            >
              {t.language.english}
            </button>
            <button
              type="button"
              className={language === "zh" ? "active" : ""}
              onClick={() => updateLanguage("zh")}
            >
              {t.language.chinese}
            </button>
          </div>
          <div className="status-pill" aria-live="polite">
            <span className="status-dot" />
            {health ? `${health.phase} ${health.status}` : t.status.connecting}
          </div>
          <div className="user-chip">
            <span>{t.workspace.signedInAs}</span>
            <strong>{profile.displayName}</strong>
          </div>
          <button className="secondary-action compact-action" type="button" onClick={logout}>
            {t.auth.signOut}
          </button>
        </div>
      </header>

      <main className="dashboard">
        <section className="workspace-hero">
          <div>
            <p className="eyebrow">{t.hero.eyebrow}</p>
            <h1>{isAdminView ? t.workspace.adminTitle : t.workspace.businessTitle}</h1>
            <p>{isAdminView ? t.workspace.adminSubtitle : t.workspace.businessSubtitle}</p>
            <div className="actions">
              <a className="primary-action" href="http://localhost:8080/swagger-ui.html">
                {t.hero.apiDocs}
              </a>
              <a className="secondary-action" href="http://localhost:8080/actuator/health">
                {t.hero.serviceHealth}
              </a>
            </div>
            {loadState === "error" && (
              <div className="error-banner">
                {t.hero.backendUnavailable}: {error}
              </div>
            )}
          </div>

          <aside className="workspace-summary">
            <div>
              <span>{t.workspace.roles}</span>
              <div className="role-row">
                {profile.roles.map((role) => (
                  <span className="step-tag" key={role}>
                    {role}
                  </span>
                ))}
              </div>
            </div>
            <div>
              <span>{t.workspace.currentWorkspace}</span>
              <strong>{isAdminView ? t.workspace.adminTab : t.workspace.businessTab}</strong>
            </div>
            <small>{isAdminView ? t.workspace.adminDefault : t.workspace.businessDefault}</small>
          </aside>
        </section>

        {isBusinessView && (
          <section className="section-panel overview-panel business-overview">
            <div className="section-heading">
              <div>
                <h2>{t.overview.businessTitle}</h2>
                <span>{t.overview.businessSubtitle}</span>
              </div>
            </div>

            <div className="overview-metrics">
              <article className="overview-card">
                <span>{t.portfolio.totalMarketValue}</span>
                <strong>{formatMoney(portfolioSummary?.totalMarketValue)}</strong>
                <small>{t.overview.marketValueHint}</small>
              </article>
              <article className="overview-card">
                <span>{t.portfolio.unrealizedPnl}</span>
                <strong
                  className={
                    (portfolioSummary?.totalUnrealizedPnl ?? 0) >= 0 ? "metric-positive" : "metric-negative"
                  }
                >
                  {formatMoney(portfolioSummary?.totalUnrealizedPnl)}
                </strong>
                <small>{formatPercent(portfolioSummary?.totalUnrealizedPnlRatio)}</small>
              </article>
              <article className="overview-card">
                <span>{t.portfolio.holdingCount}</span>
                <strong>{formatNumber(portfolioSummary?.holdingCount)}</strong>
                <small>
                  {formatNumber(transactions.length)} {t.overview.transactionsTracked}
                </small>
              </article>
              <article className="overview-card">
                <span>{t.overview.profileCompletion}</span>
                <strong>{formatPercent(profileCompletionRatio)}</strong>
                <small>{profileCompletionRatio >= 1 ? t.overview.profileReady : t.overview.profileNeedsContext}</small>
              </article>
            </div>

            <div className="overview-insights">
              <article className="overview-insight">
                <span>{t.overview.largestHolding}</span>
                {largestHolding ? (
                  <>
                    <strong>{largestHolding.asset.symbol}</strong>
                    <p>
                      {largestHolding.asset.name} · {formatPercent(largestHoldingRatio)} {t.overview.ofPortfolio}
                    </p>
                  </>
                ) : (
                  <p>{t.portfolio.noHoldings}</p>
                )}
              </article>
              <article className="overview-insight">
                <span>{t.overview.riskWatch}</span>
                <p>{portfolioWarnings[0] ?? t.portfolio.disclaimer}</p>
              </article>
              <article className="overview-insight">
                <span>{t.overview.aiReadiness}</span>
                <strong>{selectedModel?.displayName ?? t.ai.modelCatalog}</strong>
                <p>
                  {selectedModel
                    ? selectedModel.enabled
                      ? t.overview.aiReady
                      : selectedModel.statusNote
                    : t.overview.aiModelMissing}
                </p>
              </article>
              <article className="overview-insight">
                <span>{t.overview.recentAnalysis}</span>
                {analysisResult ? (
                  <>
                    <strong>
                      {analysisResult.symbol} · {formatPercent(analysisResult.confidence)}
                    </strong>
                    <p>{new Date(analysisResult.createdAt).toLocaleString()}</p>
                  </>
                ) : (
                  <p>{t.ai.noAnalysis}</p>
                )}
              </article>
            </div>
          </section>
        )}

        {isAdminView && (
          <section className="section-panel overview-panel admin-overview">
            <div className="section-heading">
              <div>
                <h2>{t.overview.adminTitle}</h2>
                <span>{t.overview.adminSubtitle}</span>
              </div>
            </div>

            <div className="overview-metrics">
              <article className="overview-card">
                <span>{t.overview.pendingApprovals}</span>
                <strong>{formatNumber(approvalRequests.length)}</strong>
                <small>{t.overview.pendingApprovalsHint}</small>
              </article>
              <article className="overview-card">
                <span>{t.overview.sandboxQueue}</span>
                <strong>{formatNumber(pendingSandboxTaskCount)}</strong>
                <small>
                  {formatNumber(sandboxTasks.length)} {t.overview.sandboxTasksTracked}
                </small>
              </article>
              <article className="overview-card">
                <span>{t.overview.modelAvailability}</span>
                <strong>
                  {formatNumber(enabledAiModelCount)} / {formatNumber(aiModels.length)}
                </strong>
                <small>{t.overview.enabledModels}</small>
              </article>
              <article className="overview-card">
                <span>{t.overview.activeSkills}</span>
                <strong>{formatNumber(activeSkills.length)}</strong>
                <small>
                  {formatNumber(adminSkills.length)} {t.overview.skillsManaged}
                </small>
              </article>
            </div>

            <div className="overview-insights">
              <article className="overview-insight">
                <span>{t.overview.platformHealth}</span>
                <strong>{health ? `${health.phase} ${health.status}` : t.status.connecting}</strong>
                <p>
                  {health?.complianceGuardEnabled ? t.overview.complianceGuardOn : t.overview.complianceGuardUnknown}
                </p>
              </article>
              <article className="overview-insight">
                <span>{t.overview.sandboxAttention}</span>
                <strong>{formatNumber(sandboxAttentionCount)}</strong>
                <p>{sandboxAttentionCount > 0 ? t.overview.sandboxNeedsReview : t.overview.sandboxStable}</p>
              </article>
              <article className="overview-insight">
                <span>{t.overview.providerReadiness}</span>
                <strong>
                  {formatNumber(enabledMarketProviderCount)} / {formatNumber(marketProviders.length)}
                </strong>
                <p>{t.overview.providerReadinessHint}</p>
              </article>
              <article className="overview-insight">
                <span>{t.overview.skillVersionFlow}</span>
                <strong>
                  {formatNumber(pendingSkillVersionCount)} / {formatNumber(draftSkillVersionCount)}
                </strong>
                <p>{t.overview.skillVersionFlowHint}</p>
              </article>
            </div>
          </section>
        )}

        {isAdminView && (
          <section className="grid">
          <div className="section-panel">
            <div className="section-heading">
              <h2>{t.pipeline.title}</h2>
              <span>{t.pipeline.subtitle}</span>
            </div>
            <div className="pipeline">
              {pipeline.map((step) => (
                <article className="pipeline-step" key={step.agent}>
                  <strong>{step.agent}</strong>
                  <p>{step.description}</p>
                  <span className="step-tag">{step.status}</span>
                </article>
              ))}
            </div>
          </div>

          <aside className="risk-panel">
            <h2>{localizedNotice?.title ?? t.compliance.fallbackTitle}</h2>
            {localizedNotice ? (
              <ul>
                {localizedNotice.requiredDisclosures.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            ) : (
              <div className="empty-state">{t.compliance.loading}</div>
            )}
          </aside>
        </section>
        )}

        {isBusinessView && (
          <section className="section-panel profile-editor">
            <div className="section-heading">
              <h2>{t.profile.title}</h2>
              <span>{t.profile.subtitle}</span>
            </div>
            <form className="profile-form" onSubmit={handleProfileSubmit}>
              <label>
                {t.profile.riskPreference}
                <select
                  value={riskPreference}
                  onChange={(event) => setRiskPreference(event.target.value as RiskPreference)}
                >
                  <option value="UNKNOWN">{getRiskPreferenceLabel(language, "UNKNOWN")}</option>
                  <option value="CONSERVATIVE">{getRiskPreferenceLabel(language, "CONSERVATIVE")}</option>
                  <option value="BALANCED">{getRiskPreferenceLabel(language, "BALANCED")}</option>
                  <option value="AGGRESSIVE">{getRiskPreferenceLabel(language, "AGGRESSIVE")}</option>
                </select>
              </label>
              <label>
                {t.profile.investmentHorizon}
                <select
                  value={investmentHorizon}
                  onChange={(event) => setInvestmentHorizon(event.target.value as InvestmentHorizon)}
                >
                  <option value="UNKNOWN">{getInvestmentHorizonLabel(language, "UNKNOWN")}</option>
                  <option value="SHORT_TERM">{getInvestmentHorizonLabel(language, "SHORT_TERM")}</option>
                  <option value="MID_TERM">{getInvestmentHorizonLabel(language, "MID_TERM")}</option>
                  <option value="LONG_TERM">{getInvestmentHorizonLabel(language, "LONG_TERM")}</option>
                </select>
              </label>
              <label>
                {t.profile.capitalPurpose}
                <select
                  value={capitalPurpose}
                  onChange={(event) => setCapitalPurpose(event.target.value as CapitalPurpose)}
                >
                  <option value="UNKNOWN">{getCapitalPurposeLabel(language, "UNKNOWN")}</option>
                  <option value="LIQUIDITY_RESERVE">
                    {getCapitalPurposeLabel(language, "LIQUIDITY_RESERVE")}
                  </option>
                  <option value="EDUCATION">{getCapitalPurposeLabel(language, "EDUCATION")}</option>
                  <option value="HOUSE_PURCHASE">{getCapitalPurposeLabel(language, "HOUSE_PURCHASE")}</option>
                  <option value="RETIREMENT">{getCapitalPurposeLabel(language, "RETIREMENT")}</option>
                  <option value="GENERAL_WEALTH">{getCapitalPurposeLabel(language, "GENERAL_WEALTH")}</option>
                </select>
              </label>
              <button className="primary-action" type="submit">
                {t.profile.save}
              </button>
            </form>
          </section>
        )}

        {isBusinessView && (
          <section className="section-panel market-data-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.marketData.title}</h2>
                <span>{t.marketData.subtitle}</span>
              </div>
            </div>

            <div className="market-data-grid">
              <form className="market-quote-form" onSubmit={handleQuoteSubmit}>
                <h3>{t.marketData.lookupTitle}</h3>
                <label>
                  {t.marketData.symbol}
                  <input value={quoteSymbol} onChange={(event) => setQuoteSymbol(event.target.value)} />
                </label>
                <label>
                  {t.marketData.exchange}
                  <input value={quoteExchange} onChange={(event) => setQuoteExchange(event.target.value)} />
                </label>
                <label>
                  {t.marketData.currency}
                  <input value={quoteCurrency} onChange={(event) => setQuoteCurrency(event.target.value)} />
                </label>
                <button className="primary-action" type="submit">
                  {t.marketData.lookup}
                </button>
                {marketMessage && <div className="inline-message">{marketMessage}</div>}
              </form>

              <div className="market-quote-card">
                {marketQuote ? (
                  <>
                    <div className="quote-heading">
                      <div>
                        <strong>{marketQuote.symbol}</strong>
                        <span>
                          {marketQuote.exchange} · {marketQuote.currency}
                        </span>
                      </div>
                      <span className="step-tag">
                        {getMarketDataSourceLabel(language, marketQuote.sourceType)}
                      </span>
                    </div>
                    <div className="summary-metrics quote-metrics">
                      <div>
                        <span>{t.marketData.latestPrice}</span>
                        <strong>{formatMoney(marketQuote.latestPrice, marketQuote.currency)}</strong>
                      </div>
                      <div>
                        <span>{t.marketData.previousClose}</span>
                        <strong>{formatMoney(marketQuote.previousClose, marketQuote.currency)}</strong>
                      </div>
                      <div>
                        <span>{t.marketData.change}</span>
                        <strong>{formatMoney(marketQuote.changeAmount, marketQuote.currency)}</strong>
                        <small>{formatPercent(marketQuote.changePercent)}</small>
                      </div>
                      <div>
                        <span>{t.marketData.confidence}</span>
                        <strong>{formatPercent(marketQuote.confidence)}</strong>
                      </div>
                    </div>
                    <div className="quote-details">
                      <span>
                        {t.marketData.source}: {marketQuote.provider}
                      </span>
                      <span>
                        {t.marketData.asOf}: {new Date(marketQuote.asOf).toLocaleString()}
                      </span>
                    </div>
                    <div className="risk-note-list">
                      <strong>{t.marketData.assumptions}</strong>
                      <ul>
                        {marketAssumptions.map((line) => (
                          <li key={line}>{line}</li>
                        ))}
                      </ul>
                    </div>
                    <div className="risk-note-list">
                      <strong>{t.marketData.riskWarnings}</strong>
                      <ul>
                        {marketWarnings.map((line) => (
                          <li key={line}>{line}</li>
                        ))}
                      </ul>
                    </div>
                  </>
                ) : (
                  <div className="empty-state">{t.marketData.noQuote}</div>
                )}
              </div>

              <div className="provider-list">
                <h3>{t.marketData.providerTitle}</h3>
                {marketProviders.map((provider) => (
                  <article className="provider-row" key={provider.name}>
                    <div>
                      <strong>{provider.name}</strong>
                      <span>{provider.description}</span>
                    </div>
                    <div className="provider-tags">
                      <span className="step-tag">{getMarketDataSourceLabel(language, provider.sourceType)}</span>
                      <span className="step-tag">
                        {provider.enabled ? t.marketData.enabled : t.marketData.disabled}
                      </span>
                      {provider.requiresApproval && <span className="step-tag">{t.marketData.approvalRequired}</span>}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </section>
        )}

        {isBusinessView && (
          <section className="section-panel ai-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.ai.title}</h2>
                <span>{t.ai.subtitle}</span>
              </div>
            </div>

            <div className="ai-grid">
              <form className="ai-analysis-form" onSubmit={handleAnalysisSubmit}>
                <h3>{t.ai.formTitle}</h3>
                <label>
                  {t.ai.model}
                  <select value={selectedModelId} onChange={(event) => setSelectedModelId(event.target.value)}>
                    {aiModels.map((model) => (
                      <option key={model.id} value={model.id}>
                        {model.displayName} ({getAiProviderLabel(language, model.provider)} /{" "}
                        {model.enabled ? t.ai.enabled : t.ai.disabled})
                      </option>
                    ))}
                  </select>
                </label>
                {selectedModel && (
                  <div className="inline-message wide-field">
                    {selectedModel.displayName}: {selectedModel.statusNote}
                  </div>
                )}
                <label>
                  {t.ai.symbol}
                  <input value={analysisSymbol} onChange={(event) => setAnalysisSymbol(event.target.value)} />
                </label>
                <label>
                  {t.ai.exchange}
                  <input value={analysisExchange} onChange={(event) => setAnalysisExchange(event.target.value)} />
                </label>
                <label>
                  {t.ai.currency}
                  <input value={analysisCurrency} onChange={(event) => setAnalysisCurrency(event.target.value)} />
                </label>
                <label className="wide-field">
                  {t.ai.question}
                  <textarea value={analysisQuestion} onChange={(event) => setAnalysisQuestion(event.target.value)} />
                </label>
                <label className="checkbox-field wide-field">
                  <input
                    type="checkbox"
                    checked={includePortfolioContext}
                    onChange={(event) => setIncludePortfolioContext(event.target.checked)}
                  />
                  <span>{t.ai.includePortfolio}</span>
                </label>
                <button className="primary-action" type="submit" disabled={analysisLoading || !selectedModel?.enabled}>
                  {analysisLoading ? t.ai.running : t.ai.submit}
                </button>
                {analysisMessage && <div className="inline-message">{analysisMessage}</div>}
              </form>

              <div className="ai-analysis-card">
                {analysisResult ? (
                  <>
                    <div className="quote-heading">
                      <div>
                        <strong>{analysisResult.symbol}</strong>
                        <span>
                          {analysisResult.exchange} · {analysisResult.currency}
                        </span>
                      </div>
                      <span className="step-tag">{getAiProviderLabel(language, analysisResult.model.provider)}</span>
                    </div>
                    <div className="summary-metrics ai-metrics">
                      <div>
                        <span>{t.ai.confidence}</span>
                        <strong>{formatPercent(analysisResult.confidence)}</strong>
                      </div>
                      <div>
                        <span>{t.ai.totalTokens}</span>
                        <strong>{formatNumber(analysisResult.tokenUsage.totalTokens)}</strong>
                      </div>
                      <div>
                        <span>{t.ai.usageSource}</span>
                        <strong>{getTokenUsageSourceLabel(language, analysisResult.tokenUsage.usageSource)}</strong>
                      </div>
                      <div>
                        <span>{t.ai.estimatedCost}</span>
                        <strong>
                          {formatMoney(
                            analysisResult.tokenUsage.estimatedCost,
                            analysisResult.tokenUsage.currency
                          )}
                        </strong>
                      </div>
                    </div>
                    <div className="analysis-summary">
                      <strong>{t.ai.summary}</strong>
                      <p>{analysisResult.investmentSummary}</p>
                    </div>
                    <div className="token-ledger">
                      <span>
                        {t.ai.promptTokens}: {formatNumber(analysisResult.tokenUsage.promptTokens)}
                      </span>
                      <span>
                        {t.ai.completionTokens}: {formatNumber(analysisResult.tokenUsage.completionTokens)}
                      </span>
                      <span>{analysisResult.model.billingMode}</span>
                    </div>
                    <div className="agent-workflow">
                      <div className="quote-heading">
                        <div>
                          <strong>{t.ai.agentWorkflow}</strong>
                          <span>
                            {t.ai.workflowStatus}: {analysisResult.agentWorkflow.status}
                          </span>
                        </div>
                        <span className="step-tag">
                          {analysisResult.agentWorkflow.humanApprovalRequired
                            ? t.ai.humanReviewRequired
                            : t.ai.noHumanReview}
                        </span>
                      </div>
                      {analysisResult.agentWorkflow.approvalReasons.length > 0 && (
                        <div className="risk-note-list">
                          <strong>{t.ai.approvalReasons}</strong>
                          <ul>
                            {analysisResult.agentWorkflow.approvalReasons.map((reason) => (
                              <li key={reason}>{reason}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                      <div className="agent-step-list">
                        {analysisResult.agentWorkflow.steps.map((step) => (
                          <article className="agent-step-row" key={step.agentName}>
                            <div>
                              <strong>{step.agentName}</strong>
                              <span>
                                {step.role} / {step.status}
                              </span>
                            </div>
                            <p>{step.summary}</p>
                            {step.requiresHumanApproval && step.approvalReason && (
                              <small>{step.approvalReason}</small>
                            )}
                          </article>
                        ))}
                      </div>
                    </div>
                    <div className="analysis-lists">
                      <div className="risk-note-list">
                        <strong>{t.ai.observations}</strong>
                        <ul>
                          {analysisResult.keyObservations.map((line) => (
                            <li key={line}>{line}</li>
                          ))}
                        </ul>
                      </div>
                      <div className="risk-note-list">
                        <strong>{t.ai.assumptions}</strong>
                        <ul>
                          {analysisResult.assumptions.map((line) => (
                            <li key={line}>{line}</li>
                          ))}
                        </ul>
                      </div>
                      <div className="risk-note-list">
                        <strong>{t.ai.riskWarnings}</strong>
                        <ul>
                          {analysisResult.riskWarnings.map((line) => (
                            <li key={line}>{line}</li>
                          ))}
                        </ul>
                      </div>
                      <div className="risk-note-list">
                        <strong>{t.ai.educationalNotes}</strong>
                        <ul>
                          {analysisResult.educationalNotes.map((line) => (
                            <li key={line}>{line}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                    <div className="inline-message">
                      {t.ai.disclaimer}: {analysisResult.disclaimer}
                    </div>
                  </>
                ) : (
                  <div className="empty-state">{t.ai.noAnalysis}</div>
                )}
              </div>
            </div>
          </section>
        )}

        {isAdminView && (
          <section className="section-panel model-admin-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.ai.modelCatalog}</h2>
                <span>{t.workspace.modelCatalogSubtitle}</span>
              </div>
            </div>
            <div className="provider-list">
              {aiModels.map((model) => (
                <article className="provider-row" key={model.id}>
                  <div>
                    <strong>{model.displayName}</strong>
                    <span>
                      {getAiProviderLabel(language, model.provider)} / {model.modelName} / {model.billingMode}
                    </span>
                    <span>{model.statusNote}</span>
                  </div>
                  <div className="provider-tags">
                    <span className="step-tag">{model.enabled ? t.ai.enabled : t.ai.disabled}</span>
                    {model.local && <span className="step-tag">{t.ai.localFree}</span>}
                    {model.paidTier && <span className="step-tag">{t.ai.paidReserved}</span>}
                    {model.testModeFree && <span className="step-tag">{t.ai.testFree}</span>}
                    {model.requiresApiKey && <span className="step-tag">{t.ai.apiKeyRequired}</span>}
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}

        {isAdminView && (
          <section className="section-panel sandbox-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.sandbox.title}</h2>
                <span>{t.sandbox.subtitle}</span>
              </div>
            </div>

            <div className="sandbox-grid">
              <form className="sandbox-form" onSubmit={handleSandboxSubmit}>
                <h3>{t.sandbox.formTitle}</h3>
                <label>
                  {t.sandbox.taskType}
                  <select
                    value={sandboxTaskType}
                    onChange={(event) => handleSandboxTaskTypeChange(event.target.value as SandboxTaskType)}
                  >
                    {sandboxTaskTypes.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  {t.sandbox.timeoutMs}
                  <input value={sandboxTimeoutMs} onChange={(event) => setSandboxTimeoutMs(event.target.value)} />
                </label>
                <label className="wide-field">
                  {t.sandbox.script}
                  <textarea value={sandboxScript} onChange={(event) => setSandboxScript(event.target.value)} />
                </label>
                <button className="primary-action" type="submit" disabled={sandboxLoading}>
                  {sandboxLoading ? t.sandbox.running : t.sandbox.submit}
                </button>
                {sandboxMessage && <div className="inline-message">{sandboxMessage}</div>}
              </form>

              <div className="sandbox-task-list">
                <h3>{t.sandbox.taskList}</h3>
                {sandboxTasks.length > 0 ? (
                  sandboxTasks.map((task) => (
                    <article className="sandbox-task-card" key={task.id}>
                      <div className="quote-heading">
                        <div>
                          <strong>
                            #{task.id} {task.taskType}
                          </strong>
                          <span>
                            {t.sandbox.status}: {task.status} / {t.sandbox.riskLevel}: {task.riskLevel}
                          </span>
                        </div>
                        <span className="step-tag">{task.executionTimeMs ?? 0} ms</span>
                      </div>
                      {task.approvalReason && (
                        <div className="inline-message">
                          {t.sandbox.approvalReason}: {task.approvalReason}
                        </div>
                      )}
                      {task.errorMessage && (
                        <div className="error-banner">
                          {t.sandbox.error}: {task.errorMessage}
                        </div>
                      )}
                      {task.output ? (
                        <>
                          <div className="analysis-summary">
                            <strong>{t.sandbox.output}</strong>
                            <p>{task.output.summary}</p>
                          </div>
                          <pre className="sandbox-metrics">
                            {JSON.stringify(task.output.metrics, null, 2)}
                          </pre>
                          <div className="risk-note-list">
                            <strong>{t.sandbox.riskWarnings}</strong>
                            <ul>
                              {task.output.riskWarnings.map((warning) => (
                                <li key={warning}>{warning}</li>
                              ))}
                            </ul>
                          </div>
                        </>
                      ) : (
                        <div className="empty-state">{t.sandbox.noOutput}</div>
                      )}
                    </article>
                  ))
                ) : (
                  <div className="empty-state">{t.sandbox.noTasks}</div>
                )}
              </div>
            </div>
          </section>
        )}

        {isAdminView && (
          <section className="section-panel skill-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.skill.title}</h2>
                <span>{t.skill.subtitle}</span>
              </div>
            </div>

            <div className="skill-grid">
              <div className="skill-list">
                <h3>{t.skill.activeTitle}</h3>
                {activeSkills.length > 0 ? (
                  activeSkills.map((skill) => (
                    <article className="skill-card" key={skill.id}>
                      <div className="quote-heading">
                        <div>
                          <strong>{skill.name}</strong>
                          <span>
                            {skill.skillKey} / {skill.category}
                          </span>
                        </div>
                        <span className="step-tag">{t.skill.active}</span>
                      </div>
                      <p>{skill.description}</p>
                      {skill.activeVersion && (
                        <div className="analysis-summary">
                          <strong>
                            {t.skill.version} {skill.activeVersion.versionNumber}
                          </strong>
                          <p>{skill.activeVersion.content}</p>
                        </div>
                      )}
                    </article>
                  ))
                ) : (
                  <div className="empty-state">{t.skill.noActive}</div>
                )}
              </div>

              {isAdmin && (
                <form className="skill-form" onSubmit={handleCreateSkill}>
                  <h3>{t.skill.formTitle}</h3>
                  <label>
                    {t.skill.key}
                    <input value={newSkillKey} onChange={(event) => setNewSkillKey(event.target.value)} />
                  </label>
                  <label>
                    {t.skill.name}
                    <input value={newSkillName} onChange={(event) => setNewSkillName(event.target.value)} />
                  </label>
                  <label>
                    {t.skill.category}
                    <select
                      value={newSkillCategory}
                      onChange={(event) => setNewSkillCategory(event.target.value as SkillCategory)}
                    >
                      {skillCategories.map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="wide-field">
                    {t.skill.description}
                    <input
                      value={newSkillDescription}
                      onChange={(event) => setNewSkillDescription(event.target.value)}
                    />
                  </label>
                  <label className="wide-field">
                    {t.skill.content}
                    <textarea value={newSkillContent} onChange={(event) => setNewSkillContent(event.target.value)} />
                  </label>
                  <label className="wide-field">
                    {t.skill.testScript}
                    <textarea
                      value={newSkillTestScript}
                      onChange={(event) => setNewSkillTestScript(event.target.value)}
                    />
                  </label>
                  <button className="primary-action" type="submit" disabled={skillLoading}>
                    {skillLoading ? t.skill.running : t.skill.create}
                  </button>
                  {skillMessage && <div className="inline-message">{skillMessage}</div>}
                </form>
              )}

              {isAdmin && (
                <div className="skill-list">
                  <h3>{t.skill.adminTitle}</h3>
                  {adminSkills.length > 0 ? (
                    adminSkills.map((skill) => (
                      <article className="skill-card" key={skill.id}>
                        <div className="quote-heading">
                          <div>
                            <strong>{skill.name}</strong>
                            <span>
                              {skill.skillKey} / {skill.category}
                            </span>
                          </div>
                          <button
                            className="table-action"
                            type="button"
                            disabled={skillLoading}
                            onClick={() => handleCreateSkillVersion(skill.id)}
                          >
                            {t.skill.createVersion}
                          </button>
                        </div>
                        {skill.versions.map((version) => (
                          <div className="skill-version-row" key={version.id}>
                            <div>
                              <strong>
                                {t.skill.version} {version.versionNumber}
                              </strong>
                              <span>
                                {t.skill.status}: {version.status}
                              </span>
                            </div>
                            <p>{version.content}</p>
                            {version.testResult && (
                              <small>
                                {version.testResult.summary} / sandbox #{version.testResult.sandboxTaskId}
                              </small>
                            )}
                            <div className="skill-actions">
                              <button
                                className="table-action"
                                type="button"
                                disabled={
                                  skillLoading ||
                                  version.status === "PENDING_APPROVAL" ||
                                  version.status === "APPROVED" ||
                                  version.status === "ACTIVE"
                                }
                                onClick={() => handleTestSkillVersion(version.id)}
                              >
                                {t.skill.test}
                              </button>
                              <button
                                className="table-action"
                                type="button"
                                disabled={skillLoading || version.status !== "TESTED"}
                                onClick={() => handleSubmitSkillApproval(version.id)}
                              >
                                {t.skill.submitApproval}
                              </button>
                              <button
                                className="table-action"
                                type="button"
                                disabled={skillLoading || version.status !== "APPROVED"}
                                onClick={() => handleActivateSkillVersion(version.id)}
                              >
                                {t.skill.activate}
                              </button>
                            </div>
                          </div>
                        ))}
                      </article>
                    ))
                  ) : (
                    <div className="empty-state">{t.skill.noSkills}</div>
                  )}
                </div>
              )}

              {isAdmin && (
                <div className="skill-list">
                  <h3>{t.skill.approvalQueue}</h3>
                  <label>
                    {t.skill.decisionComment}
                    <input value={approvalComment} onChange={(event) => setApprovalComment(event.target.value)} />
                  </label>
                  {approvalRequests.length > 0 ? (
                    approvalRequests.map((approval) => (
                      <article className="skill-card" key={approval.id}>
                        <div className="quote-heading">
                          <div>
                            <strong>
                              #{approval.id} {approval.requestType}
                            </strong>
                            <span>
                              {approval.targetType} #{approval.targetId} / {approval.status}
                            </span>
                          </div>
                        </div>
                        <p>{approval.reason}</p>
                        <div className="skill-actions">
                          <button
                            className="table-action"
                            type="button"
                            disabled={skillLoading}
                            onClick={() => handleApprovalDecision(approval.id, "approve")}
                          >
                            {t.skill.approve}
                          </button>
                          <button
                            className="table-action"
                            type="button"
                            disabled={skillLoading}
                            onClick={() => handleApprovalDecision(approval.id, "reject")}
                          >
                            {t.skill.reject}
                          </button>
                        </div>
                      </article>
                    ))
                  ) : (
                    <div className="empty-state">{t.skill.noApprovals}</div>
                  )}
                </div>
              )}
            </div>
          </section>
        )}

        {isBusinessView && (
          <section className="section-panel portfolio-workspace">
            <div className="section-heading">
              <div>
                <h2>{t.portfolio.title}</h2>
                <span>{t.portfolio.subtitle}</span>
              </div>
            </div>

            <div className="portfolio-grid">
              <form className="portfolio-form" onSubmit={handleTransactionSubmit}>
                <h3>{t.portfolio.formTitle}</h3>
                <label>
                  {t.portfolio.symbol}
                  <input value={symbol} onChange={(event) => setSymbol(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.name}
                  <input value={assetName} onChange={(event) => setAssetName(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.assetType}
                  <select value={assetType} onChange={(event) => setAssetType(event.target.value as AssetType)}>
                    {assetTypes.map((type) => (
                      <option key={type} value={type}>
                        {getAssetTypeLabel(language, type)}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  {t.portfolio.transactionType}
                  <select
                    value={transactionType}
                    onChange={(event) => setTransactionType(event.target.value as TransactionType)}
                  >
                    {transactionTypes.map((type) => (
                      <option key={type} value={type}>
                        {getTransactionTypeLabel(language, type)}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  {t.portfolio.quantity}
                  <input value={quantity} onChange={(event) => setQuantity(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.price}
                  <input value={price} onChange={(event) => setPrice(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.fee}
                  <input value={fee} onChange={(event) => setFee(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.tradedAt}
                  <input
                    type="datetime-local"
                    value={tradedAt}
                    onChange={(event) => setTradedAt(event.target.value)}
                  />
                </label>
                <label>
                  {t.portfolio.exchange}
                  <input value={exchange} onChange={(event) => setExchange(event.target.value)} />
                </label>
                <label>
                  {t.portfolio.currency}
                  <input value={currency} onChange={(event) => setCurrency(event.target.value)} />
                </label>
                <label className="wide-field">
                  {t.portfolio.note}
                  <input value={note} onChange={(event) => setNote(event.target.value)} />
                </label>
                <button className="primary-action" type="submit">
                  {t.portfolio.submit}
                </button>
                {portfolioMessage && <div className="inline-message">{portfolioMessage}</div>}
              </form>

              <div className="portfolio-summary">
                <h3>{t.portfolio.summaryTitle}</h3>
                <div className="summary-metrics">
                  <div>
                    <span>{t.portfolio.totalMarketValue}</span>
                    <strong>{formatMoney(portfolioSummary?.totalMarketValue)}</strong>
                  </div>
                  <div>
                    <span>{t.portfolio.totalCostBasis}</span>
                    <strong>{formatMoney(portfolioSummary?.totalCostBasis)}</strong>
                  </div>
                  <div>
                    <span>{t.portfolio.unrealizedPnl}</span>
                    <strong>{formatMoney(portfolioSummary?.totalUnrealizedPnl)}</strong>
                    <small>{formatPercent(portfolioSummary?.totalUnrealizedPnlRatio)}</small>
                  </div>
                  <div>
                    <span>{t.portfolio.realizedPnl}</span>
                    <strong>{formatMoney(portfolioSummary?.totalRealizedPnl)}</strong>
                  </div>
                </div>
                <div className="risk-note-list">
                  <strong>{t.portfolio.riskWarnings}</strong>
                  <ul>
                    {(portfolioWarnings.length > 0 ? portfolioWarnings : [t.portfolio.disclaimer]).map((warning) => (
                      <li key={warning}>{warning}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>

            <div className="portfolio-lists">
              <div>
                <h3>{t.portfolio.holdings}</h3>
                {portfolioSummary && portfolioSummary.holdings.length > 0 ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>{t.portfolio.symbol}</th>
                          <th>{t.portfolio.quantity}</th>
                          <th>{t.portfolio.averageCost}</th>
                          <th>{t.portfolio.latestPrice}</th>
                          <th>{t.portfolio.unrealizedPnl}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {portfolioSummary.holdings.map((holding) => (
                          <tr key={holding.asset.id}>
                            <td>
                              <strong>{holding.asset.symbol}</strong>
                              <span>{holding.asset.name}</span>
                            </td>
                            <td>{formatNumber(holding.quantity)}</td>
                            <td>{formatMoney(holding.averageCost, holding.asset.currency)}</td>
                            <td>{formatMoney(holding.latestPrice, holding.asset.currency)}</td>
                            <td>
                              {formatMoney(holding.unrealizedPnl, holding.asset.currency)}
                              <span>{formatPercent(holding.unrealizedPnlRatio)}</span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">{t.portfolio.noHoldings}</div>
                )}
              </div>

              <div>
                <h3>{t.portfolio.transactions}</h3>
                {transactions.length > 0 ? (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>{t.portfolio.symbol}</th>
                          <th>{t.portfolio.transactionType}</th>
                          <th>{t.portfolio.quantity}</th>
                          <th>{t.portfolio.price}</th>
                          <th>{t.portfolio.delete}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {transactions.map((transaction) => (
                          <tr key={transaction.id}>
                            <td>
                              <strong>{transaction.asset.symbol}</strong>
                              <span>{new Date(transaction.tradedAt).toLocaleString()}</span>
                            </td>
                            <td>{getTransactionTypeLabel(language, transaction.transactionType)}</td>
                            <td>{formatNumber(transaction.quantity)}</td>
                            <td>{formatMoney(transaction.price, transaction.asset.currency)}</td>
                            <td>
                              <button
                                className="table-action"
                                type="button"
                                onClick={() => handleDeleteTransaction(transaction.id)}
                              >
                                {t.portfolio.delete}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">{t.portfolio.noTransactions}</div>
                )}
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

export default App;
