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
exports.getSecureStorageUrl = exports.onUserAccountStatusChanged = exports.provisionStaff = void 0;
/**
 * HBX SOLAR ONE — Enterprise Cloud Functions Entry Point
 */
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
const firestore_1 = require("firebase-functions/v2/firestore");
const staffProvisioning_1 = require("./staffProvisioning");
const accountSuspension_1 = require("./accountSuspension");
const secureStorage_1 = require("./secureStorage");
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
exports.provisionStaff = (0, https_1.onCall)({
    cors: false, // Disallow arbitrary browser cross-origin calls
    enforceAppCheck: false, // Set to true once Play Integrity / App Check is registered
}, async (request) => {
    return (0, staffProvisioning_1.handleStaffProvisioning)(request, auth, db);
});
/**
 * 2. Firestore Trigger: User Account Suspension & Token Revocation
 * Immediately revokes refresh tokens and syncs custom claims when accountStatus changes.
 */
exports.onUserAccountStatusChanged = (0, firestore_1.onDocumentUpdated)({
    document: "users/{uid}",
    retry: true,
}, async (event) => {
    return (0, accountSuspension_1.handleUserAccountStatusUpdate)(event, auth, db);
});
/**
 * 3. Callable Function: Storage Stale-Token Mitigation & Secure URL Generator
 * Issues short-lived v4 signed URLs after validating live accountStatus from Firestore.
 */
exports.getSecureStorageUrl = (0, https_1.onCall)({
    cors: false,
}, async (request) => {
    return (0, secureStorage_1.handleSecureStorageUrl)(request, storage, db);
});
//# sourceMappingURL=index.js.map