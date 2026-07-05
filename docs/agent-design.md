# Agent Design

## Agent 职责

- `MarketDataAgent`：采集、清洗、校验市场数据，记录数据来源与时间。
- `PortfolioAgent`：分析持仓结构、盈亏、资产集中度和再平衡空间。
- `RiskAgent`：评估波动、集中度、回撤、流动性、杠杆和用户画像缺口。
- `StrategyAgent`：生成辅助分析、可选方案和教育性解释，不给确定性收益承诺。
- `ComplianceAgent`：检查输出是否包含免责声明、风险提示、假设、数据来源和禁止用语。
- `SupervisorAgent`：编排任务、触发人工介入、汇总结构化结果。

## 结构化输出草案

```json
{
  "taskId": "uuid",
  "agent": "RiskAgent",
  "status": "COMPLETED",
  "dataSources": [
    {
      "name": "mock-market-data",
      "asOf": "2026-07-05T00:00:00Z"
    }
  ],
  "assumptions": [
    "用户未提供完整风险偏好，默认不生成强个性化建议"
  ],
  "findings": [
    {
      "title": "持仓集中度偏高",
      "severity": "MEDIUM",
      "evidence": "单一资产占比超过 40%"
    }
  ],
  "recommendations": [
    {
      "type": "OPTIONAL_ACTION",
      "content": "可评估分批再平衡，但需结合税费、流动性和个人投资期限",
      "confidence": 0.62
    }
  ],
  "requiredDisclosures": [
    "本内容仅用于辅助分析和教育性解释，不构成投资建议",
    "历史表现不代表未来收益"
  ],
  "humanApprovalRequired": true
}
```

## 禁止输出

- “保证上涨”
- “稳赚”
- “必买”
- “无风险收益”
- 未说明假设和风险的强个性化建议

## 人工介入触发点

- 生成投资建议或组合再平衡建议。
- 调用外部付费或受限市场数据接口。
- 运行 Sandbox 脚本或策略回测。
- 新增、修改、启用 Skill。
- 推送远程仓库或发布演示环境。

