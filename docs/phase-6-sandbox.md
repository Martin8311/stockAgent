# Phase 6 Sandbox Execution

阶段 6 引入受控 Sandbox 执行环境，用于策略回测、组合压力测试和后续 Skill 验证。当前实现采用安全 DSL / 白名单任务执行，不开放任意 Python、JavaScript、Shell 或外部网络访问。

## 为什么需要 Sandbox

投资研究助手后续会遇到可执行逻辑：

- 策略回测，例如均线策略、调仓策略、风险预算策略。
- 组合压力测试，例如单资产下跌、整体市场冲击、集中持仓冲击。
- LLM 生成的计算逻辑，需要先验证再进入用户报告。
- Skill 自我进化，需要在启用前测试新 skill 的行为。
- 外部行情或用户上传数据处理，需要隔离解析逻辑。

这些能力如果直接运行任意脚本，会带来文件读取、环境变量泄露、网络访问、无限循环、超大输出、命令执行和审计绕过风险。

## 当前实现

### API

```text
POST /api/sandbox/tasks
GET  /api/sandbox/tasks
GET  /api/sandbox/tasks/{taskId}
```

### 白名单任务

1. `MOCK_BACKTEST`
   - 输入：`symbol`、`initialCapital`、`lookbackDays`、`strategy`
   - 作用：用确定性本地公式生成模拟回测结果。
   - 限制：不读取真实行情，不访问网络，不代表历史或未来收益。

2. `PORTFOLIO_STRESS_TEST`
   - 输入：`shockPercent`、`shock.{SYMBOL}`
   - 作用：基于当前持仓摘要做组合压力测试。
   - 限制：只做情景分析，不作为投资建议。

### DSL 示例

```text
symbol=AAPL
initialCapital=10000
lookbackDays=60
strategy=moving-average-cross
```

```text
shockPercent=-0.10
shock.AAPL=-0.20
```

## 安全策略

`SandboxPolicyService` 在执行前做准入控制：

- 限制脚本长度。
- 限制超时时间。
- 拒绝系统命令、网络访问、文件访问、环境变量、密钥、SQL 破坏性关键词。
- 对杠杆、融资、融券、期权、期货、真实交易、外部接口、付费数据等关键词标记 `PENDING_APPROVAL`。

## 生命周期

```text
SUBMIT
  -> REJECTED
  -> PENDING_APPROVAL
  -> COMPLETED
  -> FAILED
```

当前 `SandboxTaskStatus` 包含：

- `COMPLETED`
- `FAILED`
- `REJECTED`
- `PENDING_APPROVAL`

## 审计事件

- `SANDBOX_TASK_SUBMITTED`
- `SANDBOX_TASK_COMPLETED`
- `SANDBOX_TASK_FAILED`
- `SANDBOX_TASK_REJECTED`
- `SANDBOX_TASK_REQUIRES_APPROVAL`

## 设计取舍

- 当前不开放任意脚本执行，因为没有容器级隔离和完整审批流。
- 当前使用同步执行，便于 MVP 闭环和前端展示；后续可演进为异步队列。
- 当前结果存为任务记录 JSON，便于审计和回放；后续可拆分为更细的执行步骤表。
- 当前 `PENDING_APPROVAL` 只记录审批状态，后续阶段 7 会接入人工审批工作台。

## 面试表达

Sandbox 的价值不是“跑脚本”，而是安全地承载 Agent 工具调用。我的实现把执行能力拆成策略准入、白名单执行、任务持久化、风险状态、审计事件和前端工作台，先保证可控，再逐步演进到 Docker/队列/审批流。
