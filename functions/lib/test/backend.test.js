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
/**
 * HBX SOLAR ONE — Trusted Backend Infrastructure Unit & Security Tests
 * Uses node:test to verify staff provisioning, token revocation, RBAC hierarchy, and idempotency.
 */
const node_test_1 = require("node:test");
const assert = __importStar(require("node:assert"));
const types_1 = require("../src/types");
const staffProvisioning_1 = require("../src/staffProvisioning");
const accountSuspension_1 = require("../src/accountSuspension");
const secureStorage_1 = require("../src/secureStorage");
// Mock Factory for Firebase Auth
function createMockAuth(initialUsers = {}) {
    const users = { ...initialUsers };
    const customClaimsMap = {};
    const revokedTokens = [];
    return {
        users,
        customClaimsMap,
        revokedTokens,
        authInstance: {
            getUser: async (uid) => {
                if (!users[uid]) {
                    throw new Error(`User ${uid} not found`);
                }
                return {
                    uid,
                    email: users[uid].email || `${uid}@example.com`,
                    displayName: users[uid].displayName || `User ${uid}`,
                    customClaims: customClaimsMap[uid] || {},
                };
            },
            setCustomUserClaims: async (uid, claims) => {
                customClaimsMap[uid] = claims;
            },
            revokeRefreshTokens: async (uid) => {
                revokedTokens.push(uid);
            },
        },
    };
}
// Mock Factory for Cloud Firestore
function createMockDb(initialDocs = {}) {
    const store = { ...initialDocs };
    return {
        store,
        dbInstance: {
            collection: (colName) => ({
                doc: (docId) => {
                    const path = `${colName}/${docId}`;
                    return {
                        path,
                        get: async () => ({
                            exists: !!store[path],
                            data: () => store[path],
                        }),
                        set: async (data) => {
                            store[path] = { ...data };
                        },
                        update: async (data) => {
                            if (!store[path])
                                throw new Error(`Document ${path} does not exist`);
                            store[path] = { ...store[path], ...data };
                        },
                    };
                },
                where: (field, op, val) => ({
                    limit: (n) => ({
                        get: async () => {
                            const matched = [];
                            for (const [p, data] of Object.entries(store)) {
                                if (p.startsWith(`${colName}/`)) {
                                    if (op === "==" && data[field] === val) {
                                        matched.push({ id: p.split("/")[1], data: () => data });
                                    }
                                }
                            }
                            return {
                                empty: matched.length === 0,
                                docs: matched.slice(0, n),
                            };
                        },
                    }),
                }),
            }),
        },
    };
}
// Mock Factory for Cloud Storage
function createMockStorage() {
    return {
        storageInstance: {
            bucket: () => ({
                file: (filePath) => ({
                    name: filePath,
                    getSignedUrl: async (cfg) => {
                        return [`https://storage.googleapis.com/signed-mock-url/${filePath}?token=test_v4`];
                    },
                }),
            }),
        },
    };
}
// ==========================================
// 1. Staff Provisioning Security Tests
// ==========================================
(0, node_test_1.test)("SEC-PROV-1: Unauthorized caller (no auth) cannot provision staff", async () => {
    const { authInstance } = createMockAuth();
    const { dbInstance } = createMockDb();
    const req = {
        data: { targetUid: "user_123", role: types_1.UserRole.BDE },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, staffProvisioning_1.handleStaffProvisioning)(req, authInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "unauthenticated");
        return true;
    });
});
(0, node_test_1.test)("SEC-PROV-2: Ordinary Customer CANNOT provision BDE", async () => {
    const { authInstance } = createMockAuth({
        cust_1: { email: "cust@gmail.com" },
        target_user: { email: "target@example.com" },
    });
    const { dbInstance } = createMockDb({
        "users/cust_1": { uid: "cust_1", role: "CUSTOMER", accountStatus: "ACTIVE" },
    });
    const req = {
        auth: { uid: "cust_1", token: { role: "CUSTOMER" } },
        data: { targetUid: "target_user", role: types_1.UserRole.BDE },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, staffProvisioning_1.handleStaffProvisioning)(req, authInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        return true;
    });
});
(0, node_test_1.test)("SEC-PROV-3: Staff BDE CANNOT provision ADMIN", async () => {
    const { authInstance } = createMockAuth({
        bde_1: { email: "bde@hbxsolar.com" },
        target_user: { email: "target@example.com" },
    });
    const { dbInstance } = createMockDb({
        "users/bde_1": { uid: "bde_1", role: "BDE", accountStatus: "ACTIVE" },
    });
    const req = {
        auth: { uid: "bde_1", token: { role: "BDE" } },
        data: { targetUid: "target_user", role: types_1.UserRole.ADMIN },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, staffProvisioning_1.handleStaffProvisioning)(req, authInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        return true;
    });
});
(0, node_test_1.test)("SEC-PROV-4: ADMIN CANNOT self-escalate or provision SUPER_ADMIN", async () => {
    const { authInstance } = createMockAuth({
        admin_pooja: { email: "pooja@hbxsolar.com" },
        target_emp: { email: "emp@hbxsolar.com" },
    });
    const { dbInstance } = createMockDb({
        "users/admin_pooja": { uid: "admin_pooja", role: "ADMIN", accountStatus: "ACTIVE" },
    });
    // Attempting to provision SUPER_ADMIN
    const req = {
        auth: { uid: "admin_pooja", token: { role: "ADMIN" } },
        data: { targetUid: "target_emp", role: types_1.UserRole.SUPER_ADMIN },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, staffProvisioning_1.handleStaffProvisioning)(req, authInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        return true;
    });
    // Attempting self-promotion
    const selfReq = {
        auth: { uid: "admin_pooja", token: { role: "ADMIN" } },
        data: { targetUid: "admin_pooja", role: types_1.UserRole.SUPER_ADMIN },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, staffProvisioning_1.handleStaffProvisioning)(selfReq, authInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        return true;
    });
});
(0, node_test_1.test)("SEC-PROV-5: Authorized SUPER_ADMIN CAN provision BDE and ADMIN successfully", async () => {
    const { authInstance, customClaimsMap } = createMockAuth({
        super_admin_1: { email: "super@hbxsolar.com" },
        rahul_bde: { email: "rahul@hbxsolar.com" },
    });
    const { dbInstance, store } = createMockDb({
        "users/super_admin_1": { uid: "super_admin_1", role: "SUPER_ADMIN", accountStatus: "ACTIVE" },
        "users/rahul_bde": { uid: "rahul_bde", role: "CUSTOMER", accountStatus: "ACTIVE" },
    });
    const req = {
        auth: { uid: "super_admin_1", token: { role: "SUPER_ADMIN" } },
        data: {
            targetUid: "rahul_bde",
            role: types_1.UserRole.BDE,
            employeeId: "HBX-AZM-101",
            territoryId: "Azamgarh",
        },
        rawRequest: {},
    };
    const response = await (0, staffProvisioning_1.handleStaffProvisioning)(req, authInstance, dbInstance);
    assert.strictEqual(response.success, true);
    assert.strictEqual(response.assignedRole, types_1.UserRole.BDE);
    // Verify Custom Claims assigned via Admin SDK
    assert.strictEqual(customClaimsMap["rahul_bde"].role, types_1.UserRole.BDE);
    assert.strictEqual(customClaimsMap["rahul_bde"].accountStatus, types_1.AccountStatus.ACTIVE);
    // Verify Firestore document synchronized
    assert.strictEqual(store["users/rahul_bde"].role, types_1.UserRole.BDE);
    assert.strictEqual(store["users/rahul_bde"].provisionedBy, "super_admin_1");
});
// ==========================================
// 2. Account Suspension & Token Revocation Tests
// ==========================================
(0, node_test_1.test)("SEC-REV-1: User status changed to SUSPENDED triggers refresh-token revocation", async () => {
    const { authInstance, revokedTokens, customClaimsMap } = createMockAuth({
        user_rogue: { email: "rogue@example.com" },
    });
    customClaimsMap["user_rogue"] = { role: "BDE", accountStatus: "ACTIVE" };
    const { dbInstance } = createMockDb();
    const mockEvent = {
        id: "evt_suspension_001",
        params: { uid: "user_rogue" },
        data: {
            before: { data: () => ({ accountStatus: "ACTIVE", role: "BDE" }) },
            after: { data: () => ({ accountStatus: "SUSPENDED", role: "BDE", updatedBy: "admin_pooja" }) },
        },
    };
    await (0, accountSuspension_1.handleUserAccountStatusUpdate)(mockEvent, authInstance, dbInstance);
    // Assert token revocation was called for this user
    assert.ok(revokedTokens.includes("user_rogue"), "User's refresh tokens must be revoked");
    // Assert custom claims were updated with SUSPENDED status
    assert.strictEqual(customClaimsMap["user_rogue"].accountStatus, "SUSPENDED");
});
(0, node_test_1.test)("SEC-REV-2: User status changed to INACTIVE triggers refresh-token revocation", async () => {
    const { authInstance, revokedTokens, customClaimsMap } = createMockAuth({
        user_resigned: { email: "resigned@example.com" },
    });
    customClaimsMap["user_resigned"] = { role: "BDE", accountStatus: "ACTIVE" };
    const { dbInstance } = createMockDb();
    const mockEvent = {
        id: "evt_inactive_002",
        params: { uid: "user_resigned" },
        data: {
            before: { data: () => ({ accountStatus: "ACTIVE", role: "BDE" }) },
            after: { data: () => ({ accountStatus: "INACTIVE", role: "BDE", updatedBy: "admin_pooja" }) },
        },
    };
    await (0, accountSuspension_1.handleUserAccountStatusUpdate)(mockEvent, authInstance, dbInstance);
    assert.ok(revokedTokens.includes("user_resigned"), "User's refresh tokens must be revoked");
    assert.strictEqual(customClaimsMap["user_resigned"].accountStatus, "INACTIVE");
});
(0, node_test_1.test)("SEC-REV-3: Duplicate suspension events are handled idempotently", async () => {
    const { authInstance, revokedTokens } = createMockAuth({
        user_dup: { email: "dup@example.com" },
    });
    const { dbInstance } = createMockDb();
    const mockEvent = {
        id: "evt_dup_003",
        params: { uid: "user_dup" },
        data: {
            before: { data: () => ({ accountStatus: "ACTIVE", role: "BDE" }) },
            after: { data: () => ({ accountStatus: "SUSPENDED", role: "BDE", updatedBy: "admin" }) },
        },
    };
    // First invocation
    await (0, accountSuspension_1.handleUserAccountStatusUpdate)(mockEvent, authInstance, dbInstance);
    assert.strictEqual(revokedTokens.length, 1);
    // Second duplicate invocation
    await (0, accountSuspension_1.handleUserAccountStatusUpdate)(mockEvent, authInstance, dbInstance);
    // Must NOT trigger another revocation because idempotency key exists
    assert.strictEqual(revokedTokens.length, 1);
});
(0, node_test_1.test)("SEC-REV-4: Reactivation validates role and updates claims safely", async () => {
    const { authInstance, revokedTokens, customClaimsMap } = createMockAuth({
        user_reinstated: { email: "reinstated@example.com" },
    });
    customClaimsMap["user_reinstated"] = { role: "BDE", accountStatus: "SUSPENDED" };
    const { dbInstance } = createMockDb();
    const mockEvent = {
        id: "evt_reactivate_004",
        params: { uid: "user_reinstated" },
        data: {
            before: { data: () => ({ accountStatus: "SUSPENDED", role: "BDE" }) },
            after: { data: () => ({ accountStatus: "ACTIVE", role: "BDE", updatedBy: "super_admin" }) },
        },
    };
    await (0, accountSuspension_1.handleUserAccountStatusUpdate)(mockEvent, authInstance, dbInstance);
    assert.strictEqual(customClaimsMap["user_reinstated"].accountStatus, "ACTIVE");
    assert.strictEqual(customClaimsMap["user_reinstated"].role, "BDE");
    // Tokens revoked to force reauthentication with clean active token
    assert.ok(revokedTokens.includes("user_reinstated"));
});
// ==========================================
// 3. Storage Stale-Token Mitigation Tests
// ==========================================
(0, node_test_1.test)("SEC-STORE-1: Suspended user CANNOT obtain Signed Storage URL", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/suspended_caller": {
            uid: "suspended_caller",
            role: "CUSTOMER",
            accountStatus: "SUSPENDED", // Suspended in Firestore!
        },
    });
    const req = {
        auth: { uid: "suspended_caller", token: { role: "CUSTOMER" } },
        data: { storagePath: "customers/suspended_caller/kyc/pan.jpg" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /Account is suspended/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-2: Active user CAN obtain Signed Storage URL for owned document", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/active_alice": {
            uid: "active_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
        "documents/doc_alice_pan": {
            documentId: "doc_alice_pan",
            storagePath: "customers/active_alice/kyc/pan.jpg",
            uploadedByUid: "active_alice",
            parentEntityId: "active_alice",
            documentCategory: "KYC",
            isDeleted: false,
        },
    });
    const req = {
        auth: { uid: "active_alice", token: { role: "CUSTOMER" } },
        data: { storagePath: "customers/active_alice/kyc/pan.jpg" },
        rawRequest: {},
    };
    const response = await (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance);
    assert.ok(response.downloadUrl.includes("https://storage.googleapis.com"));
    assert.strictEqual(response.expiresInSeconds, 300);
});
(0, node_test_1.test)("SEC-STORE-3: Arbitrary Storage path URL request without matching Firestore document metadata is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/active_alice": {
            uid: "active_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
    });
    const req = {
        auth: { uid: "active_alice", token: { role: "CUSTOMER" } },
        data: { storagePath: "customers/bob/untracked_secret_file.pdf" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /Arbitrary Storage path rejected/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-4: Cross-customer access attempt (Customer Alice requesting Customer Bob's document) is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/cust_alice": {
            uid: "cust_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
        "documents/doc_bob_pan": {
            documentId: "doc_bob_pan",
            storagePath: "customers/cust_bob/kyc/pan.jpg",
            uploadedByUid: "cust_bob",
            parentEntityId: "cust_bob",
            documentCategory: "KYC",
            isDeleted: false,
        },
    });
    const req = {
        auth: { uid: "cust_alice", token: { role: "CUSTOMER" } },
        data: { documentId: "doc_bob_pan" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /is not authorized to access document/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-5: Request for soft-deleted document (isDeleted: true) is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/active_alice": {
            uid: "active_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
        "documents/doc_alice_deleted": {
            documentId: "doc_alice_deleted",
            storagePath: "customers/active_alice/kyc/old_aadhaar.jpg",
            uploadedByUid: "active_alice",
            isDeleted: true, // Soft deleted!
        },
    });
    const req = {
        auth: { uid: "active_alice", token: { role: "CUSTOMER" } },
        data: { documentId: "doc_alice_deleted" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /Document has been deleted/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-6: Path traversal attempt ('customers/alice/../bob/kyc/pan.jpg') is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/active_alice": {
            uid: "active_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
    });
    const req = {
        auth: { uid: "active_alice", token: { role: "CUSTOMER" } },
        data: { storagePath: "customers/active_alice/../../secret/pan.jpg" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "invalid-argument");
        assert.match(err.message, /Path traversal attempt detected/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-7: Engineer attempting to access customer KYC/financial document is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/eng_rajesh": {
            uid: "eng_rajesh",
            role: "INSTALLATION_ENGINEER",
            accountStatus: "ACTIVE",
        },
        "documents/doc_cust_kyc": {
            documentId: "doc_cust_kyc",
            storagePath: "customers/cust_bob/kyc/aadhaar.pdf",
            uploadedByUid: "cust_bob",
            documentCategory: "KYC",
            isDeleted: false,
        },
    });
    const req = {
        auth: { uid: "eng_rajesh", token: { role: "INSTALLATION_ENGINEER" } },
        data: { documentId: "doc_cust_kyc" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /is not authorized to access document/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-8: Unassigned BDE attempting to access another BDE's customer is REJECTED", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance } = createMockDb({
        "users/bde_vikram": {
            uid: "bde_vikram",
            role: "BDE",
            accountStatus: "ACTIVE",
        },
        "documents/doc_rahul_cust": {
            documentId: "doc_rahul_cust",
            storagePath: "customers/cust_sita/kyc/pan.jpg",
            uploadedByUid: "cust_sita",
            assignedToBdeUid: "bde_rahul", // Assigned to Rahul, NOT Vikram
            documentCategory: "KYC",
            isDeleted: false,
        },
    });
    const req = {
        auth: { uid: "bde_vikram", token: { role: "BDE" } },
        data: { documentId: "doc_rahul_cust" },
        rawRequest: {},
    };
    await assert.rejects(async () => (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance), (err) => {
        assert.strictEqual(err.code, "permission-denied");
        assert.match(err.message, /is not authorized to access document/);
        return true;
    });
});
(0, node_test_1.test)("SEC-STORE-9: Audit log creation excludes signed URL token and records actor metadata", async () => {
    const { storageInstance } = createMockStorage();
    const { dbInstance, store } = createMockDb({
        "users/active_alice": {
            uid: "active_alice",
            role: "CUSTOMER",
            accountStatus: "ACTIVE",
        },
        "documents/doc_alice_bill": {
            documentId: "doc_alice_bill",
            storagePath: "customers/active_alice/bills/electricity.pdf",
            uploadedByUid: "active_alice",
            parentEntityId: "active_alice",
            documentCategory: "ELECTRICITY_BILL",
            isDeleted: false,
        },
    });
    const req = {
        auth: { uid: "active_alice", token: { role: "CUSTOMER" } },
        data: { documentId: "doc_alice_bill" },
        rawRequest: {},
    };
    const res = await (0, secureStorage_1.handleSecureStorageUrl)(req, storageInstance, dbInstance);
    assert.ok(res.downloadUrl.includes("token=test_v4"));
    // Check audit log stored
    const auditEntries = Object.entries(store).filter(([k]) => k.startsWith("document_audit_logs/"));
    assert.strictEqual(auditEntries.length, 1);
    const [, logData] = auditEntries[0];
    assert.strictEqual(logData.action, "SECURE_URL_GENERATED");
    assert.strictEqual(logData.actorUid, "active_alice");
    assert.strictEqual(logData.actorRole, "CUSTOMER");
    assert.strictEqual(logData.metadata.storagePath, "customers/active_alice/bills/electricity.pdf");
    // CRITICAL: Ensure signedUrl is NOT stored in the log data
    assert.strictEqual(logData.metadata.signedUrl, undefined);
    assert.strictEqual(logData.signedUrl, undefined);
});
//# sourceMappingURL=backend.test.js.map