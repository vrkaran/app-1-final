/**
 * HBX SOLAR ONE — Enterprise Cloud Functions Entry Point
 */
import * as admin from "firebase-admin";
import { onCall } from "firebase-functions/v2/https";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { handleStaffProvisioning } from "./staffProvisioning";
import { handleUserAccountStatusUpdate } from "./accountSuspension";
import { handleSecureStorageUrl } from "./secureStorage";
import { ProvisionStaffRequest } from "./types";

// Initialize Firebase Admin SDK (uses Application Default Credentials in Cloud Functions environment)
if (admin.apps.length === 0) {
  admin.initializeApp();
}

const auth = admin.auth();
const db = admin.firestore();
const storage = admin.storage();

/**
 * 1. Callable Function: Trusted Staff Provisioning
 * Assigns custom claims via Firebase Admin SDK with strict RBAC hierarchy checks.
 */
export const provisionStaff = onCall<ProvisionStaffRequest>(
  {
    cors: false, // Disallow arbitrary browser cross-origin calls
    enforceAppCheck: false, // Set to true once Play Integrity / App Check is registered
  },
  async (request) => {
    return handleStaffProvisioning(request, auth, db);
  }
);

/**
 * 2. Firestore Trigger: User Account Suspension & Token Revocation
 * Immediately revokes refresh tokens and syncs custom claims when accountStatus changes.
 */
export const onUserAccountStatusChanged = onDocumentUpdated(
  {
    document: "users/{uid}",
    retry: true,
  },
  async (event) => {
    return handleUserAccountStatusUpdate(event, auth, db);
  }
);

/**
 * 3. Callable Function: Storage Stale-Token Mitigation & Secure URL Generator
 * Issues short-lived v4 signed URLs after validating live accountStatus from Firestore.
 */
export const getSecureStorageUrl = onCall(
  {
    cors: false,
  },
  async (request) => {
    return handleSecureStorageUrl(request, storage, db);
  }
);
