export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export interface SystemHealth {
  service: string;
  phase: string;
  status: string;
  complianceGuardEnabled: boolean;
  timestamp: string;
}

export interface ComplianceNotice {
  title: string;
  allowedUse: string;
  limitations: string;
  requiredDisclosures: string[];
}

export type AppRole = "USER" | "ADMIN";
export type UserStatus = "ACTIVE" | "DISABLED";
export type RiskPreference = "UNKNOWN" | "CONSERVATIVE" | "BALANCED" | "AGGRESSIVE";
export type InvestmentHorizon = "UNKNOWN" | "SHORT_TERM" | "MID_TERM" | "LONG_TERM";
export type CapitalPurpose =
  | "UNKNOWN"
  | "LIQUIDITY_RESERVE"
  | "EDUCATION"
  | "HOUSE_PURCHASE"
  | "RETIREMENT"
  | "GENERAL_WEALTH";
export type AssetType = "STOCK" | "FUND" | "ETF" | "BOND" | "CASH" | "CRYPTO" | "OTHER";
export type TransactionType = "BUY" | "SELL";
export type MarketDataSourceType = "MOCK" | "EXTERNAL_PLACEHOLDER";
export type AiProviderType = "OLLAMA" | "MINIMAX";
export type TokenUsageSource = "ACTUAL" | "ESTIMATED" | "MOCK";
export type AgentRole = "MARKET_DATA" | "PORTFOLIO" | "RISK" | "STRATEGY" | "COMPLIANCE" | "SUPERVISOR";
export type AgentStepStatus = "COMPLETED" | "SKIPPED" | "REVIEW_REQUIRED";
export type AgentWorkflowStatus = "COMPLETED" | "HUMAN_REVIEW_REQUIRED";
export type SandboxTaskType = "MOCK_BACKTEST" | "PORTFOLIO_STRESS_TEST";
export type SandboxTaskStatus = "COMPLETED" | "FAILED" | "REJECTED" | "PENDING_APPROVAL";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type SkillCategory =
  | "PORTFOLIO_ANALYSIS"
  | "RISK_CONTROL"
  | "STRATEGY_EXPLANATION"
  | "COMPLIANCE_REVIEW"
  | "SANDBOX_TEMPLATE";
export type SkillVersionStatus =
  | "DRAFT"
  | "TESTED"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "ACTIVE"
  | "REJECTED"
  | "ARCHIVED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ApprovalRequestType = "SKILL_VERSION_APPROVAL";
export type ApprovalTargetType = "SKILL_VERSION";

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  roles: AppRole[];
}

export interface AuthResponse {
  tokenType: "Bearer";
  accessToken: string;
  expiresAt: string;
  user: AuthUser;
}

