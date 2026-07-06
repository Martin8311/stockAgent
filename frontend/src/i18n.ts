import type {
  AiProviderType,
  AssetType,
  CapitalPurpose,
  ComplianceNotice,
  InvestmentHorizon,
  MarketDataSourceType,
  RiskPreference,
  TokenUsageSource,
  TransactionType
} from "./types";

export type Language = "en" | "zh";

export const LANGUAGE_STORAGE_KEY = "harness_agent_language";

export const messages = {
  en: {
    language: {
      label: "Language",
      english: "EN",
      chinese: "中文"
    },
    brand: {
      title: "Harness Engineering Assistant",
      subtitle: "Investment research, portfolio workflow, agent governance"
    },
    status: {
      connecting: "Connecting"
    },
    hero: {
      eyebrow: "Phase 4 AI analysis foundation",
      title: "Selectable AI models with token accounting",
      body:
        "The assistant now combines secure identity, portfolio accounting, market data abstraction, local Ollama analysis, MiniMax paid-tier preparation, structured outputs, audit, and token usage records.",
      apiDocs: "API docs",
      serviceHealth: "Service health",
      backendUnavailable: "Backend API unavailable"
    },
    auth: {
      panelLabel: "Authentication panel",
      signedIn: "Signed in",
      accountAccess: "Account access",
      signOut: "Sign out",
      register: "Register",
      login: "Login",
      displayName: "Display name",
      email: "Email",
      password: "Password",
      createAccount: "Create account",
      signIn: "Sign in",
      registeredAndSignedIn: "Registered and signed in.",
      signedInMessage: "Signed in.",
      signedOut: "Signed out.",
      failed: "Authentication failed"
    },
    pipeline: {
      title: "Delivery pipeline",
      subtitle: "Identity, portfolio, market data, and AI analysis",
      steps: [
        {
          agent: "Identity",
          description: "User registration, password hashing, bearer token authentication, and role checks.",
          status: "Ready"
        },
        {
          agent: "Audit",
          description: "Registration, login, disclaimer access, profile changes, market data, and AI analysis are audited.",
          status: "Ready"
        },
        {
          agent: "Portfolio",
          description: "Transaction ledger, average cost, realized P/L, unrealized P/L, and holdings summary.",
          status: "Ready"
        },
        {
          agent: "MarketData",
          description: "Provider abstraction, local mock quotes, source confidence, assumptions, and audit logging.",
          status: "Ready"
        },
        {
          agent: "AnalysisAgent",
          description: "Selectable AI models, structured investment analysis, compliance guardrails, and token accounting.",
          status: "Ready"
        },
        {
          agent: "ComplianceAgent",
          description: "Investment outputs remain constrained by risk disclosure and suitability context.",
          status: "Baseline"
        }
      ]
    },
    compliance: {
      fallbackTitle: "Investment risk disclosure",
      loading: "Loading compliance notice."
    },
    profile: {
      title: "Investment profile context",
      subtitle: "Used to avoid unsuitable personalized suggestions",
      riskPreference: "Risk preference",
      investmentHorizon: "Investment horizon",
      capitalPurpose: "Capital purpose",
      save: "Save profile",
      updated: "Investment profile context updated."
    },
    portfolio: {
      title: "Portfolio workspace",
      subtitle: "Phase 2 holdings and transaction ledger",
      summaryTitle: "Portfolio summary",
      totalMarketValue: "Market value",
      totalCostBasis: "Cost basis",
      unrealizedPnl: "Unrealized P/L",
      realizedPnl: "Realized P/L",
      holdingCount: "Holdings",
      formTitle: "Record transaction",
      symbol: "Symbol",
      name: "Asset name",
      assetType: "Asset type",
      exchange: "Exchange",
      currency: "Currency",
      transactionType: "Transaction type",
      quantity: "Quantity",
      price: "Price",
      fee: "Fee",
      tradedAt: "Trade time",
      note: "Note",
      submit: "Save transaction",
      recorded: "Transaction recorded.",
      deleted: "Transaction deleted.",
      holdings: "Holdings",
      transactions: "Transaction ledger",
      averageCost: "Avg cost",
      latestPrice: "Latest price",
      riskWarnings: "Risk warnings",
      noHoldings: "No holdings yet.",
      noTransactions: "No transactions yet.",
      delete: "Delete",
      disclaimer:
        "Portfolio analytics are educational and risk-oriented only. They do not promise returns and do not replace advice from a licensed financial advisor."
    },
    marketData: {
      title: "Market data",
      subtitle: "Phase 3 mock provider and external adapter slot",
      lookupTitle: "Quote lookup",
      providerTitle: "Providers",
      symbol: "Symbol",
      exchange: "Exchange",
      currency: "Currency",
      lookup: "Get quote",
      latestPrice: "Latest price",
      previousClose: "Previous close",
      change: "Change",
      confidence: "Confidence",
      source: "Source",
      asOf: "As of",
      assumptions: "Assumptions",
      riskWarnings: "Market data risk warnings",
      enabled: "Enabled",
      disabled: "Disabled",
      approvalRequired: "Approval required",
      noQuote: "No quote loaded yet.",
      fetched: "Quote loaded."
    },
    ai: {
      title: "AI investment analysis",
      subtitle: "Phase 4 selectable model, structured output, and token billing baseline",
      formTitle: "Run analysis",
      model: "Model",
      symbol: "Symbol",
      exchange: "Exchange",
      currency: "Currency",
      question: "Question",
      includePortfolio: "Include portfolio context",
      submit: "Run analysis",
      running: "Analyzing...",
      completed: "Analysis completed.",
      modelCatalog: "Model catalog",
      localFree: "Local free",
      paidReserved: "Paid reserved",
      testFree: "Test free",
      apiKeyRequired: "API key",
      enabled: "Enabled",
      disabled: "Disabled",
      noAnalysis: "No AI analysis yet.",
      summary: "Summary",
      observations: "Key observations",
      assumptions: "Assumptions",
      educationalNotes: "Educational notes",
      riskWarnings: "Risk warnings",
      tokenUsage: "Token usage",
      promptTokens: "Prompt",
      completionTokens: "Completion",
      totalTokens: "Total",
      estimatedCost: "Estimated cost",
      usageSource: "Usage source",
      confidence: "Confidence",
      agentWorkflow: "Agent workflow",
      workflowStatus: "Workflow status",
      humanReviewRequired: "Human review required",
      noHumanReview: "No human review required",
      approvalReasons: "Approval reasons",
      disclaimer: "Disclaimer"
    },
    sandbox: {
      title: "Sandbox execution",
      subtitle: "Phase 6 governed execution for safe backtests and stress tests",
      formTitle: "Submit sandbox task",
      taskType: "Task type",
      script: "Safe DSL script",
      timeoutMs: "Timeout ms",
      submit: "Run sandbox task",
      running: "Running...",
      completed: "Sandbox task processed.",
      taskList: "Sandbox task history",
      status: "Status",
      riskLevel: "Risk",
      approvalReason: "Approval reason",
      error: "Error",
      output: "Output",
      riskWarnings: "Sandbox risk warnings",
      noOutput: "No executable output. The task may be rejected or pending approval.",
      noTasks: "No sandbox tasks yet."
    },
    errors: {
      unknownApi: "Unknown API error"
    }
  },
  zh: {
    language: {
      label: "语言",
      english: "EN",
      chinese: "中文"
    },
    brand: {
      title: "Harness Engineering 智能助手",
      subtitle: "投资研究、持仓工作流、Agent 治理"
    },
    status: {
      connecting: "连接中"
    },
    hero: {
      eyebrow: "阶段 4：AI 分析基础",
      title: "可选模型与 token 计费的智能分析",
      body:
        "当前系统已组合安全身份、持仓账本、市场数据抽象、本地 Ollama 分析、MiniMax 付费通道预留、结构化输出、审计和 token 使用记录。",
      apiDocs: "接口文档",
      serviceHealth: "服务健康",
      backendUnavailable: "后端 API 不可用"
    },
    auth: {
      panelLabel: "认证面板",
      signedIn: "已登录",
      accountAccess: "账户访问",
      signOut: "退出登录",
      register: "注册",
      login: "登录",
      displayName: "显示名称",
      email: "邮箱",
      password: "密码",
      createAccount: "创建账户",
      signIn: "登录",
      registeredAndSignedIn: "注册成功并已登录。",
      signedInMessage: "登录成功。",
      signedOut: "已退出登录。",
      failed: "认证失败"
    },
    pipeline: {
      title: "交付流水线",
      subtitle: "身份、持仓、市场数据与 AI 分析",
      steps: [
        {
          agent: "Identity",
          description: "用户注册、密码哈希、Bearer Token 认证和角色校验已经就绪。",
          status: "就绪"
        },
        {
          agent: "Audit",
          description: "注册、登录、免责声明访问、画像修改、行情请求和 AI 分析都会记录为审计事件。",
          status: "就绪"
        },
        {
          agent: "Portfolio",
          description: "交易台账、平均成本、已实现盈亏、未实现盈亏和持仓摘要已经就绪。",
          status: "就绪"
        },
        {
          agent: "MarketData",
          description: "数据源抽象、本地 Mock 行情、来源置信度、假设说明和审计日志已经接入。",
          status: "就绪"
        },
        {
          agent: "AnalysisAgent",
          description: "可选择 AI 模型、结构化投资分析、合规兜底和 token 计费记录已经接入。",
          status: "就绪"
        },
        {
          agent: "ComplianceAgent",
          description: "投资相关输出受风险披露和适当性上下文约束。",
          status: "基线"
        }
      ]
    },
    compliance: {
      fallbackTitle: "投资风险提示",
      loading: "正在加载合规提示。"
    },
    profile: {
      title: "投资画像上下文",
      subtitle: "用于避免不适当的个性化建议",
      riskPreference: "风险偏好",
      investmentHorizon: "投资期限",
      capitalPurpose: "资金用途",
      save: "保存画像",
      updated: "投资画像上下文已更新。"
    },
    portfolio: {
      title: "持仓工作台",
      subtitle: "阶段 2：持仓与交易台账",
      summaryTitle: "组合摘要",
      totalMarketValue: "持仓市值",
      totalCostBasis: "成本金额",
      unrealizedPnl: "未实现盈亏",
      realizedPnl: "已实现盈亏",
      holdingCount: "持仓数",
      formTitle: "录入交易",
      symbol: "代码",
      name: "资产名称",
      assetType: "资产类型",
      exchange: "交易所",
      currency: "币种",
      transactionType: "交易方向",
      quantity: "数量",
      price: "价格",
      fee: "费用",
      tradedAt: "交易时间",
      note: "备注",
      submit: "保存交易",
      recorded: "交易已记录。",
      deleted: "交易已删除。",
      holdings: "当前持仓",
      transactions: "交易台账",
      averageCost: "平均成本",
      latestPrice: "最新价格",
      riskWarnings: "风险提示",
      noHoldings: "暂无持仓。",
      noTransactions: "暂无交易。",
      delete: "删除",
      disclaimer: "组合分析仅用于教育性解释和风险提示，不承诺收益，也不替代持牌金融顾问意见。"
    },
    marketData: {
      title: "市场数据",
      subtitle: "阶段 3：Mock 行情源与外部适配预留",
      lookupTitle: "行情查询",
      providerTitle: "数据源",
      symbol: "代码",
      exchange: "交易所",
      currency: "币种",
      lookup: "查询行情",
      latestPrice: "最新价格",
      previousClose: "昨收",
      change: "涨跌",
      confidence: "置信度",
      source: "来源",
      asOf: "时间",
      assumptions: "假设",
      riskWarnings: "市场数据风险提示",
      enabled: "已启用",
      disabled: "未启用",
      approvalRequired: "需要审批",
      noQuote: "尚未加载行情。",
      fetched: "行情已加载。"
    },
    ai: {
      title: "AI 投资分析",
      subtitle: "阶段 4：可选模型、结构化输出与 token 计费基线",
      formTitle: "运行分析",
      model: "模型",
      symbol: "代码",
      exchange: "交易所",
      currency: "币种",
      question: "问题",
      includePortfolio: "包含持仓上下文",
      submit: "运行分析",
      running: "分析中...",
      completed: "分析已完成。",
      modelCatalog: "模型目录",
      localFree: "本地免费",
      paidReserved: "付费预留",
      testFree: "测试免费",
      apiKeyRequired: "需要 API Key",
      enabled: "已启用",
      disabled: "未启用",
      noAnalysis: "尚未生成 AI 分析。",
      summary: "摘要",
      observations: "关键观察",
      assumptions: "假设",
      educationalNotes: "教育性说明",
      riskWarnings: "风险提示",
      tokenUsage: "Token 使用",
      promptTokens: "Prompt",
      completionTokens: "Completion",
      totalTokens: "总计",
      estimatedCost: "预估费用",
      usageSource: "用量来源",
      confidence: "置信度",
      agentWorkflow: "Agent 工作流",
      workflowStatus: "工作流状态",
      humanReviewRequired: "需要人工复核",
      noHumanReview: "无需人工复核",
      approvalReasons: "复核原因",
      disclaimer: "免责声明"
    },
    sandbox: {
      title: "Sandbox 执行",
      subtitle: "阶段 6：用于安全回测和压力测试的受控执行环境",
      formTitle: "提交 Sandbox 任务",
      taskType: "任务类型",
      script: "安全 DSL 脚本",
      timeoutMs: "超时时间 ms",
      submit: "运行 Sandbox 任务",
      running: "运行中...",
      completed: "Sandbox 任务已处理。",
      taskList: "Sandbox 任务记录",
      status: "状态",
      riskLevel: "风险",
      approvalReason: "审批原因",
      error: "错误",
      output: "输出",
      riskWarnings: "Sandbox 风险提示",
      noOutput: "暂无可执行输出，任务可能已被拒绝或正在等待审批。",
      noTasks: "暂无 Sandbox 任务。"
    },
    errors: {
      unknownApi: "未知 API 错误"
    }
  }
} as const;

