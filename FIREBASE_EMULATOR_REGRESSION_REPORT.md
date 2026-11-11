# HBX SOLAR ONE — FIREBASE EMULATOR REGRESSION TEST REPORT

**Test Runner Framework:** Node.js Native Test Runner (`node:test`) + Robolectric / JUnit 4  
**Target Backend:** Firebase Local Emulators (Auth :9099, Firestore :8085, Storage :9199, Functions :5001)  
**Total Automated Tests Executed:** 94  
**Total Tests Passed:** 94 (100% Pass Rate)  
**Total Tests Failed:** 0  
**Date:** November 11, 2026  

---

## 1. COMPREHENSIVE REGRESSION MATRIX

```
┌──────────────────────────────────────┬───────┬────────┬────────┬──────────┐
│ Test Domain                          │ Total │ Passed │ Failed │ Pass Rate│
├──────────────────────────────────────┼───────┼────────┼────────┼──────────┤
│ Cloud Functions & Admin SDK Security │ 18    │ 18     │ 0      │ 100%     │
│ Cloud Firestore Security Rules       │ 27    │ 27     │ 0      │ 100%     │
│ Cloud Storage Security Rules         │ 24    │ 24     │ 0      │ 100%     │
│ Android JVM Data Layer & Offline Sync│ 25    │ 25     │ 0      │ 100%     │
├──────────────────────────────────────┼───────┼────────┼────────┼──────────┤
│ TOTAL AUTOMATED REGRESSION SUITE     │ 94    │ 94     │ 0      │ 100%     │
└──────────────────────────────────────┴───────┴────────┴────────┴──────────┘
```

---

## 2. SUITE 1: CLOUD FUNCTIONS UNIT & SECURITY TESTS (18/18 PASSED)

**Execution:** `npm --prefix functions test`  
**Duration:** 450.8ms  

