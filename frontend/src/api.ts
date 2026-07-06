import type {
  ApiResponse,
  AuthResponse,
  AiModelDescriptor,
  ApprovalRequest,
  ApprovalStatus,
  ComplianceNotice,
  CreateSkillPayload,
  CreateSkillVersionPayload,
  InvestmentAnalysisPayload,
  InvestmentAnalysisResponse,
  MarketDataProvider,
  MarketQuote,
  PortfolioSummary,
  PortfolioTransaction,
  PortfolioTransactionPayload,
  SandboxTask,
  SandboxTaskPayload,
  SkillDefinition,
  SkillVersion,
  SubmitSkillApprovalPayload,
  LoginPayload,
  RegisterPayload,
  SystemHealth,
  UpdateProfilePayload,
  UserProfile
} from "./types";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      Accept: "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

async function sendJson<T>(method: "POST" | "PUT", path: string, body: unknown, token?: string): Promise<T> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json"
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    method,
    headers,
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    const fallback = `Request failed with status ${response.status}`;
    try {
      const errorBody = (await response.json()) as { message?: string; detail?: string };
      throw new Error(errorBody.message ?? errorBody.detail ?? fallback);
    } catch (error) {
      if (error instanceof Error && error.message !== fallback) {
        throw error;
      }
      throw new Error(fallback);
    }
  }

  return response.json() as Promise<T>;
}

