/**
 * HBX SOLAR ONE — Trusted Backend Types & Security Enums
 */

export enum UserRole {
  SUPER_ADMIN = "SUPER_ADMIN",
  ADMIN = "ADMIN",
  OPERATIONS_MANAGER = "OPERATIONS_MANAGER",
  SALES_MANAGER = "SALES_MANAGER",
  TEAM_LEADER = "TEAM_LEADER",
  BDE = "BDE",
  INSTALLATION_ENGINEER = "INSTALLATION_ENGINEER",
  SERVICE_ENGINEER = "SERVICE_ENGINEER",
  PARTNER = "PARTNER",
  CUSTOMER = "CUSTOMER",
}

export enum AccountStatus {
  ACTIVE = "ACTIVE",
  PENDING_APPROVAL = "PENDING_APPROVAL",
  SUSPENDED = "SUSPENDED",
  INACTIVE = "INACTIVE",
}

export interface ProvisionStaffRequest {
  targetUid: string;
  role: UserRole;
  employeeId?: string;
  territoryId?: string;
}

export interface ProvisionStaffResponse {
  success: boolean;
  targetUid: string;
  assignedRole: UserRole;
  provisionedAt: string;
  message: string;
}

export interface SecurityAuditRecord {
  logId: string;
  eventId?: string;
  action: "STAFF_PROVISIONED" | "ACCOUNT_SUSPENDED" | "ACCOUNT_REACTIVATED" | "STORAGE_SIGNED_URL_ISSUED";
  actorUid: string;
  actorRole?: string;
  targetUid: string;
  previousRole?: string;
  assignedRole?: string;
  previousStatus?: string;
  newStatus?: string;
  tokensRevoked?: boolean;
  timestamp: FirebaseFirestore.FieldValue;
  metadata?: Record<string, unknown>;
}