const riskPreferenceLabels: Record<Language, Record<RiskPreference, string>> = {
  en: {
    UNKNOWN: "Unknown",
    CONSERVATIVE: "Conservative",
    BALANCED: "Balanced",
    AGGRESSIVE: "Aggressive"
  },
  zh: {
    UNKNOWN: "未知",
    CONSERVATIVE: "稳健型",
    BALANCED: "平衡型",
    AGGRESSIVE: "进取型"
  }
};

const investmentHorizonLabels: Record<Language, Record<InvestmentHorizon, string>> = {
  en: {
    UNKNOWN: "Unknown",
    SHORT_TERM: "Short term",
    MID_TERM: "Mid term",
    LONG_TERM: "Long term"
  },
  zh: {
    UNKNOWN: "未知",
    SHORT_TERM: "短期",
    MID_TERM: "中期",
    LONG_TERM: "长期"
  }
};

const capitalPurposeLabels: Record<Language, Record<CapitalPurpose, string>> = {
  en: {
    UNKNOWN: "Unknown",
    LIQUIDITY_RESERVE: "Liquidity reserve",
    EDUCATION: "Education",
    HOUSE_PURCHASE: "House purchase",
    RETIREMENT: "Retirement",
    GENERAL_WEALTH: "General wealth"
  },
  zh: {
    UNKNOWN: "未知",
    LIQUIDITY_RESERVE: "流动性储备",
    EDUCATION: "教育支出",
    HOUSE_PURCHASE: "购房",
    RETIREMENT: "退休养老",
    GENERAL_WEALTH: "一般财富管理"
  }
};

