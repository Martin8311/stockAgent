import { FormEvent, useEffect, useMemo, useState } from "react";
import "./App.css";
import {
  deletePortfolioTransaction,
  getComplianceNotice,
  getCurrentUser,
  getMarketDataProviders,
  getMarketQuote,
  getPortfolioSummary,
  getPortfolioTransactions,
  getSystemHealth,
  login,
  recordPortfolioTransaction,
  register,
  updateCurrentUserProfile
} from "./api";
import {
  getAssetTypeLabel,
  getCapitalPurposeLabel,
  getInvestmentHorizonLabel,
  getLocalizedComplianceNotice,
  getMarketDataSourceLabel,
  getRiskPreferenceLabel,
  getTransactionTypeLabel,
  LANGUAGE_STORAGE_KEY,
  localizeMarketDataText,
  localizePortfolioRiskWarnings,
  messages,
  normalizeLanguage,
  type Language
} from "./i18n";
import type {
  AuthResponse,
  CapitalPurpose,
  ComplianceNotice,
  InvestmentHorizon,
  MarketDataProvider,
  MarketQuote,
  PortfolioSummary,
  PortfolioTransaction,
  AssetType,
  TransactionType,
  RiskPreference,
  SystemHealth,
  UserProfile
} from "./types";

type LoadState = "loading" | "ready" | "error";
type AuthMode = "login" | "register";

const TOKEN_STORAGE_KEY = "harness_agent_token";
const assetTypes: AssetType[] = ["STOCK", "FUND", "ETF", "BOND", "CASH", "CRYPTO", "OTHER"];
const transactionTypes: TransactionType[] = ["BUY", "SELL"];

function defaultTradeTime() {
  return new Date().toISOString().slice(0, 16);
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
        setRiskPreference(response.data.riskPreference);
        setInvestmentHorizon(response.data.investmentHorizon);
        setCapitalPurpose(response.data.capitalPurpose);
        loadPortfolio(token);
        loadMarketProviders(token);
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken("");
        setProfile(null);
        setPortfolioSummary(null);
        setTransactions([]);
        setMarketProviders([]);
        setMarketQuote(null);
      });
  }, [token]);

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

  function applyAuth(auth: AuthResponse) {
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.accessToken);
    setToken(auth.accessToken);
  }

  function logout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken("");
    setProfile(null);
    setPortfolioSummary(null);
    setTransactions([]);
    setMarketProviders([]);
    setMarketQuote(null);
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

  return (
    <div className="app-shell">
      <header className="top-bar">
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

      <main className="dashboard">
        <section className="hero">
          <div className="hero-copy">
            <p className="eyebrow">{t.hero.eyebrow}</p>
            <h1>{t.hero.title}</h1>
            <p>{t.hero.body}</p>
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

          <aside className="auth-panel" aria-label={t.auth.panelLabel}>
            <div className="section-heading compact">
              <h2>{profile ? t.auth.signedIn : t.auth.accountAccess}</h2>
              {profile && <button onClick={logout}>{t.auth.signOut}</button>}
            </div>

            {profile ? (
              <div className="profile-summary">
                <strong>{profile.displayName}</strong>
                <span>{profile.email}</span>
                <div className="role-row">
                  {profile.roles.map((role) => (
                    <span className="step-tag" key={role}>
                      {role}
                    </span>
                  ))}
                </div>
              </div>
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
                  <input
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                </label>
                <button className="primary-action" type="submit">
                  {authMode === "register" ? t.auth.createAccount : t.auth.signIn}
                </button>
              </form>
            )}
            {authMessage && <div className="inline-message">{authMessage}</div>}
          </aside>
        </section>

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

        {profile && (
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

        {profile && (
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

        {profile && (
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
