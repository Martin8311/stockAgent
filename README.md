# Harness Engineering Intelligent Assistant

Harness Engineering Intelligent Assistant 是一个面向个人投资研究与持仓管理的工业级智能助手项目。它以 Spring Boot、Spring AI、Multi-agent 协作、Sandbox 执行、Skill 版本化、人机审批、审计与可观测性为主线，用于展示全栈工程能力、AI 工程能力和安全合规意识。

> 合规声明：本系统只提供教育性解释、辅助分析和风险提示，不承诺收益，不替代持牌金融顾问意见。任何投资决策都需要用户自行判断并承担风险。

## 当前阶段

阶段 0 已建立最小可运行闭环：

- 后端：Java 17、Spring Boot、Spring Security、Spring Data JPA、Flyway、Actuator、OpenAPI。
- 前端：React、TypeScript、Vite 仪表盘骨架。
- 数据库：Docker Compose 本地 MySQL。
- 文档：架构、Agent 设计、路线图。
- 合规：公共免责声明 API、审计事件基线表。

## 项目结构

```text
harness-agent/
  backend/                 Spring Boot API 服务
  frontend/                React + Vite 前端
  docs/                    架构、Agent、路线图文档
  docker-compose.yml       本地 MySQL 开发环境
  .env.example             本地环境变量模板
```

## 本地运行

### 1. 启动数据库

```bash
docker compose up -d mysql
```

### 2. 启动后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

后端默认地址：

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Public health: `http://localhost:8080/api/public/system/health`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 测试

后端：

```bash
mvn -f backend/pom.xml test
```

前端：

```bash
cd frontend
npm install
npm run build
```

## 核心原则

- 所有投资分析输出必须附带风险提示。
- 个性化建议必须依赖用户风险偏好、投资期限、资金用途等上下文。
- 高风险操作需要人工审批，例如生成投资建议、外部抓取、Skill 更新、Sandbox 脚本执行、远程推送。
- 密钥与外部 API Token 只能通过环境变量或配置模板注入。
- Agent 输出使用结构化 DTO 或 JSON Schema，便于审计、回放和测试。

## 下一阶段

阶段 1 将实现用户认证、权限、资料表、数据库迁移完善、基础 API 鉴权策略和审计增强。

