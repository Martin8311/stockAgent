# Phase 5 Multi-agent Orchestration

阶段 5 将原有单个 `InvestmentAnalysisAgent` 升级为由 `SupervisorAgent` 编排的结构化工作流。当前仍复用 `/api/ai/analysis` 作为入口，避免产生孤立 demo；接口会在原分析结果之外返回 `agentWorkflow`。

## 实现步骤与作用

1. 定义 Agent trace DTO
   - 作用：把每个 Agent 的职责、状态、摘要、观察点、风险提示和人工复核原因结构化，便于前端展示、测试断言和后续审计持久化。
   - 相关对象：`AgentRole`、`AgentStepStatus`、`AgentWorkflowStatus`、`AgentStepResponse`、`AgentWorkflowResponse`。

2. 实现 `SupervisorAgentService`
   - 作用：统一编排 `MarketDataAgent`、`PortfolioAgent`、`RiskAgent`、`StrategyAgent`、`ComplianceAgent`，并汇总为整体 workflow 状态。
   - 当前策略：同步编排，低复杂度接入现有分析链路；后续可替换为异步任务、队列或审批流。

3. `MarketDataAgent`
   - 作用：校验行情来源、价格、时间、置信度和市场数据风险。
   - 输出：provider、sourceType、latest、previousClose、confidence、行情风险提示。
   - 人工复核触发：外部数据源或过低置信度。

4. `PortfolioAgent`
   - 作用：读取用户是否请求持仓上下文，并汇总持仓数量、市值、未实现盈亏和组合风险。
   - 输出：holdingCount、totalMarketValue、totalUnrealizedPnl、组合风险提示。
   - 人工复核触发：请求了持仓上下文但没有可用持仓数据。

5. `RiskAgent`
   - 作用：聚合行情、持仓、用户画像和模型输出中的风险信号。
   - 输出：高风险意图识别、画像完整性、聚合风险数量。
   - 人工复核触发：用户画像缺失、问题中包含买卖/推荐/再平衡等强交易意图。

6. `StrategyAgent`
   - 作用：承接模型生成的教育性解释和可选方案，不直接做确定性投资推荐。
   - 输出：模型供应商、关键观察点、策略解释摘要。
   - 人工复核触发：付费模型叠加强交易意图。

7. `ComplianceAgent`
   - 作用：检查免责声明、假设、风险提示和禁止性确定表述。
   - 输出：合规字段是否存在、是否命中禁止词。
   - 人工复核触发：投资相关高风险意图、缺少风险/假设/免责声明、命中禁止词。

8. 接入现有 AI 分析响应
   - 作用：`/api/ai/analysis` 在原有 summary、riskWarnings、tokenUsage 外新增 `agentWorkflow`，让阶段 5 成为真实用户路径的一部分。
   - 审计：完成后写入 `AI_AGENT_WORKFLOW_COMPLETED` 审计事件，风险级别随是否需要人工复核升高。

9. 前端展示 Agent workflow
   - 作用：在 AI 报告页展示 workflow 总状态、人工复核原因和每个 Agent step，让用户能看到系统为什么谨慎、哪里需要人工介入。

10. 测试与验证
    - 作用：后端集成测试断言 workflow 存在、包含 5 个 Agent step、能触发 `HUMAN_REVIEW_REQUIRED`。
    - 前端执行 TypeScript/Vite 构建，确保新增类型和展示不破坏页面。

## 当前响应结构示例

```json
{
  "agentWorkflow": {
    "workflowId": "ai-analysis-1",
    "status": "HUMAN_REVIEW_REQUIRED",
    "humanApprovalRequired": true,
    "approvalReasons": [
      "High-risk intent or incomplete suitability profile requires human review."
    ],
    "steps": [
      {
        "agentName": "MarketDataAgent",
        "role": "MARKET_DATA",
        "status": "COMPLETED",
        "summary": "Validated market quote for AAPL on NASDAQ.",
        "observations": [
          "Quote provider=local-mock-market-data, sourceType=MOCK"
        ],
        "riskWarnings": [
          "Mock prices are not real-time quotes."
        ],
        "requiresHumanApproval": false,
        "approvalReason": null
      }
    ]
  }
}
```

## 设计取舍

- 本阶段先做同步编排，保证最小闭环可运行；后续阶段再引入队列、异步任务、审批表和任务重放。
- 人工介入当前以结构化状态返回和审计事件落地，不直接阻塞教育性分析结果；后续可把 `approvalReasons` 接入审批工作台。
- Agent step 暂不单独建表，先复用 analysis response 和 audit event；等阶段 7 人工审批/Skill 管理落地时再统一任务表模型。