const assetTypeLabels: Record<Language, Record<AssetType, string>> = {
  en: {
    STOCK: "Stock",
    FUND: "Fund",
    ETF: "ETF",
    BOND: "Bond",
    CASH: "Cash",
    CRYPTO: "Crypto",
    OTHER: "Other"
  },
  zh: {
    STOCK: "股票",
    FUND: "基金",
    ETF: "ETF",
    BOND: "债券",
    CASH: "现金",
    CRYPTO: "加密资产",
    OTHER: "其他"
  }
};

const transactionTypeLabels: Record<Language, Record<TransactionType, string>> = {
  en: {
    BUY: "Buy",
    SELL: "Sell"
  },
  zh: {
    BUY: "买入",
    SELL: "卖出"
  }
};

const marketDataSourceLabels: Record<Language, Record<MarketDataSourceType, string>> = {
  en: {
    MOCK: "Mock",
    EXTERNAL_PLACEHOLDER: "External placeholder"
  },
  zh: {
    MOCK: "Mock 本地数据",
    EXTERNAL_PLACEHOLDER: "外部适配预留"
  }
};

const aiProviderLabels: Record<Language, Record<AiProviderType, string>> = {
  en: {
    OLLAMA: "Ollama",
    MINIMAX: "MiniMax"
  },
  zh: {
    OLLAMA: "Ollama 本地模型",
    MINIMAX: "MiniMax"
  }
};

