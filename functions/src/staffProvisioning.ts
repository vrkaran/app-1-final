/**
 * HBX SOLAR ONE — Trusted Staff Provisioning Workflow
 * Assigns custom claims via Firebase Admin SDK with strict RBAC hierarchy.
 */
import * as admin from "firebase-admin";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { UserRole, AccountStatus, ProvisionStaffRequest, ProvisionStaffResponse } from "./types";
import { logSecurityEvent } from "./auditLogger";

const ALLOWED_STAFF_ROLES: UserRole[] = [
  UserRole.SUPER_ADMIN,
  UserRole.ADMIN,
  UserRole.OPERATIONS_MANAGER,
  UserRole.SALES_MANAGER,
  UserRole.TEAM_LEADER,
  UserRole.BDE,
  UserRole.INSTALLATION_ENGINEER,
  UserRole.SERVICE_ENGINEER,
  UserRole.PARTNER,
  UserRole.CUSTOMER,
];

export async function handleStaffProvisioning(
  request: CallableRequest<ProvisionStaffRequest>,
  auth: admin.auth.Auth,
  db: admin.firestore.Firestore
): Promise<ProvisionStaffResponse> {
  // 1. Authentication Check
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required to provision staff members.");
  }

  const callerUid = request.auth.uid;
  const callerClaims = request.auth.token || {};
  const callerRole = (callerClaims.role as string) || "";

  // 2. Caller Authorization Check (Must be SUPER_ADMIN or ADMIN)
  if (callerRole !== UserRole.SUPER_ADMIN && callerRole !== UserRole.ADMIN) {
    // Secondary check: verify against live Firestore user record
    const callerDoc = await db.collection("users").doc(callerUid).get();
    const liveCallerRole = callerDoc.data()?.role || "";
    if (liveCallerRole !== UserRole.SUPER_ADMIN && liveCallerRole !== UserRole.ADMIN) {
      throw new HttpsError(
        "permission-denied",
        "Access denied. Only authorized administrators (SUPER_ADMIN or ADMIN) may provision staff."
      );
    }
  }

  // 3. Input Validation
  const { targetUid, role, employeeId, territoryId } = request.data || {};

  if (!targetUid || typeof targetUid !== "string" || targetUid.trim().length === 0) {
    throw new HttpsError("invalid-argument", "Valid target UID is required.");
  }

  if (!role || !ALLOWED_STAFF_ROLES.includes(role)) {
    throw new HttpsError("invalid-argument", `Invalid role requested. Allowed roles: ${ALLOWED_STAFF_ROLES.join(", ")}`);
  }

  // 4. Anti-Self-Escalation Check
  if (callerUid === targetUid) {
    throw new HttpsError("permission-denied", "Self-promotion is strictly prohibited. An administrator cannot change their own role.");
  }

  // 5. Hierarchy Enforcement
  // ADMIN cannot provision another ADMIN or SUPER_ADMIN (Only SUPER_ADMIN can do this)
  const effectiveCallerRole = (callerRole === UserRole.SUPER_ADMIN) ? UserRole.SUPER_ADMIN : UserRole.ADMIN;
  if (effectiveCallerRole === UserRole.ADMIN && (role === UserRole.SUPER_ADMIN || role === UserRole.ADMIN)) {
    throw new HttpsError(
      "permission-denied",
      "Administrators cannot provision ADMIN or SUPER_ADMIN roles. Only SUPER_ADMIN may assign administrative tiers."
    );
  }

  // 6. Verify Target User Exists in Firebase Auth
  let targetUserRecord: admin.auth.UserRecord;
  try {
    targetUserRecord = await auth.getUser(targetUid);
  } catch (error) {
    throw new HttpsError("not-found", `Target user with UID ${targetUid} was not found in Firebase Authentication.`);
  }

  // 7. Verify / Resolve Target Firestore Profile
  const targetDocRef = db.collection("users").doc(targetUid);
  const targetDocSnapshot = await targetDocRef.get();
  const currentProfileData = targetDocSnapshot.data() || {};
  const currentRole = currentProfileData.role || "CUSTOMER";
  const currentStatus = (currentProfileData.accountStatus as AccountStatus) || AccountStatus.ACTIVE;

  // 8. Assign Cryptographic Custom Claims via Firebase Admin SDK
  const updatedCustomClaims = {
    ...(targetUserRecord.customClaims || {}),
    role: role,
    accountStatus: currentStatus,
  };

  await auth.setCustomUserClaims(targetUid, updatedCustomClaims);

  // 9. Synchronize Profile in Cloud Firestore
  const updatePayload: Record<string, unknown> = {
    role: role,
    accountStatus: currentStatus,
    provisionedBy: callerUid,
    provisionedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (employeeId && typeof employeeId === "string") {
    updatePayload.employeeId = employeeId.trim();
  }
  if (territoryId && typeof territoryId === "string") {
    updatePayload.territoryId = territoryId.trim();
  }

  if (targetDocSnapshot.exists) {
    await targetDocRef.update(updatePayload);
  } else {
    // If user profile doc wasn't created yet, initialize it
    await targetDocRef.set({
      uid: targetUid,
      fullName: targetUserRecord.displayName || `Staff ${targetUid.slice(0, 5)}`,
      email: targetUserRecord.email || "",
      phone: targetUserRecord.phoneNumber || "",
      employeeId: employeeId || `HBX-${targetUid.slice(0, 4).toUpperCase()}`,
      territoryId: territoryId || "Azamgarh",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      ...updatePayload,
    });
  }

  // 10. Record Immutable Event in Security Audit Logs
  await logSecurityEvent(db, {
    action: "STAFF_PROVISIONED",
    actorUid: callerUid,
    actorRole: effectiveCallerRole,
    targetUid,
    previousRole: currentRole,
    assignedRole: role,
    metadata: {
      employeeId: employeeId || null,
      territoryId: territoryId || null,
      targetEmail: targetUserRecord.email || null,
    },
  });

  return {
    success: true,
    targetUid,
    assignedRole: role,
    provisionedAt: new Date().toISOString(),
    message: `User ${targetUid} successfully provisioned as ${role} by ${callerUid}.`,
  };
}
