import type {
  ApiResponse,
  AuthResponse,
  ComplianceNotice,
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