const tokenUsageSourceLabels: Record<Language, Record<TokenUsageSource, string>> = {
  en: {
    ACTUAL: "Actual",
    ESTIMATED: "Estimated",
    MOCK: "Mock"
  },
  zh: {
    ACTUAL: "实际返回",
    ESTIMATED: "估算",
    MOCK: "Mock"
  }
};

const zhComplianceNotice: ComplianceNotice = {
  title: "投资研究合规提示",
  allowedUse: "本系统仅提供教育性解释、辅助分析和风险提醒。",
  limitations: "本系统不承诺收益，不替代持牌金融顾问意见，任何投资决策都需要用户自行判断并承担风险。",
  requiredDisclosures: [
    "投资有风险，包括本金亏损的可能。",
    "历史表现不代表未来结果。",
    "任何预测都必须说明假设、数据来源、置信度和风险。",
    "个性化建议需要补充风险偏好、投资期限和资金用途等上下文。"
  ]
};

export function normalizeLanguage(value: string | null): Language {
  return value === "zh" ? "zh" : "en";
}

export function getRiskPreferenceLabel(language: Language, value: RiskPreference): string {
  return riskPreferenceLabels[language][value];
}

export function getInvestmentHorizonLabel(language: Language, value: InvestmentHorizon): string {
  return investmentHorizonLabels[language][value];
}

