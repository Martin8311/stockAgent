import type {
  CapitalPurpose,
  ComplianceNotice,
  InvestmentHorizon,
  RiskPreference
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
      eyebrow: "Phase 1 identity foundation",
      title: "Secure sign-in, profile context, and role-aware workflow baseline",
      body:
        "The assistant now supports registration, login, bearer token authentication, role checks, and investment profile context. This creates the trust boundary for portfolio, market data, sandbox, and agent workflows.",
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
      subtitle: "Identity first",
      steps: [
        {
          agent: "Identity",
          description: "User registration, password hashing, bearer token authentication, and role checks.",
          status: "Ready"
        },
        {
          agent: "Audit",
          description: "Registration, login, disclaimer access, and profile changes are recorded as audit events.",
          status: "Ready"
        },
        {
          agent: "PortfolioAgent",
          description: "Holdings and portfolio analysis begin in the next delivery phase.",
          status: "Next"
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
      eyebrow: "阶段 1：认证与身份基础",
      title: "安全登录、画像上下文与基于角色的工作流基线",
      body:
        "当前系统已支持注册、登录、Bearer Token 认证、角色校验和投资画像上下文，为后续持仓、市场数据、Sandbox 与 Agent 工作流建立可信边界。",
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
      subtitle: "身份优先",
      steps: [
        {
          agent: "Identity",
          description: "用户注册、密码哈希、Bearer Token 认证和角色校验已经就绪。",
          status: "就绪"
        },
        {
          agent: "Audit",
          description: "注册、登录、免责声明访问和画像修改都会记录为审计事件。",
          status: "就绪"
        },
        {
          agent: "PortfolioAgent",
          description: "持仓管理和组合分析将在下一阶段开始交付。",
          status: "下一步"
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

export function getLocalizedComplianceNotice(
  language: Language,
  notice: ComplianceNotice | null
): ComplianceNotice | null {
  if (!notice) {
    return null;
  }
  return language === "zh" ? zhComplianceNotice : notice;
}