async function getJsonWithToken<T>(path: string, token: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

async function deleteJson<T>(path: string, token: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "DELETE",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getSystemHealth(): Promise<ApiResponse<SystemHealth>> {
  return getJson<ApiResponse<SystemHealth>>("/api/public/system/health");
}

export function getComplianceNotice(): Promise<ApiResponse<ComplianceNotice>> {
  return getJson<ApiResponse<ComplianceNotice>>("/api/public/compliance/disclaimer");
}

export function register(payload: RegisterPayload): Promise<ApiResponse<AuthResponse>> {
  return sendJson<ApiResponse<AuthResponse>>("POST", "/api/public/auth/register", payload);
}

export function login(payload: LoginPayload): Promise<ApiResponse<AuthResponse>> {
  return sendJson<ApiResponse<AuthResponse>>("POST", "/api/public/auth/login", payload);
}

export function getCurrentUser(token: string): Promise<ApiResponse<UserProfile>> {
  return getJsonWithToken<ApiResponse<UserProfile>>("/api/me", token);
}

export function updateCurrentUserProfile(
  token: string,
  payload: UpdateProfilePayload
): Promise<ApiResponse<UserProfile>> {
  return sendJson<ApiResponse<UserProfile>>("PUT", "/api/me/profile", payload, token);
}

export function getPortfolioSummary(token: string): Promise<ApiResponse<PortfolioSummary>> {
  return getJsonWithToken<ApiResponse<PortfolioSummary>>("/api/portfolio/summary", token);
}

export function getPortfolioTransactions(token: string): Promise<ApiResponse<PortfolioTransaction[]>> {
  return getJsonWithToken<ApiResponse<PortfolioTransaction[]>>("/api/portfolio/transactions", token);
}

export function recordPortfolioTransaction(
  token: string,
  payload: PortfolioTransactionPayload
): Promise<ApiResponse<PortfolioTransaction>> {
  return sendJson<ApiResponse<PortfolioTransaction>>("POST", "/api/portfolio/transactions", payload, token);
}

export function deletePortfolioTransaction(token: string, transactionId: number): Promise<ApiResponse<null>> {
  return deleteJson<ApiResponse<null>>(`/api/portfolio/transactions/${transactionId}`, token);
}

export function getMarketDataProviders(token: string): Promise<ApiResponse<MarketDataProvider[]>> {
  return getJsonWithToken<ApiResponse<MarketDataProvider[]>>("/api/market-data/providers", token);
}

export function getMarketQuote(
  token: string,
  symbol: string,
  exchange: string,
  currency: string
): Promise<ApiResponse<MarketQuote>> {
  const params = new URLSearchParams({ symbol, exchange, currency });
  return getJsonWithToken<ApiResponse<MarketQuote>>(`/api/market-data/quote?${params.toString()}`, token);
}

export function getAiModels(token: string): Promise<ApiResponse<AiModelDescriptor[]>> {
  return getJsonWithToken<ApiResponse<AiModelDescriptor[]>>("/api/ai/models", token);
}

export function requestInvestmentAnalysis(
  token: string,
  payload: InvestmentAnalysisPayload
): Promise<ApiResponse<InvestmentAnalysisResponse>> {
  return sendJson<ApiResponse<InvestmentAnalysisResponse>>("POST", "/api/ai/analysis", payload, token);
}

export function getSandboxTasks(token: string): Promise<ApiResponse<SandboxTask[]>> {
  return getJsonWithToken<ApiResponse<SandboxTask[]>>("/api/sandbox/tasks", token);
}

export function submitSandboxTask(
  token: string,
  payload: SandboxTaskPayload
): Promise<ApiResponse<SandboxTask>> {
  return sendJson<ApiResponse<SandboxTask>>("POST", "/api/sandbox/tasks", payload, token);
}

export function getSkills(token: string): Promise<ApiResponse<SkillDefinition[]>> {
  return getJsonWithToken<ApiResponse<SkillDefinition[]>>("/api/skills", token);
}

export function getAdminSkills(token: string): Promise<ApiResponse<SkillDefinition[]>> {
  return getJsonWithToken<ApiResponse<SkillDefinition[]>>("/api/admin/skills", token);
}

export function createSkill(token: string, payload: CreateSkillPayload): Promise<ApiResponse<SkillDefinition>> {
  return sendJson<ApiResponse<SkillDefinition>>("POST", "/api/admin/skills", payload, token);
}

export function createSkillVersion(
  token: string,
  skillId: number,
  payload: CreateSkillVersionPayload
): Promise<ApiResponse<SkillDefinition>> {
  return sendJson<ApiResponse<SkillDefinition>>("POST", `/api/admin/skills/${skillId}/versions`, payload, token);
}

export function testSkillVersion(token: string, versionId: number): Promise<ApiResponse<SkillVersion>> {
  return sendJson<ApiResponse<SkillVersion>>("POST", `/api/admin/skills/versions/${versionId}/test`, {}, token);
}

export function submitSkillVersionApproval(
  token: string,
  versionId: number,
  payload: SubmitSkillApprovalPayload
): Promise<ApiResponse<ApprovalRequest>> {
  return sendJson<ApiResponse<ApprovalRequest>>(
    "POST",
    `/api/admin/skills/versions/${versionId}/submit-approval`,
    payload,
    token
  );
}

export function activateSkillVersion(token: string, versionId: number): Promise<ApiResponse<SkillDefinition>> {
  return sendJson<ApiResponse<SkillDefinition>>("POST", `/api/admin/skills/versions/${versionId}/activate`, {}, token);
}

export function getApprovals(token: string, status?: ApprovalStatus): Promise<ApiResponse<ApprovalRequest[]>> {
  const query = status ? `?status=${status}` : "";
  return getJsonWithToken<ApiResponse<ApprovalRequest[]>>(`/api/admin/approvals${query}`, token);
}

export function approveApproval(
  token: string,
  approvalId: number,
  comment: string
): Promise<ApiResponse<ApprovalRequest>> {
  return sendJson<ApiResponse<ApprovalRequest>>("POST", `/api/admin/approvals/${approvalId}/approve`, { comment }, token);
}

export function rejectApproval(
  token: string,
  approvalId: number,
  comment: string
): Promise<ApiResponse<ApprovalRequest>> {
  return sendJson<ApiResponse<ApprovalRequest>>("POST", `/api/admin/approvals/${approvalId}/reject`, { comment }, token);
}
