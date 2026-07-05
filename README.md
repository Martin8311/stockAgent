# Harness Engineering Intelligent Assistant

Harness Engineering Intelligent Assistant 是一个面向个人投资研究与持仓管理的工业级智能助手项目。它以 Spring Boot、Spring AI、Multi-agent 协作、Sandbox 执行、Skill 版本化、人机审批、审计与可观测性为主线，用于展示全栈工程能力、AI 工程能力和安全合规意识。

> 合规声明：本系统只提供教育性解释、辅助分析和风险提示，不承诺收益，不替代持牌金融顾问意见。任何投资决策都需要用户自行判断并承担风险。

## 当前阶段

阶段 3 已建立认证、权限、持仓管理、组合风险摘要与市场数据接入最小闭环：

- 后端：Java 17、Spring Boot、Spring Security、Spring Data JPA、Flyway、Actuator、OpenAPI。
- 前端：React、TypeScript、Vite 仪表盘、注册/登录、个人资料上下文。
- 数据库：Docker Compose 本地 MySQL。
- 文档：架构、Agent 设计、路线图、工程坑点日志。
- 合规：公共免责声明 API、审计事件基线表。
- 认证：用户注册、登录、BCrypt 密码哈希、JWT Bearer Token、角色权限、管理员用户列表。
- 持仓：资产分类、交易录入/删除、平均成本、持仓市值、未实现/已实现盈亏和组合风险提示。
- 市场数据：Provider 抽象、本地 mock 行情、外部数据源适配预留、报价来源/假设/风险提示和审计记录。
- 前端：中英文切换、账户访问、投资画像、持仓工作台和交易台账。

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

### 一键启动

PowerShell 或命令提示符：

```powershell
.\start-dev.cmd
```

脚本会自动：

- 启动 Docker Compose MySQL。
- 等待 MySQL 健康检查。
- 启动 Spring Boot 后端。
- 安装缺失的前端依赖并启动 Vite 前端。
- 将日志写入 `.dev/logs/`。

停止脚本启动的后端和前端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

同时停止 MySQL：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1 -StopDocker
```

如果直接双击 `start-dev.cmd`，窗口会在成功或失败后保留提示，失败详情会写入 `.dev/logs/start-dev.log`。

Docker Desktop 不可访问时，脚本会自动降级到本地 H2 dev profile，保证演示环境可以启动；MySQL/Docker 仍然是首选开发数据库路径。常用参数：

```powershell
# 明确使用 H2，本地演示最快
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2

# 必须使用 Docker/MySQL，Docker 不可用时直接失败
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -RequireDocker

# 自己已经启动 MySQL 时跳过 Docker Compose
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -SkipDocker
```

### 手动启动

#### 1. 启动数据库

```bash
docker compose up -d mysql
```

#### 2. 启动后端

PowerShell：

```powershell
$env:JWT_SECRET="replace-with-at-least-32-random-characters"
mvn -f backend/pom.xml spring-boot:run
```

后端默认地址：

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Public health: `http://localhost:8080/api/public/system/health`
- Register: `POST http://localhost:8080/api/public/auth/register`
- Login: `POST http://localhost:8080/api/public/auth/login`
- Me: `GET http://localhost:8080/api/me`，需要 `Authorization: Bearer <token>`
- Portfolio summary: `GET http://localhost:8080/api/portfolio/summary`，需要 `Authorization: Bearer <token>`
- Portfolio transactions: `GET/POST http://localhost:8080/api/portfolio/transactions`，需要 `Authorization: Bearer <token>`
- Market data providers: `GET http://localhost:8080/api/market-data/providers`，需要 `Authorization: Bearer <token>`
- Market quote: `GET http://localhost:8080/api/market-data/quote?symbol=AAPL&exchange=NASDAQ&currency=USD`，需要 `Authorization: Bearer <token>`

#### 3. 启动前端

```powershell
cd frontend
npm install --cache .npm-cache
npm run dev
```

前端默认地址：`http://localhost:5173`

## 测试

后端：

```powershell
mvn -f backend/pom.xml test
```

前端：

```powershell
cd frontend
npm install --cache .npm-cache
npm run build
```

## 手动验证认证流程

注册第一个用户。第一个注册用户会自动拥有 `ADMIN` 和 `USER` 角色，便于本地初始化管理后台：

```powershell
curl.exe -X POST http://localhost:8080/api/public/auth/register -H "Content-Type: application/json" -d "{\"email\":\"demo@example.com\",\"password\":\"StrongPass123\",\"displayName\":\"Demo Investor\"}"
```

登录后复制返回的 `accessToken`：

```powershell
curl.exe -X POST http://localhost:8080/api/public/auth/login -H "Content-Type: application/json" -d "{\"email\":\"demo@example.com\",\"password\":\"StrongPass123\"}"
```

访问当前用户资料：

```powershell
curl.exe http://localhost:8080/api/me -H "Authorization: Bearer <accessToken>"
```

## 核心原则

- 所有投资分析输出必须附带风险提示。
- 个性化建议必须依赖用户风险偏好、投资期限、资金用途等上下文。
- 高风险操作需要人工审批，例如生成投资建议、外部抓取、Skill 更新、Sandbox 脚本执行、远程推送。
- 密钥与外部 API Token 只能通过环境变量或配置模板注入。
- Agent 输出使用结构化 DTO 或 JSON Schema，便于审计、回放和测试。

## 下一阶段

阶段 4 将接入 Spring AI，定义基础投资分析 Agent、结构化分析 DTO、风险提示模板和合规检查，为后续 Multi-agent 编排做准备。