export interface UserProfile {
  id: number;
  email: string;
  displayName: string;
  status: UserStatus;
  roles: AppRole[];
  riskPreference: RiskPreference;
  investmentHorizon: InvestmentHorizon;
  capitalPurpose: CapitalPurpose;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface UpdateProfilePayload {
  riskPreference: RiskPreference;
  investmentHorizon: InvestmentHorizon;
  capitalPurpose: CapitalPurpose;
}

export interface PortfolioAsset {
  id: number;
  symbol: string;
  name: string;
  assetType: AssetType;
  exchange: string;
  currency: string;
  latestPrice: number;
}

export interface PortfolioTransaction {
  id: number;
  asset: PortfolioAsset;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  fee: number;
  grossAmount: number;
  tradedAt: string;
  note: string | null;
}

export interface Holding {
  asset: PortfolioAsset;
  quantity: number;
  averageCost: number;
  costBasis: number;
  latestPrice: number;
  marketValue: number;
  unrealizedPnl: number;
  unrealizedPnlRatio: number;
  realizedPnl: number;
}

export interface PortfolioSummary {
  totalCostBasis: number;
  totalMarketValue: number;
  totalUnrealizedPnl: number;
  totalUnrealizedPnlRatio: number;
  totalRealizedPnl: number;
  holdingCount: number;
  holdings: Holding[];
  riskWarnings: string[];
  disclaimer: string;
}

export interface PortfolioTransactionPayload {
  symbol: string;
  name: string;
  assetType: AssetType;
  exchange: string;
  currency: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  fee: number;
  tradedAt: string;
  note: string;
}

export interface MarketDataProvider {
  name: string;
  sourceType: MarketDataSourceType;
  enabled: boolean;
  requiresApproval: boolean;
  description: string;
}

export interface MarketQuote {
  symbol: string;
  exchange: string;
  currency: string;
  latestPrice: number;
  previousClose: number;
  changeAmount: number;
  changePercent: number;
  asOf: string;
  provider: string;
  sourceType: MarketDataSourceType;
  confidence: number;
  assumptions: string[];
  riskWarnings: string[];
  disclaimer: string;
}

export interface AiModelDescriptor {
  id: string;
  provider: AiProviderType;
  displayName: string;
  modelName: string;
  enabled: boolean;
  local: boolean;
  freeTier: boolean;
  paidTier: boolean;
  testModeFree: boolean;
  requiresApiKey: boolean;
  billingMode: string;
  promptPricePerMillionTokens: number;
  completionPricePerMillionTokens: number;
  currency: string;
  statusNote: string;
}

export interface TokenUsage {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  usageSource: TokenUsageSource;
  estimatedCost: number;
  currency: string;
  billable: boolean;
  testMode: boolean;
}

export interface AgentStep {
  agentName: string;
  role: AgentRole;
  status: AgentStepStatus;
  summary: string;
  observations: string[];
  riskWarnings: string[];
  requiresHumanApproval: boolean;
  approvalReason: string | null;
}

export interface AgentWorkflow {
  workflowId: string;
  status: AgentWorkflowStatus;
  humanApprovalRequired: boolean;
  approvalReasons: string[];
  steps: AgentStep[];
}

export interface InvestmentAnalysisPayload {
  modelId: string;
  symbol: string;
  exchange: string;
  currency: string;
  question: string;
  includePortfolioContext: boolean;
}

export interface InvestmentAnalysisResponse {
  analysisId: number;
  symbol: string;
  exchange: string;
  currency: string;
  model: AiModelDescriptor;
  quote: MarketQuote;
  investmentSummary: string;
  keyObservations: string[];
  assumptions: string[];
  riskWarnings: string[];
  educationalNotes: string[];
  confidence: number;
  tokenUsage: TokenUsage;
  agentWorkflow: AgentWorkflow;
  disclaimer: string;
  createdAt: string;
}

export interface SandboxExecutionOutput {
  summary: string;
  metrics: Record<string, unknown>;
  observations: string[];
  riskWarnings: string[];
  disclaimer: string;
}

export interface SandboxTask {
  id: number;
  taskType: SandboxTaskType;
  status: SandboxTaskStatus;
  riskLevel: RiskLevel;
  script: string;
  output: SandboxExecutionOutput | null;
  errorMessage: string | null;
  approvalReason: string | null;
  timeoutMs: number;
  executionTimeMs: number | null;
  createdAt: string;
  completedAt: string | null;
}

export interface SandboxTaskPayload {
  taskType: SandboxTaskType;
  script: string;
  timeoutMs: number;
}

export interface SkillTestResult {
  passed: boolean;
  sandboxTaskId: number | null;
  sandboxStatus: string;
  summary: string;
  checks: string[];
  riskWarnings: string[];
  testedAt: string;
}

export interface SkillVersion {
  id: number;
  skillId: number;
  versionNumber: number;
  status: SkillVersionStatus;
  content: string;
  testScript: string;
  testResult: SkillTestResult | null;
  approvalReason: string | null;
  createdBy: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SkillDefinition {
  id: number;
  skillKey: string;
  name: string;
  description: string | null;
  category: SkillCategory;
  enabled: boolean;
  activeVersionId: number | null;
  activeVersion: SkillVersion | null;
  versions: SkillVersion[];
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSkillPayload {
  skillKey: string;
  name: string;
  description: string;
  category: SkillCategory;
  content: string;
  testScript: string;
}

export interface CreateSkillVersionPayload {
  content: string;
  testScript: string;
}

export interface SubmitSkillApprovalPayload {
  reason: string;
}

export interface ApprovalRequest {
  id: number;
  requestType: ApprovalRequestType;
  targetType: ApprovalTargetType;
  targetId: number;
  status: ApprovalStatus;
  reason: string;
  decisionComment: string | null;
  requestedBy: string;
  reviewedBy: string | null;
  createdAt: string;
  decidedAt: string | null;
}
