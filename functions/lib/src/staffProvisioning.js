"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleStaffProvisioning = handleStaffProvisioning;
/**
 * HBX SOLAR ONE — Trusted Staff Provisioning Workflow
 * Assigns custom claims via Firebase Admin SDK with strict RBAC hierarchy.
 */
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
const types_1 = require("./types");
const auditLogger_1 = require("./auditLogger");
const ALLOWED_STAFF_ROLES = [
    types_1.UserRole.SUPER_ADMIN,
    types_1.UserRole.ADMIN,
    types_1.UserRole.OPERATIONS_MANAGER,
    types_1.UserRole.SALES_MANAGER,
    types_1.UserRole.TEAM_LEADER,
    types_1.UserRole.BDE,
    types_1.UserRole.INSTALLATION_ENGINEER,
    types_1.UserRole.SERVICE_ENGINEER,
    types_1.UserRole.PARTNER,
    types_1.UserRole.CUSTOMER,
];
async function handleStaffProvisioning(request, auth, db) {
    // 1. Authentication Check
    if (!request.auth || !request.auth.uid) {
        throw new https_1.HttpsError("unauthenticated", "Authentication is required to provision staff members.");
    }
    const callerUid = request.auth.uid;
    const callerClaims = request.auth.token || {};
    const callerRole = callerClaims.role || "";
    // 2. Caller Authorization Check (Must be SUPER_ADMIN or ADMIN)
    if (callerRole !== types_1.UserRole.SUPER_ADMIN && callerRole !== types_1.UserRole.ADMIN) {
        // Secondary check: verify against live Firestore user record
        const callerDoc = await db.collection("users").doc(callerUid).get();
        const liveCallerRole = callerDoc.data()?.role || "";
        if (liveCallerRole !== types_1.UserRole.SUPER_ADMIN && liveCallerRole !== types_1.UserRole.ADMIN) {
            throw new https_1.HttpsError("permission-denied", "Access denied. Only authorized administrators (SUPER_ADMIN or ADMIN) may provision staff.");
        }
    }
    // 3. Input Validation
    const { targetUid, role, employeeId, territoryId } = request.data || {};
    if (!targetUid || typeof targetUid !== "string" || targetUid.trim().length === 0) {
        throw new https_1.HttpsError("invalid-argument", "Valid target UID is required.");
    }
    if (!role || !ALLOWED_STAFF_ROLES.includes(role)) {
        throw new https_1.HttpsError("invalid-argument", `Invalid role requested. Allowed roles: ${ALLOWED_STAFF_ROLES.join(", ")}`);
    }
    // 4. Anti-Self-Escalation Check
    if (callerUid === targetUid) {
        throw new https_1.HttpsError("permission-denied", "Self-promotion is strictly prohibited. An administrator cannot change their own role.");
    }
    // 5. Hierarchy Enforcement
    // ADMIN cannot provision another ADMIN or SUPER_ADMIN (Only SUPER_ADMIN can do this)
    const effectiveCallerRole = (callerRole === types_1.UserRole.SUPER_ADMIN) ? types_1.UserRole.SUPER_ADMIN : types_1.UserRole.ADMIN;
    if (effectiveCallerRole === types_1.UserRole.ADMIN && (role === types_1.UserRole.SUPER_ADMIN || role === types_1.UserRole.ADMIN)) {
        throw new https_1.HttpsError("permission-denied", "Administrators cannot provision ADMIN or SUPER_ADMIN roles. Only SUPER_ADMIN may assign administrative tiers.");
    }
    // 6. Verify Target User Exists in Firebase Auth
    let targetUserRecord;
    try {
        targetUserRecord = await auth.getUser(targetUid);
    }
    catch (error) {
        throw new https_1.HttpsError("not-found", `Target user with UID ${targetUid} was not found in Firebase Authentication.`);
    }
    // 7. Verify / Resolve Target Firestore Profile
    const targetDocRef = db.collection("users").doc(targetUid);
    const targetDocSnapshot = await targetDocRef.get();
    const currentProfileData = targetDocSnapshot.data() || {};
    const currentRole = currentProfileData.role || "CUSTOMER";
    const currentStatus = currentProfileData.accountStatus || types_1.AccountStatus.ACTIVE;
    // 8. Assign Cryptographic Custom Claims via Firebase Admin SDK
    const updatedCustomClaims = {
        ...(targetUserRecord.customClaims || {}),
        role: role,
        accountStatus: currentStatus,
    };
    await auth.setCustomUserClaims(targetUid, updatedCustomClaims);
    // 9. Synchronize Profile in Cloud Firestore
    const updatePayload = {
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
    }
    else {
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
    await (0, auditLogger_1.logSecurityEvent)(db, {
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
//# sourceMappingURL=staffProvisioning.js.map