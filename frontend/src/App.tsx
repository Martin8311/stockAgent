import { useEffect, useMemo, useState } from "react";
import "./App.css";
import { getComplianceNotice, getSystemHealth } from "./api";
import type { ComplianceNotice, SystemHealth } from "./types";

type LoadState = "loading" | "ready" | "error";

function App() {
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [notice, setNotice] = useState<ComplianceNotice | null>(null);
  const [error, setError] = useState<string | null>(null);

  const pipeline = useMemo(
    () => [
      {
        agent: "MarketDataAgent",
        description: "市场数据采集、清洗、来源标记和时效性检查。",
        status: "Planned"
      },
      {
        agent: "PortfolioAgent",
        description: "持仓录入、盈亏分析、组合暴露与资产集中度分析。",
        status: "Planned"
      },
      {
        agent: "RiskAgent",
        description: "波动、回撤、集中持仓、杠杆和用户画像缺口提醒。",
        status: "Planned"
      },
      {
        agent: "ComplianceAgent",
        description: "检查免责声明、禁止用语、假设、数据来源和人工审批要求。",
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
            <p className="eyebrow">Phase 0 foundation</p>
            <h1>面向投资研究与持仓管理的工业级智能助手骨架</h1>
            <p>
              当前版本聚焦项目地基：Spring Boot API、React 仪表盘、MySQL 开发环境、
              OpenAPI、审计基线和投资合规提示。后续阶段会逐步接入认证、持仓、市场数据、
              Spring AI、Multi-agent 编排、Sandbox 和 Skill 审批。
            </p>
            <div className="actions">
              <a className="primary-action" href="http://localhost:8080/swagger-ui.html">
                查看 API 文档
              </a>
              <a className="secondary-action" href="http://localhost:8080/actuator/health">
                查看服务健康
              </a>
            </div>
            {loadState === "error" && (
              <div className="error-banner">后端 API 暂不可用：{error}</div>
            )}
          </div>

          <aside className="signal-panel" aria-label="System signals">
            <div className="metric-card">
              <span>Compliance guard</span>
              <strong>{health?.complianceGuardEnabled ? "Enabled" : "Pending"}</strong>
              <small>所有投资相关输出必须保留风险提示、假设和数据来源。</small>
            </div>
            <div className="metric-card">
              <span>Audit baseline</span>
              <strong>{loadState === "ready" ? "Recording" : "Waiting"}</strong>
              <small>公共免责声明访问已经接入审计事件，为后续审批和回放打底。</small>
            </div>
          </aside>
        </section>

        <section className="grid">
          <div className="section-panel">
            <div className="section-heading">
              <h2>Agent 协作路线</h2>
              <span>Structured outputs first</span>
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
              <div className="empty-state">正在加载合规提示。</div>
            )}
          </aside>
        </section>
      </main>
    </div>
  );
}

export default App;

