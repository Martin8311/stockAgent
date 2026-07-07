# Deployment Guide

This guide covers local development, containerized demo deployment, observability endpoints, and configuration safety.

## Local Development

Recommended fast path:

```powershell
cd D:\harness-agent
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2
```

MySQL-backed path:

```powershell
cd D:\harness-agent
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -RequireDocker
```

Stop services:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

Stop every project service including Docker MySQL:

```powershell
.\stop-all.cmd
```

## Containerized Demo

Copy the environment template and set secrets:

```powershell
copy .env.example .env
```

At minimum, change:

```text
JWT_SECRET=replace-with-at-least-32-random-characters
DB_PASSWORD=change-this-password
DB_ROOT_PASSWORD=change-this-root-password
```

Run the full stack:

```powershell
docker compose --profile app up --build
```

Open:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info
- Prometheus metrics: http://localhost:8080/actuator/prometheus

The frontend container serves the React app with Nginx and proxies `/api`, `/actuator`, `/v3/api-docs`, and `/swagger-ui` to the backend container.

## AI Provider Configuration

Local Ollama from Docker uses `host.docker.internal` by default:

```text
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen2.5:3b
```

MiniMax is disabled for paid execution unless the key and access switches are explicitly configured:

```text
MINIMAX_API_KEY=your-key
AI_TEST_MODE=true
AI_PAID_ACCESS_ENABLED=true
```

Never commit real API keys. Keep them in `.env`, local environment variables, or a deployment secret manager.

## Observability

Current observability surface:

- `/actuator/health`: service readiness and liveness checks.
- `/actuator/info`: app name, phase, description, and compliance posture.
- `/actuator/prometheus`: Micrometer metrics for Prometheus scraping.
- `audit_event`: domain audit records for authentication, compliance notice access, market data, AI analysis, sandbox execution, skill governance, and approval workflows.
- `.dev/logs`: local script logs for backend, frontend, and startup orchestration.

Suggested Prometheus scrape target for local Docker:

```yaml
scrape_configs:
  - job_name: harness-agent
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

## Production Hardening Checklist

- Use a managed MySQL instance with backups and migration review.
- Inject `JWT_SECRET`, database passwords, and provider API keys from a secret manager.
- Put the backend behind TLS and an API gateway or reverse proxy.
- Restrict `/actuator/prometheus` to the monitoring network in production.
- Enable centralized logs and alerting for failed migrations, sandbox rejections, model provider errors, and approval queue growth.
- Keep all investment outputs constrained by risk disclosure and suitability context.
