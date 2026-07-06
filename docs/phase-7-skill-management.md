# Phase 7 Skill Management

阶段 7 引入可治理的 Skill 管理能力，用于承载 Agent 的可控能力演进。它不是简单的 prompt 模板 CRUD，而是包含版本、测试、审批、激活和回滚的完整治理链路。

## 为什么需要 Skill 管理

投资分析 Agent 的能力会持续扩展，例如：

- 新增组合风险检查规则。
- 新增策略解释模板。
- 新增合规输出检查规则。
- 新增 Sandbox 回测模板。

如果这些能力由模型或用户直接写入生产分析流程，会带来以下风险：

- 未测试规则污染投资分析结果。
- 高风险策略绕过人工审核。
- Skill 更新不可追溯，无法回滚。
- 审计日志无法解释某次分析为何采用某条规则。

因此阶段 7 把 Skill 设计成受控资产：只有通过 Sandbox 测试、人工审批并被管理员激活的版本，才会进入 Agent 分析上下文。

## 状态流

```text
DRAFT
  -> TESTED
  -> PENDING_APPROVAL
  -> APPROVED
  -> ACTIVE
```

拒绝路径：

```text
PENDING_APPROVAL -> REJECTED
```

回滚方式：

```text
ACTIVE(v3) -> APPROVED(v3)
APPROVED(v2) -> ACTIVE(v2)
```

## 后端模块

- `skill`：Skill 定义、版本、测试、激活和 Agent runtime context。
- `approval`：审批请求、审批状态和审批决策 DTO。
- `sandbox`：复用阶段 6 的白名单 DSL 执行器测试 Skill 版本。
- `audit`：记录创建、测试、提交审批、批准、拒绝和激活事件。

## 核心 API

普通用户：

```http
GET /api/skills
```

管理员：

```http
GET  /api/admin/skills
POST /api/admin/skills
POST /api/admin/skills/{skillId}/versions
POST /api/admin/skills/versions/{versionId}/test
POST /api/admin/skills/versions/{versionId}/submit-approval
POST /api/admin/skills/versions/{versionId}/activate
```

审批：

```http
GET  /api/admin/approvals?status=PENDING
POST /api/admin/approvals/{approvalId}/approve
POST /api/admin/approvals/{approvalId}/reject
```

## Agent 集成

`SkillRuntimeService` 只读取：

- `skill_definition.enabled = true`
- `active_version_id` 指向的版本
- `skill_version.status = ACTIVE`

然后将 active skill 摘要注入投资分析 prompt 的 `Active governed skills` 区域。未测试、未审批或未激活的版本不会进入模型上下文。

## 当前取舍

- `active_version_id` 暂不建外键，避免 `skill_definition` 与 `skill_version` 之间产生循环外键；激活时由 service 校验版本归属和状态。
- Skill 测试当前复用 `MOCK_BACKTEST` DSL，不开放任意脚本执行。
- 审批表先做通用 target 模型，当前只接 `SKILL_VERSION`，后续可复用到外部数据源、Sandbox 高风险任务、远程 push 等高风险动作。
- 前端先做单页 Admin 工作台，不引入路由和复杂状态管理。

## 面试表达

我把 Skill 自我进化拆成“版本化资产 + Sandbox 测试 + 人工审批 + 激活指针 + 审计日志”。这样 Agent 可以演进能力，但不会绕过安全边界；任何进入分析链路的规则都能说明来源、版本、审批人和测试结果。
