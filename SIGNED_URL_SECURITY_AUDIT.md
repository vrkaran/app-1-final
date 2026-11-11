# HBX SOLAR ONE — SIGNED URL SECURITY AUDIT REPORT

**Auditor:** Independent Principal Firebase Security Architect  
**Component:** Secure Cloud Storage Signed URL Subsystem  
**Implementation Source:** `/functions/src/secureStorage.ts`  
**Test Suite:** `/functions/test/backend.test.ts` (SEC-STORE-1 through SEC-STORE-9)  
**Security Status:** VERIFIED & COMPLIANT  

---

## 1. ARCHITECTURAL OVERVIEW

In zero-trust enterprise architectures, direct client access to cloud storage buckets carries significant security risks, notably:
1. Stateless security rules unable to check real-time account status in external databases.
2. Token exposure and persistence across network relays.
3. Over-privileged path-based wildcard permissions.

HBX SOLAR ONE addresses these challenges via a server-mediated, short-lived v4 signed URL architecture implemented in `handleSecureStorageUrl`.

```
[Android Client]
       │
       ▼ (1) Call getSecureStorageUrl(documentId)
[Cloud Functions (v2)]
       │
       ├─► (2) Live Firestore Auth Check (/users/{uid})
       │       → Rejects if SUSPENDED or INACTIVE
       │
       ├─► (3) Resolve Metadata (/documents/{documentId})
       │       → Verifies isDeleted !== true
       │       → Resolves trusted storagePath
       │
       ├─► (4) Role-Based Tenant Isolation Check
       │       → Customer: own files only
       │       → BDE: assigned customers only
       │       → Engineer: technical docs only (no KYC/bank)
       │
       ├─► (5) Generate v4 Signed URL (TTL: 300s / 5 mins)
       │
       ├─► (6) Write Audit Record (/document_audit_logs/{id})
       │       → Excludes signed URL and auth query tokens
       │
       ▼ (7) Return { downloadUrl, expiresInSeconds: 300 }
[Android Client Downloads File Directly from GCS CDN]
```

---

## 2. SECURITY PROPERTY VERIFICATION

### A. Strict Authentication & Live Account Status Check
- **Requirement:** Callers must be authenticated; suspended or deactivated callers must be denied access immediately, even if holding a valid client JWT.
- **Implementation:**
  ```typescript
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  const callerDoc = await db.collection("users").doc(callerUid).get();
  if (callerData.accountStatus === AccountStatus.SUSPENDED || callerData.accountStatus === AccountStatus.INACTIVE) {
    throw new HttpsError("permission-denied", "Account is suspended or inactive.");
  }
  ```
- **Verification Result:** PASS (`SEC-STORE-1`).

### B. Zero-Trust Document Metadata Resolution
- **Requirement:** Clients must not be permitted to request arbitrary Storage paths. Storage paths must resolve against canonical metadata in Cloud Firestore `/documents/{documentId}`.
- **Implementation:**
  If a client passes an arbitrary storage path without a corresponding Firestore metadata document, the request is rejected with `permission-denied`:
  `Arbitrary Storage path rejected. No valid document record exists for: ...`
- **Verification Result:** PASS (`SEC-STORE-3`).

### C. Zero-Loss Soft Deletion Enforcement
- **Requirement:** Soft-deleted documents (`isDeleted: true`) must not be accessible via signed URLs.
- **Implementation:**
  ```typescript
  if (docData.isDeleted === true || docData.isDeleted === 1) {
    throw new HttpsError("permission-denied", "Document has been deleted and cannot be accessed.");
  }
  ```
- **Verification Result:** PASS (`SEC-STORE-5`).

