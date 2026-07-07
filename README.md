# Harness Engineering Intelligent Assistant

面向个人投资研究与持仓管理的工业级智能助手项目，用于展示全栈工程、Spring AI、Agent 编排、Sandbox、安全合规、可观测性和阶段性交付能力。

> 合规声明：本系统只提供教育性解释、辅助分析和风险提醒，不承诺收益，不替代持牌金融顾问意见。任何投资决策都需要用户自行判断并承担风险。

## Latest Delivery

Phase 9 is in progress and now includes:

- React role-based UI with an independent login page, business workspace, and admin console.
- Portfolio, market data, AI analysis, multi-agent workflow trace, sandbox execution, and governed Skill approval.
- Spring Boot Actuator health/info/prometheus endpoints.
- Dockerfiles for backend/frontend and a Docker Compose `app` profile for full-stack demo deployment.
- Deployment documentation and engineering logs for interview review.

## Tech Stack

- Backend: Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, Spring AI, Actuator, OpenAPI.
- Frontend: React, TypeScript, Vite, Nginx container image for deployment.
- Database: MySQL 8.4 via Docker Compose; H2 fallback for fast local demo.
- AI providers: local Ollama Qwen2.5 3B by default, MiniMax reserved for paid/test-mode usage.
- Observability: Actuator health/info/prometheus, audit events, startup logs.

## Project Structure

```text
harness-agent/
  backend/                 Spring Boot API service
  frontend/                React + Vite frontend
  docs/                    Architecture, roadmap, deployment, agent design, engineering log
  scripts/                 One-click local start/stop scripts
  docker-compose.yml       MySQL plus optional full-stack app profile
  .env.example             Local and container environment template
```

## Local Run

Fast demo with H2:

```powershell
cd D:\harness-agent
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2
```

MySQL-backed run:

```powershell
cd D:\harness-agent
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -RequireDocker
```

Stop services:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

Stop all project services including Docker MySQL:

```powershell
.\stop-all.cmd
```

Open:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info
- Prometheus: http://localhost:8080/actuator/prometheus

## Containerized Demo

```powershell
copy .env.example .env
docker compose --profile app up --build
```

The frontend container serves the React app through Nginx and proxies `/api`, `/actuator`, `/v3/api-docs`, and `/swagger-ui` to the backend container.

See [docs/deployment.md](docs/deployment.md) for deployment details and production hardening notes.

## AI Model Configuration

Local free model:

```powershell
$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="qwen2.5:3b"
```

MiniMax test or paid channel:

```powershell
$env:MINIMAX_API_KEY="your-minimax-api-key"
$env:MINIMAX_ENABLED="true"
$env:AI_TEST_MODE="true"
$env:AI_PAID_ACCESS_ENABLED="true"
```

Never commit real API keys. Use environment variables, `.env`, or a secret manager.

## Main Capabilities

- User authentication, role-based access control, current profile, and admin user APIs.
- Portfolio transactions, holdings summary, unrealized/realized P&L, risk warnings.
- Market data provider abstraction with local mock provider and external adapter placeholder.
- Selectable AI model catalog, token usage accounting, structured investment analysis.
- Multi-agent workflow trace through Supervisor, MarketData, Portfolio, Risk, Strategy, and Compliance roles.
- Sandbox task execution with policy checks, timeout, audit, and approval status.
- Skill governance with versioning, sandbox testing, approval, activation, and rollback-friendly active pointers.
- Business workspace and admin console with role-specific overview metrics.

## Tests

Backend:

```powershell
cd D:\harness-agent\backend
mvn test
```

Frontend:

```powershell
cd D:\harness-agent\frontend
npm run build
```

## Core Safety Rules

- Investment outputs must include risk warnings and disclaimers.
- The system must not use deterministic claims such as "guaranteed rise", "must buy", or "risk-free profit".
- Personalized analysis should require risk preference, investment horizon, and capital purpose.
- External data, paid models, sandbox execution, skill updates, and high-risk outputs should remain auditable and approval-aware.
- Configuration secrets must not be hardcoded or committed.
