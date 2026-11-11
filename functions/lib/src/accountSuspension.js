"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleUserAccountStatusUpdate = handleUserAccountStatusUpdate;
const types_1 = require("./types");
const auditLogger_1 = require("./auditLogger");
async function handleUserAccountStatusUpdate(event, auth, db) {
    if (!event.data) {
        return;
    }
    const { before, after } = event.data;
    const uid = event.params.uid;
    const eventId = event.id;
    const beforeData = before.data() || {};
    const afterData = after.data() || {};
    const beforeStatus = beforeData.accountStatus || types_1.AccountStatus.ACTIVE;
    const afterStatus = afterData.accountStatus || types_1.AccountStatus.ACTIVE;
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
    if (afterStatus === types_1.AccountStatus.SUSPENDED || afterStatus === types_1.AccountStatus.INACTIVE) {
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
            await (0, auditLogger_1.logSecurityEvent)(db, {
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
        }
        catch (err) {
            const errorMessage = err instanceof Error ? err.message : String(err);
            console.error(`[Security Error] Failed to revoke tokens for user ${uid}: ${errorMessage}`);
            throw err;
        }
    }
    // Case 2: Account Reactivated (from SUSPENDED/INACTIVE to ACTIVE)
    else if (afterStatus === types_1.AccountStatus.ACTIVE && (beforeStatus === types_1.AccountStatus.SUSPENDED || beforeStatus === types_1.AccountStatus.INACTIVE)) {
        console.log(`[Security Trigger] User ${uid} reactivated: ${beforeStatus} -> ACTIVE.`);
        try {
            // 1. Validate that the current role in the document is an approved role
            const liveRole = afterData.role || "CUSTOMER";
            // 2. Update custom claims with ACTIVE status and verified role
            const userRecord = await auth.getUser(uid);
            const updatedClaims = {
                ...(userRecord.customClaims || {}),
                role: liveRole,
                accountStatus: types_1.AccountStatus.ACTIVE,
            };
            await auth.setCustomUserClaims(uid, updatedClaims);
            // 3. Revoke tokens to force client re-authentication and claim refresh
            await auth.revokeRefreshTokens(uid);
            // 4. Record audit log
            await (0, auditLogger_1.logSecurityEvent)(db, {
                logId: idempotencyKey,
                eventId,
                action: "ACCOUNT_REACTIVATED",
                actorUid: afterData.updatedBy || "SYSTEM_OR_ADMIN",
                targetUid: uid,
                previousStatus: beforeStatus,
                newStatus: types_1.AccountStatus.ACTIVE,
                tokensRevoked: true,
                metadata: {
                    verifiedRole: liveRole,
                },
            });
        }
        catch (err) {
            const errorMessage = err instanceof Error ? err.message : String(err);
            console.error(`[Security Error] Failed to process reactivation for user ${uid}: ${errorMessage}`);
            throw err;
        }
    }
}
//# sourceMappingURL=accountSuspension.js.map