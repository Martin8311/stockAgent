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
