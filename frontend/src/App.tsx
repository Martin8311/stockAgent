import { FormEvent, useEffect, useMemo, useState } from "react";
import "./App.css";
import {
  getComplianceNotice,
  getCurrentUser,
  getSystemHealth,
  login,
  register,
  updateCurrentUserProfile
} from "./api";
import {
  getCapitalPurposeLabel,
  getInvestmentHorizonLabel,
  getLocalizedComplianceNotice,
  getRiskPreferenceLabel,
  LANGUAGE_STORAGE_KEY,
  messages,
  normalizeLanguage,
  type Language
} from "./i18n";
import type {
  AuthResponse,
  CapitalPurpose,
  ComplianceNotice,
  InvestmentHorizon,
  RiskPreference,
  SystemHealth,
  UserProfile
} from "./types";

type LoadState = "loading" | "ready" | "error";
type AuthMode = "login" | "register";

const TOKEN_STORAGE_KEY = "harness_agent_token";

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

  const t = messages[language];
  const pipeline = useMemo(() => t.pipeline.steps, [t]);
  const localizedNotice = useMemo(() => getLocalizedComplianceNotice(language, notice), [language, notice]);

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
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken("");
        setProfile(null);
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

  function applyAuth(auth: AuthResponse) {
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.accessToken);
    setToken(auth.accessToken);
  }

  function logout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken("");
    setProfile(null);
    setAuthMessage(t.auth.signedOut);
  }

  function updateLanguage(nextLanguage: Language) {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, nextLanguage);
    setLanguage(nextLanguage);
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
      </main>
    </div>
  );
}

export default App;