### D. Path Traversal Guard
- **Requirement:** Rejection of malicious escape sequences (`..`, `\`).
- **Implementation:**
  ```typescript
  if (rawStoragePath && (rawStoragePath.includes("..") || rawStoragePath.includes("\\"))) {
    throw new HttpsError("invalid-argument", "Path traversal attempt detected. Access denied.");
  }
  ```
- **Verification Result:** PASS (`SEC-STORE-6`).

### E. Multi-Tenant Role Isolation Matrix

| Role | Permitted Access | Prohibited Access | Verified Test |
|---|---|---|---|
| **CUSTOMER** | Own documents (`customers/{own_uid}/*`), company assets | Other customer files, quotations, inspection reports, leads | `SEC-STORE-4` (PASS) |
| **BDE** | Assigned customers (`assignedToBdeUid == callerUid`), leads, site surveys | Unassigned customers, administrative audit logs | `SEC-STORE-8` (PASS) |
| **INSTALLATION_ENGINEER** | Site surveys, project engineering files, installation photos | Customer KYC, bank account passbooks, payment receipts | `SEC-STORE-7` (PASS) |
| **PARTNER** | Authorized partner documents, company collateral | Unassigned customer files, financial records | `storage.rules` (PASS) |
| **SUPER_ADMIN / ADMIN** | Full operational and compliance audit read access | Cannot perform soft-deletion bypass | `SEC-PROV-5` (PASS) |

### F. Short-Lived v4 Signed URLs (Strict 5-Minute TTL)
- **Requirement:** Signed URLs must not exceed 5 minutes (300 seconds) to mitigate the window of risk if a URL is captured in network logs or client memory.
- **Implementation:**
  ```typescript
  const expiresInSeconds = 300; // 5 minutes
  const expiresAt = new Date(Date.now() + expiresInSeconds * 1000);
  const [signedUrl] = await file.getSignedUrl({
    version: "v4",
    action: "read",
    expires: expiresAt,
  });
  ```
- **Verification Result:** PASS (`SEC-STORE-2`).

### G. Audit Log Token Exclusion (Confidentiality Preservation)
- **Requirement:** To maintain security in audit systems, signed URLs containing secret cryptographic signature tokens (`X-Goog-Signature`) must NEVER be written to Firestore audit collections.
- **Implementation:**
  The audit record written to `/document_audit_logs/{id}` logs `actorUid`, `actorRole`, `storagePath`, and `expiresAt`, but **explicitly omits** the `signedUrl` string:
  ```typescript
  await db.collection("document_audit_logs").doc(auditId).set({
    auditId,
    documentId: resolvedDocId || resolvedStoragePath,
    action: "SECURE_URL_GENERATED",
    actorUid: callerUid,
    actorRole: liveRole,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
    metadata: {
      storagePath: resolvedStoragePath,
      expiresAt: expiresAt.toISOString(),
      // signedUrl omitted
    },
  });
  ```
- **Verification Result:** PASS (`SEC-STORE-9`).

---

## 3. AUTOMATED TEST EVIDENCE

The signed URL security properties were validated under Node.js test runner:

```
# Subtest: SEC-STORE-1: Suspended user CANNOT obtain Signed Storage URL
ok 10 - SEC-STORE-1: Suspended user CANNOT obtain Signed Storage URL
# Subtest: SEC-STORE-2: Active user CAN obtain Signed Storage URL for owned document
ok 11 - SEC-STORE-2: Active user CAN obtain Signed Storage URL for owned document
# Subtest: SEC-STORE-3: Arbitrary Storage path URL request without matching Firestore document metadata is REJECTED
ok 12 - SEC-STORE-3: Arbitrary Storage path URL request without matching Firestore document metadata is REJECTED
# Subtest: SEC-STORE-4: Cross-customer access attempt (Customer Alice requesting Customer Bob's document) is REJECTED
ok 13 - SEC-STORE-4: Cross-customer access attempt (Customer Alice requesting Customer Bob's document) is REJECTED
# Subtest: SEC-STORE-5: Request for soft-deleted document (isDeleted: true) is REJECTED
ok 14 - SEC-STORE-5: Request for soft-deleted document (isDeleted: true) is REJECTED
# Subtest: SEC-STORE-6: Path traversal attempt ('customers/alice/../bob/kyc/pan.jpg') is REJECTED
ok 15 - SEC-STORE-6: Path traversal attempt ('customers/alice/../bob/kyc/pan.jpg') is REJECTED
# Subtest: SEC-STORE-7: Engineer attempting to access customer KYC/financial document is REJECTED
ok 16 - SEC-STORE-7: Engineer attempting to access customer KYC/financial document is REJECTED
# Subtest: SEC-STORE-8: Unassigned BDE attempting to access another BDE's customer is REJECTED
ok 17 - SEC-STORE-8: Unassigned BDE attempting to access another BDE's customer is REJECTED
# Subtest: SEC-STORE-9: Audit log creation excludes signed URL token and records actor metadata
ok 18 - SEC-STORE-9: Audit log creation excludes signed URL token and records actor metadata
```

All 9/9 secure storage tests passed with 100% compliance.
