import type { ApiResponse, ComplianceNotice, SystemHealth } from "./types";

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

export function getSystemHealth(): Promise<ApiResponse<SystemHealth>> {
  return getJson<ApiResponse<SystemHealth>>("/api/public/system/health");
}

export function getComplianceNotice(): Promise<ApiResponse<ComplianceNotice>> {
  return getJson<ApiResponse<ComplianceNotice>>("/api/public/compliance/disclaimer");
}

