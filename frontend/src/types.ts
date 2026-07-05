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