| Test Identifier | Category | Security Assertion | Result |
|---|---|---|---|
| `SEC-PROV-1` | Staff Provisioning | Unauthenticated caller rejected with `unauthenticated` | **PASSED** |
| `SEC-PROV-2` | Staff Provisioning | Ordinary Customer CANNOT provision BDE | **PASSED** |
| `SEC-PROV-3` | Staff Provisioning | Staff BDE CANNOT provision ADMIN | **PASSED** |
| `SEC-PROV-4` | Staff Provisioning | ADMIN CANNOT self-escalate or provision SUPER_ADMIN | **PASSED** |
| `SEC-PROV-5` | Staff Provisioning | Authorized SUPER_ADMIN provisions BDE with Custom Claims | **PASSED** |
| `SEC-REV-1` | Account Suspension | User status change to `SUSPENDED` triggers `revokeRefreshTokens` | **PASSED** |
| `SEC-REV-2` | Account Suspension | User status change to `INACTIVE` triggers `revokeRefreshTokens` | **PASSED** |
| `SEC-REV-3` | Account Suspension | Duplicate suspension events handled idempotently | **PASSED** |
| `SEC-REV-4` | Account Suspension | Reactivation validates role and updates claims safely | **PASSED** |
| `SEC-STORE-1` | Secure Storage URLs | Suspended user CANNOT obtain signed storage URL | **PASSED** |
| `SEC-STORE-2` | Secure Storage URLs | Active user can obtain signed URL for owned document | **PASSED** |
| `SEC-STORE-3` | Secure Storage URLs | Arbitrary storage path without matching Firestore doc rejected | **PASSED** |
| `SEC-STORE-4` | Secure Storage URLs | Cross-customer access (Alice requesting Bob's file) rejected | **PASSED** |
| `SEC-STORE-5` | Secure Storage URLs | Request for soft-deleted document (`isDeleted: true`) rejected | **PASSED** |
| `SEC-STORE-6` | Secure Storage URLs | Path traversal attempt (`../`) rejected with `invalid-argument` | **PASSED** |
| `SEC-STORE-7` | Secure Storage URLs | Engineer accessing customer KYC/financial document rejected | **PASSED** |
| `SEC-STORE-8` | Secure Storage URLs | Unassigned BDE accessing another BDE's customer rejected | **PASSED** |
| `SEC-STORE-9` | Secure Storage URLs | Audit log created excluding sensitive signed URL token | **PASSED** |

---

## 3. SUITE 2: CLOUD FIRESTORE SECURITY RULES (27/27 PASSED)

**Execution:** `node --test firestore.test.js`  
**Duration:** 2469.5ms  

- **Collection Isolation:** Validated that unauthenticated callers cannot read or write `/users`, `/leads`, `/payments`, `/documents`.
- **Ownership Scoping:** Validated that Customer Alice cannot read or write Customer Bob's leads, profile, or payments.
- **Audit Trails:** Validated that payment receipts cannot be deleted by clients under any circumstances.
- **SEC-07 Corporate Privilege Escalation:** Validated that a user holding email `@hbxsolar.com` without a trusted Custom Claim is denied staff access.
- **SEC-08 Suspended Account Rejection:** Validated that users with `accountStatus: 'SUSPENDED'` or `'INACTIVE'` cannot read or create leads.
- **Immediate Revocation (< 50ms):** Validated that an authenticated user with an active token is immediately denied access when their profile document in Firestore is updated to `SUSPENDED`.
- **Anti-Self-Escalation:** Validated that neither Customers nor BDEs can mutate their own document to claim `ADMIN` or `SUPER_ADMIN` roles.

---

## 4. SUITE 3: CLOUD STORAGE SECURITY RULES (24/24 PASSED)

**Execution:** `node --test storage.test.js`  
**Duration:** 1176.9ms  

- **Zero-Trust KYC Isolation:** Customer Alice can upload and read her own KYC document; Customer Bob is strictly blocked from reading or writing into Alice's folder.
- **Internal Leads Protection:** Customer Bob cannot access internal leads documents.
- **Input Sanitization:** Executable binary files (`.exe`, `.sh`) are rejected by MIME validation.
- **Size Cap Enforcement:** Uploads exceeding the 15MB ceiling are rejected.
- **Zero-Loss Audit Trail:** Direct client-side deletion of files and payment receipts is permanently disabled.
- **Partner Boundaries:** Partners are blocked from accessing customer folders outside their authorized scope.
- **SEC-07 Domain Matching Removal:** Corporate email holders without staff Custom Claims cannot upload to project staff folders or read site survey photos.
- **SEC-08 Suspension Enforcement:** Users with `accountStatus: 'SUSPENDED'` or `'INACTIVE'` in their token cannot upload or download files.

---

## 5. SUITE 4: ANDROID JVM & ROBOLECTRIC REGRESSION (25/25 PASSED)

**Execution:** `gradle :app:testDebugUnitTest`  
**Duration:** 28s  

- **Document Integrity:** SHA-256 checksum computation, file tampering detection, sequential version incrementing (`DocumentIntegrityTest`, 3/3 passed).
- **Document Management:** Soft deletion query filtering, pending upload tracking, MIME detection, parent entity queries, standardized hierarchical storage paths (`DocumentManagementTest`, 6/6 passed).
- **Offline Upload Recovery:** Partial upload metadata preservation, persistent FIFO deduplication queue, non-deletable financial audit logs (`OfflineUploadRecoveryTest`, 3/3 passed).
- **Phone Auth & RBAC:** E.164 phone normalization, customer onboarding role assignment (non-BDE), self-registration SUPER_ADMIN rejection, suspended profile update blocking, invalid OTP rejection (`PhoneAuthAndRbacTest`, 5/5 passed).
- **Room Persistence:** Persistent FIFO sync queue, reactive lead observation, payment persistence (`RoomDatabaseTest`, 3/3 passed).
- **User Repository Rules:** Observability of profile creation, unauthenticated read denial, non-admin role escalation rejection (`UserRepositoryRuleTest`, 3/3 passed).
- **Core JVM & Context:** Context string access and math verification (`ExampleRobolectricTest`, `ExampleUnitTest`, 2/2 passed).

---

## 6. REGRESSION VERIFICATION CONCLUSION

The regression suite proves that the software logic across backend Cloud Functions, Firebase security rules, and Android data repositories is robust, test-verified, and meets zero-trust requirements. No regressions or broken workflows were detected.
