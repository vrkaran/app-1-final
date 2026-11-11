/**
 * HBX SOLAR ONE — Immutable Security Audit Logger
 */
import * as admin from "firebase-admin";
import { SecurityAuditRecord } from "./types";

export async function logSecurityEvent(
  db: admin.firestore.Firestore,
  record: Omit<SecurityAuditRecord, "logId" | "timestamp"> & { logId?: string }
): Promise<string> {
  const logId = record.logId || `SEC-${Date.now()}-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;
  const auditDocRef = db.collection("security_audit_logs").doc(logId);

  const fullRecord: SecurityAuditRecord = {
    ...record,
    logId,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  };

  await auditDocRef.set(fullRecord);
  return logId;
}
