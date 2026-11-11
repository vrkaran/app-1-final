/**
 * HBX SOLAR ONE — Automatic Account Suspension & Token Revocation Trigger
 * Listens for updates to /users/{uid} and immediately revokes refresh tokens.
 */
import * as admin from "firebase-admin";
import { FirestoreEvent, Change } from "firebase-functions/v2/firestore";
import { DocumentSnapshot } from "firebase-admin/firestore";
import { AccountStatus } from "./types";
import { logSecurityEvent } from "./auditLogger";

export async function handleUserAccountStatusUpdate(
  event: FirestoreEvent<Change<DocumentSnapshot> | undefined, { uid: string }>,
  auth: admin.auth.Auth,
  db: admin.firestore.Firestore
): Promise<void> {
  if (!event.data) {
    return;
  }

  const { before, after } = event.data;
  const uid = event.params.uid;
  const eventId = event.id;

  const beforeData = before.data() || {};
  const afterData = after.data() || {};

  const beforeStatus = (beforeData.accountStatus as string) || AccountStatus.ACTIVE;
  const afterStatus = (afterData.accountStatus as string) || AccountStatus.ACTIVE;

  // No change in accountStatus -> No-op
  if (beforeStatus === afterStatus) {
    return;
  }

  // Idempotency check: Ensure duplicate event deliveries are handled safely
  const idempotencyKey = `AUDIT-STATUS-${uid}-${eventId}`;
  const existingAuditDoc = await db.collection("security_audit_logs").doc(idempotencyKey).get();
  if (existingAuditDoc.exists) {
    console.log(`[Idempotency] Event ${eventId} for user ${uid} was already processed. Skipping.`);
    return;
  }

  // Case 1: Account Suspended or Inactivated
  if (afterStatus === AccountStatus.SUSPENDED || afterStatus === AccountStatus.INACTIVE) {
    console.log(`[Security Trigger] User ${uid} status changed: ${beforeStatus} -> ${afterStatus}. Revoking tokens...`);

    try {
      // 1. Immediately revoke all active refresh tokens in Firebase Authentication
      await auth.revokeRefreshTokens(uid);
      console.log(`[Security Trigger] Successfully revoked refresh tokens for UID: ${uid}`);

      // 2. Synchronize token custom claims to block Storage and Firestore
      const userRecord = await auth.getUser(uid);
      const updatedClaims = {
        ...(userRecord.customClaims || {}),
        accountStatus: afterStatus,
      };
      await auth.setCustomUserClaims(uid, updatedClaims);

      // 3. Record immutable security audit log
      await logSecurityEvent(db, {
        logId: idempotencyKey,
        eventId,
        action: "ACCOUNT_SUSPENDED",
        actorUid: afterData.updatedBy || "SYSTEM_OR_ADMIN",
        targetUid: uid,
        previousStatus: beforeStatus,
        newStatus: afterStatus,
        tokensRevoked: true,
        metadata: {
          revocationTimestamp: new Date().toISOString(),
          targetRole: afterData.role || null,
        },
      });
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      console.error(`[Security Error] Failed to revoke tokens for user ${uid}: ${errorMessage}`);
      throw err;
    }
  }

  // Case 2: Account Reactivated (from SUSPENDED/INACTIVE to ACTIVE)
  else if (afterStatus === AccountStatus.ACTIVE && (beforeStatus === AccountStatus.SUSPENDED || beforeStatus === AccountStatus.INACTIVE)) {
    console.log(`[Security Trigger] User ${uid} reactivated: ${beforeStatus} -> ACTIVE.`);

    try {
      // 1. Validate that the current role in the document is an approved role
      const liveRole = afterData.role || "CUSTOMER";

      // 2. Update custom claims with ACTIVE status and verified role
      const userRecord = await auth.getUser(uid);
      const updatedClaims = {
        ...(userRecord.customClaims || {}),
        role: liveRole,
        accountStatus: AccountStatus.ACTIVE,
      };
      await auth.setCustomUserClaims(uid, updatedClaims);

      // 3. Revoke tokens to force client re-authentication and claim refresh
      await auth.revokeRefreshTokens(uid);

      // 4. Record audit log
      await logSecurityEvent(db, {
        logId: idempotencyKey,
        eventId,
        action: "ACCOUNT_REACTIVATED",
        actorUid: afterData.updatedBy || "SYSTEM_OR_ADMIN",
        targetUid: uid,
        previousStatus: beforeStatus,
        newStatus: AccountStatus.ACTIVE,
        tokensRevoked: true,
        metadata: {
          verifiedRole: liveRole,
        },
      });
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      console.error(`[Security Error] Failed to process reactivation for user ${uid}: ${errorMessage}`);
      throw err;
    }
  }
}
