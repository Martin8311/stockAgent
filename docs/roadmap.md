# Roadmap

## 阶段 0：项目初始化

- 已建立 Spring Boot 后端、React 前端、Docker Compose MySQL、README 和核心设计文档。
- 已提供公共健康检查、合规免责声明 API、审计事件基线表。

## 阶段 1：用户认证与权限

- 已实现用户注册、登录、JWT Bearer Token、BCrypt 密码哈希。
- 已实现 `USER` / `ADMIN` 角色权限、当前用户资料、管理员用户列表。
- 已通过 Flyway 增加用户、角色、投资画像表，并将注册、登录、资料更新接入审计。

## 阶段 2：持仓管理

- 已实现资产分类、交易录入/删除、平均成本、成本金额、未实现/已实现盈亏计算。
- 已实现组合集中度、单一持仓和较大未实现亏损风险提示。
- 已实现前端持仓工作台、交易台账和中英文展示。

## 阶段 3：市场数据接入

- 已实现市场数据 Provider 抽象接口。
- 已实现本地 mock 数据源、结构化 quote、来源置信度、假设和风险提示。
- 已预留外部数据源适配器开关，默认禁用，后续接入前需要 API Key、限流、缓存、审计和人工审批。
- 已将组合摘要估值接入 `MarketDataService`，mock quote 不可用时回落到资产本地价格。

## 阶段 4：Spring AI 基础分析 Agent

- 已接入 Spring AI Ollama starter，默认支持本地 `qwen2.5:3b` 免费模型。
- 已预留 MiniMax HTTP/OpenAI-compatible 适配通道，通过 `MINIMAX_API_KEY`、`AI_TEST_MODE`、`AI_PAID_ACCESS_ENABLED` 控制启用。
- 已实现 `/api/ai/models` 模型目录，返回模型启用状态、免费/付费属性、计费模式和状态说明。
- 已实现 `/api/ai/analysis` 结构化投资分析，输出 summary、observations、assumptions、riskWarnings、educationalNotes、confidence、quote、tokenUsage。
- 已持久化 AI analysis task 和 token usage record，并写入审计事件。
- 已在前端提供模型选择、分析表单、报告卡片和 token 计费摘要。

## 阶段 5：Multi-agent 编排

- `SupervisorAgent` 编排 `MarketDataAgent`、`PortfolioAgent`、`RiskAgent`、`StrategyAgent`、`ComplianceAgent`。
- Agent 结果可审计、可回放、可测试。
- 对高风险输出引入人工介入和审批状态。

## 阶段 6：Sandbox 执行

- 受限脚本执行、超时控制、资源限制。
- 任务记录、审批、审计日志。

## 阶段 7：Skill 管理

- Skill 版本管理、测试、审批、启用/回滚。
- 禁止未经审批的 Skill 影响生产分析流程。

## 阶段 8：前端仪表盘

- 持仓视图、组合分析、任务记录、分析报告页。
- 管理后台和审批工作台。

## 阶段 9：测试、可观测性与部署

- 单元测试、集成测试、API 测试。
- Metrics、Tracing、结构化日志。
- Docker Compose、部署文档、CI。

## 阶段 10：简历作品打磨

- GitHub README 优化。
- 架构图、截图、演示脚本、项目亮点总结。
