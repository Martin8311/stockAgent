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