export function getCapitalPurposeLabel(language: Language, value: CapitalPurpose): string {
  return capitalPurposeLabels[language][value];
}

export function getAssetTypeLabel(language: Language, value: AssetType): string {
  return assetTypeLabels[language][value];
}

export function getTransactionTypeLabel(language: Language, value: TransactionType): string {
  return transactionTypeLabels[language][value];
}

export function getMarketDataSourceLabel(language: Language, value: MarketDataSourceType): string {
  return marketDataSourceLabels[language][value];
}

export function getAiProviderLabel(language: Language, value: AiProviderType): string {
  return aiProviderLabels[language][value];
}

export function getTokenUsageSourceLabel(language: Language, value: TokenUsageSource): string {
  return tokenUsageSourceLabels[language][value];
}

export function localizePortfolioRiskWarnings(language: Language, warnings: string[]): string[] {
  if (language === "en") {
    return warnings;
  }

  return warnings.map((warning) => {
    if (warning.startsWith("No holdings recorded yet.")) {
      return "尚未录入持仓，缺少仓位数据时无法评估组合风险。";
    }
    if (warning.startsWith("Single-position portfolio detected.")) {
      return "检测到单一持仓组合，集中度风险可能较高。";
    }
    if (warning.includes("is more than 50% of portfolio market value")) {
      const symbol = warning.split(" ")[0];
      return `${symbol} 超过组合市值的 50%，请关注集中持仓风险。`;
    }
    if (warning.startsWith("Portfolio unrealized loss is greater than 10%")) {
      return "组合未实现亏损超过成本金额的 10%，请结合风险承受能力和流动性需求复核。";
    }
    if (warning.startsWith("Portfolio analytics are educational")) {
      return messages.zh.portfolio.disclaimer;
    }
    return warning;
  });
}

export function localizeMarketDataText(language: Language, lines: string[]): string[] {
  if (language === "en") {
    return lines;
  }

  return lines.map((line) => {
    if (line.startsWith("Quote is generated by a deterministic local mock provider.")) {
      return "行情由确定性的本地 Mock Provider 生成。";
    }
    if (line.startsWith("Price is intended to exercise portfolio valuation flow")) {
      return "该价格仅用于在外部行情接入前验证组合估值流程。";
    }
    if (line.startsWith("Mock prices are not real-time quotes.")) {
      return "Mock 价格不是实时行情。";
    }
    if (line.startsWith("Do not use mock data for actual investment decisions.")) {
      return "不要将 Mock 数据用于真实投资决策。";
    }
    return line;
  });
}

export function getLocalizedComplianceNotice(
  language: Language,
  notice: ComplianceNotice | null
): ComplianceNotice | null {
  if (!notice) {
    return null;
  }
  return language === "zh" ? zhComplianceNotice : notice;
}
