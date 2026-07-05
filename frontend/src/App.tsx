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

  const pipeline = useMemo(
    () => [
      {
        agent: "Identity",
        description: "User registration, password hashing, bearer token authentication, and role checks.",
        status: "Ready"
      },
      {
        agent: "Audit",
        description: "Registration, login, disclaimer access, and profile changes are recorded as audit events.",
        status: "Ready"
      },
      {
        agent: "PortfolioAgent",
        description: "Holdings and portfolio analysis begin in the next delivery phase.",
        status: "Next"
      },
      {
        agent: "ComplianceAgent",
        description: "Investment outputs remain constrained by risk disclosure and suitability context.",
        status: "Baseline"
      }
    ],
    []
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
        setError(requestError instanceof Error ? requestError.message : "Unknown API error");
        setLoadState("error");
      });

    return () => {
      cancelled = true;
    };
  }, []);

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
      setAuthMessage(authMode === "register" ? "Registered and signed in." : "Signed in.");
    } catch (requestError) {
      setAuthMessage(requestError instanceof Error ? requestError.message : "Authentication failed");
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
    setAuthMessage("Investment profile context updated.");
  }

  function applyAuth(auth: AuthResponse) {
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.accessToken);
    setToken(auth.accessToken);
  }

  function logout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken("");
    setProfile(null);
    setAuthMessage("Signed out.");
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div className="brand">
          <strong>Harness Engineering Assistant</strong>
          <span>Investment research, portfolio workflow, agent governance</span>
        </div>
        <div className="status-pill" aria-live="polite">
          <span className="status-dot" />
          {health ? `${health.phase} ${health.status}` : "Connecting"}
        </div>
      </header>

      <main className="dashboard">
        <section className="hero">
          <div className="hero-copy">
            <p className="eyebrow">Phase 1 identity foundation</p>
            <h1>Secure sign-in, profile context, and role-aware workflow baseline</h1>
            <p>
              The assistant now supports registration, login, bearer token authentication, role checks,
              and investment profile context. This creates the trust boundary for portfolio, market data,
              sandbox, and agent workflows.
            </p>
            <div className="actions">
              <a className="primary-action" href="http://localhost:8080/swagger-ui.html">
                API docs
              </a>
              <a className="secondary-action" href="http://localhost:8080/actuator/health">
                Service health
              </a>
            </div>
            {loadState === "error" && <div className="error-banner">Backend API unavailable: {error}</div>}
          </div>

          <aside className="auth-panel" aria-label="Authentication panel">
            <div className="section-heading compact">
              <h2>{profile ? "Signed in" : "Account access"}</h2>
              {profile && <button onClick={logout}>Sign out</button>}
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
                    Register
                  </button>
                  <button
                    type="button"
                    className={authMode === "login" ? "active" : ""}
                    onClick={() => setAuthMode("login")}
                  >
                    Login
                  </button>
                </div>
                {authMode === "register" && (
                  <label>
                    Display name
                    <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
                  </label>
                )}
                <label>
                  Email
                  <input value={email} onChange={(event) => setEmail(event.target.value)} />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                </label>
                <button className="primary-action" type="submit">
                  {authMode === "register" ? "Create account" : "Sign in"}
                </button>
              </form>
            )}
            {authMessage && <div className="inline-message">{authMessage}</div>}
          </aside>
        </section>

        <section className="grid">
          <div className="section-panel">
            <div className="section-heading">
              <h2>Delivery pipeline</h2>
              <span>Identity first</span>
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
            <h2>{notice?.title ?? "Investment risk disclosure"}</h2>
            {notice ? (
              <ul>
                {notice.requiredDisclosures.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            ) : (
              <div className="empty-state">Loading compliance notice.</div>
            )}
          </aside>
        </section>

        {profile && (
          <section className="section-panel profile-editor">
            <div className="section-heading">
              <h2>Investment profile context</h2>
              <span>Used to avoid unsuitable personalized suggestions</span>
            </div>
            <form className="profile-form" onSubmit={handleProfileSubmit}>
              <label>
                Risk preference
                <select
                  value={riskPreference}
                  onChange={(event) => setRiskPreference(event.target.value as RiskPreference)}
                >
                  <option value="UNKNOWN">Unknown</option>
                  <option value="CONSERVATIVE">Conservative</option>
                  <option value="BALANCED">Balanced</option>
                  <option value="AGGRESSIVE">Aggressive</option>
                </select>
              </label>
              <label>
                Investment horizon
                <select
                  value={investmentHorizon}
                  onChange={(event) => setInvestmentHorizon(event.target.value as InvestmentHorizon)}
                >
                  <option value="UNKNOWN">Unknown</option>
                  <option value="SHORT_TERM">Short term</option>
                  <option value="MID_TERM">Mid term</option>
                  <option value="LONG_TERM">Long term</option>
                </select>
              </label>
              <label>
                Capital purpose
                <select
                  value={capitalPurpose}
                  onChange={(event) => setCapitalPurpose(event.target.value as CapitalPurpose)}
                >
                  <option value="UNKNOWN">Unknown</option>
                  <option value="LIQUIDITY_RESERVE">Liquidity reserve</option>
                  <option value="EDUCATION">Education</option>
                  <option value="HOUSE_PURCHASE">House purchase</option>
                  <option value="RETIREMENT">Retirement</option>
                  <option value="GENERAL_WEALTH">General wealth</option>
                </select>
              </label>
              <button className="primary-action" type="submit">
                Save profile
              </button>
            </form>
          </section>
        )}
      </main>
    </div>
  );
}

export default App;
