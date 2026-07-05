# Harness Engineering Intelligent Assistant

Harness Engineering Intelligent Assistant 是一个面向个人投资研究与持仓管理的工业级智能助手项目。它以 Spring Boot、Spring AI、可治理 Agent、市场数据抽象、持仓分析、审计日志、安全合规和阶段性交付为主线，用于展示全栈工程能力、AI 工程能力和金融合规意识。

> 合规声明：本系统只提供教育性解释、辅助分析和风险提醒，不承诺收益，不替代持牌金融顾问意见。任何投资决策都需要用户自行判断并承担风险。

## 当前阶段

阶段 4 已建立最小可运行的 AI 分析闭环：

- 后端：Java 17、Spring Boot、Spring Security、Spring Data JPA、Flyway、Actuator、OpenAPI、Spring AI Ollama。
- 前端：React、TypeScript、Vite 仪表盘，支持中英文切换。
- 数据库：本地优先 Docker Compose MySQL；脚本支持 H2 fallback 方便演示。
- 认证：用户注册、登录、BCrypt 密码哈希、JWT Bearer Token、角色权限、管理员用户列表。
- 持仓：资产分类、交易录入/删除、平均成本、持仓市值、未实现/已实现盈亏和组合风险提示。
- 市场数据：Provider 抽象、本地 mock quote、外部数据源占位、来源置信度、假设、风险提示和审计。
- AI 分析：用户可选择模型，本地 Ollama Qwen2.5 3B 作为免费模型，MiniMax 作为付费/测试通道预留。
- 计费：记录 prompt tokens、completion tokens、total tokens、计费模式、预估成本、是否测试免费和审计事件。
- 合规：AI 输出经服务端合规兜底，补齐风险提示和免责声明，并过滤确定性收益表述。

## 项目结构

```text
harness-agent/
  backend/                 Spring Boot API 服务
  frontend/                React + Vite 前端
  docs/                    架构、Agent、路线图、工程日志文档
  docker-compose.yml       本地 MySQL 开发环境
  .env.example             本地环境变量模板
```

## 本地运行

一键启动，推荐演示模式：

```powershell
cd D:\harness-agent
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2
```

启动后访问：

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html
- Health：http://localhost:8080/actuator/health

停止服务：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

如果要使用 Docker/MySQL：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -RequireDocker
```

## AI 模型配置

本地免费模型默认走 Ollama：

```powershell
$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="qwen2.5:3b"
```

MiniMax 作为付费用户通道预留，真实 key 只能通过环境变量注入：

```powershell
$env:MINIMAX_API_KEY="your-minimax-api-key"
$env:MINIMAX_ENABLED="true"
$env:AI_PAID_ACCESS_ENABLED="true"
```

测试或演示免费模式：

```powershell
$env:AI_TEST_MODE="true"
$env:AI_MOCK_RESPONSES="true"
```

计费价格不硬编码真实供应商价格，可通过环境变量配置：

```powershell
$env:MINIMAX_PROMPT_PRICE_PER_MILLION_TOKENS="0.000000"
$env:MINIMAX_COMPLETION_PRICE_PER_MILLION_TOKENS="0.000000"
```

## 主要 API

- `POST /api/public/auth/register`
- `POST /api/public/auth/login`
- `GET /api/me`
- `GET /api/portfolio/summary`
- `GET/POST /api/portfolio/transactions`
- `GET /api/market-data/providers`
- `GET /api/market-data/quote?symbol=AAPL&exchange=NASDAQ&currency=USD`
- `GET /api/ai/models`
- `POST /api/ai/analysis`

受保护 API 需要：

```http
Authorization: Bearer <accessToken>
```

## 测试

后端：

```powershell
cd D:\harness-agent\backend
mvn test
```

前端：

```powershell
cd D:\harness-agent\frontend
npm run build
```

## 核心原则

- 所有投资分析输出必须附带风险提示和免责声明。
- 不生成“保证上涨”“必买”“稳赚”等确定性表述。
- 用户画像不足时，不给强个性化建议。
- 外部 API Key 只通过环境变量注入，不进入仓库。
- MiniMax 等付费模型必须受测试模式、付费开关和后续审批流约束。
- Agent 输出使用结构化 DTO，便于审计、回放、测试和后续 multi-agent 编排。

## 下一阶段

阶段 5 将实现 Multi-agent 编排：`SupervisorAgent` 汇总 `MarketDataAgent`、`PortfolioAgent`、`RiskAgent`、`StrategyAgent` 和 `ComplianceAgent` 的结构化结果，并引入更明确的人工介入节点。
