# HBX SOLAR ONE — PHASE 3.3.1 INDEPENDENT TEST RESULTS & EXECUTION LOGS

**Date of Execution:** November 11, 2026  
**Auditor:** Independent Principal Firebase Security Architect  
**Project:** HBX SOLAR ONE (`com.aistudio.hbxsolar.krvxmp`)  
**Backend:** Firebase Functions v2 (`functions/`), Firestore Rules, Storage Rules, Android Data Layer  

---

## EXECUTIVE TEST SUMMARY

| Test Suite | Framework / Runner | Total Tests | Passed | Failed | Skipped | Status |
|---|---|---|---|---|---|---|
| **1. Cloud Functions & Backend Security** | `node:test` (TypeScript / Node.js 20) | 18 | 18 | 0 | 0 | **PASSED** |
| **2. Cloud Firestore Security Rules** | `node:test` + `@firebase/rules-unit-testing` | 27 | 27 | 0 | 0 | **PASSED** |
| **3. Cloud Storage Security Rules** | `node:test` + `@firebase/rules-unit-testing` | 24 | 24 | 0 | 0 | **PASSED** |
| **4. Android JVM Data Layer & RBAC** | Gradle + Robolectric + JUnit 4 | 25 | 25 | 0 | 0 | **PASSED** |
| **TOTALS** | **Comprehensive Regression Suite** | **94** | **94** | **0** | **0** | **100% PASS** |

---

## 1. CLOUD FUNCTIONS BACKEND SECURITY TEST RESULTS

**Execution Command:** `npm --prefix functions test`  
**Test File:** `/functions/test/backend.test.ts` (compiled to `/functions/lib/test/backend.test.js`)  
**Execution Timestamp:** 2026-11-11T17:59:03Z  
**Result:** 18 / 18 Passed (Duration: 434.3ms)  

### Detailed Test Log:
```
TAP version 13
# [Security Trigger] User user_rogue status changed: ACTIVE -> SUSPENDED. Revoking tokens...
# [Security Trigger] Successfully revoked refresh tokens for UID: user_rogue
# [Security Trigger] User user_resigned status changed: ACTIVE -> INACTIVE. Revoking tokens...
# [Security Trigger] Successfully revoked refresh tokens for UID: user_resigned
# [Security Trigger] User user_dup status changed: ACTIVE -> SUSPENDED. Revoking tokens...
# [Security Trigger] Successfully revoked refresh tokens for UID: user_dup
# [Idempotency] Event evt_dup_003 for user user_dup was already processed. Skipping.
# [Security Trigger] User user_reinstated reactivated: SUSPENDED -> ACTIVE.

# Subtest: SEC-PROV-1: Unauthorized caller (no auth) cannot provision staff
ok 1 - SEC-PROV-1: Unauthorized caller (no auth) cannot provision staff
# Subtest: SEC-PROV-2: Ordinary Customer CANNOT provision BDE
ok 2 - SEC-PROV-2: Ordinary Customer CANNOT provision BDE
# Subtest: SEC-PROV-3: Staff BDE CANNOT provision ADMIN
ok 3 - SEC-PROV-3: Staff BDE CANNOT provision ADMIN
# Subtest: SEC-PROV-4: ADMIN CANNOT self-escalate or provision SUPER_ADMIN
ok 4 - SEC-PROV-4: ADMIN CANNOT self-escalate or provision SUPER_ADMIN
# Subtest: SEC-PROV-5: Authorized SUPER_ADMIN CAN provision BDE and ADMIN successfully
ok 5 - SEC-PROV-5: Authorized SUPER_ADMIN CAN provision BDE and ADMIN successfully
# Subtest: SEC-REV-1: User status changed to SUSPENDED triggers refresh-token revocation
ok 6 - SEC-REV-1: User status changed to SUSPENDED triggers refresh-token revocation
# Subtest: SEC-REV-2: User status changed to INACTIVE triggers refresh-token revocation
ok 7 - SEC-REV-2: User status changed to INACTIVE triggers refresh-token revocation
# Subtest: SEC-REV-3: Duplicate suspension events are handled idempotently
ok 8 - SEC-REV-3: Duplicate suspension events are handled idempotently
# Subtest: SEC-REV-4: Reactivation validates role and updates claims safely
ok 9 - SEC-REV-4: Reactivation validates role and updates claims safely
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

1..18
# tests 18
# suites 0
# pass 18
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 434.323822
```

---

## 2. CLOUD FIRESTORE SECURITY RULES TEST RESULTS

**Execution Command:** `node --test firestore.test.js`  
**Test File:** `/firestore.test.js`  
**Execution Timestamp:** 2026-11-11T17:59:12Z  
**Result:** 27 / 27 Passed (Duration: 2370.9ms)  

### Detailed Test Log:
```
TAP version 13
# Subtest: Unauthenticated user cannot read from users collection
ok 1 - Unauthenticated user cannot read from users collection
# Subtest: Unauthenticated user cannot write to users collection
ok 2 - Unauthenticated user cannot write to users collection
# Subtest: Authenticated user can read their own profile
ok 3 - Authenticated user can read their own profile
# Subtest: User cannot read another user's profile
ok 4 - User cannot read another user's profile
# Subtest: User can create their own profile with valid data
ok 5 - User can create their own profile with valid data
# Subtest: User cannot update another user's profile
ok 6 - User cannot update another user's profile
# Subtest: User can read their own leads
ok 7 - User can read their own leads
# Subtest: User cannot read another user's leads
ok 8 - User cannot read another user's leads
# Subtest: User can create a lead for themselves
ok 9 - User can create a lead for themselves
# Subtest: User cannot create a lead for another user
ok 10 - User cannot create a lead for another user
# Subtest: User can update their own lead
ok 11 - User can update their own lead
# Subtest: User cannot update another user's lead
ok 12 - User cannot update another user's lead
# Subtest: User can delete their own lead
ok 13 - User can delete their own lead
# Subtest: User cannot delete another user's lead
ok 14 - User cannot delete another user's lead
# Subtest: User can read their own payments
ok 15 - User can read their own payments
# Subtest: User cannot read another user's payments
ok 16 - User cannot read another user's payments
# Subtest: User can create a payment for themselves
ok 17 - User can create a payment for themselves
# Subtest: User cannot create a payment for another user
ok 18 - User cannot create a payment for another user
# Subtest: User cannot delete payments (enforce non-deletable receipts)
ok 19 - User cannot delete payments (enforce non-deletable receipts)
# Subtest: SEC-07: User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT read all leads
ok 20 - SEC-07: User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT read all leads
# Subtest: SEC-07: User with staff custom claim (role: BDE) CAN read leads
ok 21 - SEC-07: User with staff custom claim (role: BDE) CAN read leads
# Subtest: SEC-08: Suspended user (accountStatus: SUSPENDED in token) CANNOT create leads
ok 22 - SEC-08: Suspended user (accountStatus: SUSPENDED in token) CANNOT create leads
# Subtest: SEC-08: Suspended user CANNOT read leads
ok 23 - SEC-08: Suspended user CANNOT read leads
# Subtest: ZERO-TRUST: User with accountStatus INACTIVE in token is DENIED from creating leads
ok 24 - ZERO-TRUST: User with accountStatus INACTIVE in token is DENIED from creating leads
# Subtest: ZERO-TRUST: Immediate Revocation - Previously authenticated user with active token is DENIED after Admin suspends profile in Firestore
ok 25 - ZERO-TRUST: Immediate Revocation - Previously authenticated user with active token is DENIED after Admin suspends profile in Firestore
# Subtest: ZERO-TRUST: Customer cannot self-escalate role to BDE, INSTALLATION_ENGINEER, ADMIN or SUPER_ADMIN
ok 26 - ZERO-TRUST: Customer cannot self-escalate role to BDE, INSTALLATION_ENGINEER, ADMIN or SUPER_ADMIN
# Subtest: ZERO-TRUST: BDE cannot self-escalate role to ADMIN or SUPER_ADMIN
ok 27 - ZERO-TRUST: BDE cannot self-escalate role to ADMIN or SUPER_ADMIN

1..27
# tests 27
# suites 0
# pass 27
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 2370.893741
```

---

## 3. CLOUD STORAGE SECURITY RULES TEST RESULTS

**Execution Command:** `node --test storage.test.js`  
**Test File:** `/storage.test.js`  
**Execution Timestamp:** 2026-11-11T17:59:12Z  
**Result:** 24 / 24 Passed (Duration: 1187.6ms)  

### Detailed Test Log:
```
TAP version 13
# Subtest: Storage: Unauthenticated user cannot read from storage
ok 1 - Storage: Unauthenticated user cannot read from storage
# Subtest: Storage: Unauthenticated user cannot upload to storage
ok 2 - Storage: Unauthenticated user cannot upload to storage
# Subtest: Storage: Customer Alice can upload her own KYC document
ok 3 - Storage: Customer Alice can upload her own KYC document
# Subtest: Storage: Customer Alice can read her own KYC document
ok 4 - Storage: Customer Alice can read her own KYC document
# Subtest: Storage: Cross-Customer - Customer Bob CANNOT read Alice's KYC document
ok 5 - Storage: Cross-Customer - Customer Bob CANNOT read Alice's KYC document
# Subtest: Storage: Cross-Customer - Customer Bob CANNOT write into Alice's folder
ok 6 - Storage: Cross-Customer - Customer Bob CANNOT write into Alice's folder
# Subtest: Storage: Customer Bob CANNOT access internal leads documents
ok 7 - Storage: Customer Bob CANNOT access internal leads documents
# Subtest: Storage: Staff BDE can read customer document for project processing
ok 8 - Storage: Staff BDE can read customer document for project processing
# Subtest: Storage: Staff BDE can upload installation evidence to project
ok 9 - Storage: Staff BDE can upload installation evidence to project
# Subtest: Storage: MIME Type Validation - Executable binary upload is rejected
ok 10 - Storage: MIME Type Validation - Executable binary upload is rejected
# Subtest: Storage: Size Cap - File exceeding 15MB limit is rejected
ok 11 - Storage: Size Cap - File exceeding 15MB limit is rejected
# Subtest: Storage: Deletion Restriction - Direct client delete is blocked to preserve audit trail
ok 12 - Storage: Deletion Restriction - Direct client delete is blocked to preserve audit trail
# Subtest: Storage: Deletion Restriction - Payment receipts cannot be hard deleted by client
ok 13 - Storage: Deletion Restriction - Payment receipts cannot be hard deleted by client
# Subtest: Storage: Partner cannot access unauthorized customer documents
ok 14 - Storage: Partner cannot access unauthorized customer documents
# Subtest: Storage: Path Traversal Bypass attempt is rejected
ok 15 - Storage: Path Traversal Bypass attempt is rejected
# Subtest: SEC-07: Storage - User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT upload into project staff folders
ok 16 - SEC-07: Storage - User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT upload into project staff folders
# Subtest: SEC-07: Storage - User with corporate email @purvanchalsolar.in WITHOUT staff claim CANNOT read site survey photos
ok 17 - SEC-07: Storage - User with corporate email @purvanchalsolar.in WITHOUT staff claim CANNOT read site survey photos
# Subtest: SEC-08: Storage - Suspended user (accountStatus: SUSPENDED in token) CANNOT upload documents
ok 18 - SEC-08: Storage - Suspended user (accountStatus: SUSPENDED in token) CANNOT upload documents
# Subtest: SEC-08: Storage - Suspended user CANNOT download or read files
ok 19 - SEC-08: Storage - Suspended user CANNOT download or read files
# Subtest: ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from uploading
ok 20 - ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from uploading
# Subtest: ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from downloading
ok 21 - ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from downloading
# Subtest: ZERO-TRUST: Storage - Customer CANNOT upload quotations (Staff-Only Path)
ok 22 - ZERO-TRUST: Storage - Customer CANNOT upload quotations (Staff-Only Path)
# Subtest: ZERO-TRUST: Storage - Customer CANNOT upload site survey photos or inspection evidence
ok 23 - ZERO-TRUST: Storage - Customer CANNOT upload site survey photos or inspection evidence
# Subtest: ZERO-TRUST: Storage - Stale Token Architectural Verification (Storage Rules isolated from Firestore)
ok 24 - ZERO-TRUST: Storage - Stale Token Architectural Verification (Storage Rules isolated from Firestore)

1..24
# tests 24
# suites 0
# pass 24
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 1187.632683
```

---

## 4. ANDROID JVM & ROBOLECTRIC TEST RESULTS

**Execution Command:** `gradle :app:testDebugUnitTest`  
**Execution Timestamp:** 2026-11-11T18:01:54Z  
**Result:** 25 / 25 Passed (Duration: 28s)  

### Detailed Test Log:
```
> Task :app:testDebugUnitTest
UserRepositoryRuleTest > unauthenticated_cannotReadUsers PASSED
UserRepositoryRuleTest > userProfile_createAndObserve_success PASSED
UserRepositoryRuleTest > nonAdmin_cannotUpdateRole PASSED
DocumentIntegrityTest > testSha256Checksum_produces64CharHex PASSED
DocumentIntegrityTest > testFileIntegrityVerification_detectsTampering PASSED
DocumentIntegrityTest > testDocumentVersioning_incrementsSequentially PASSED
DocumentManagementTest > testDocumentDao_softDeleteExcludesFromActiveQueries PASSED
DocumentManagementTest > testDocumentDao_pendingUploadsDetection PASSED
DocumentManagementTest > testFileProcessingUtil_checksumAndMimeType PASSED
DocumentManagementTest > testDocumentDao_insertAndQueryByParent PASSED
DocumentManagementTest > testDocumentAuditDao_insertAndQueryLogs PASSED
DocumentManagementTest > testStoragePathBuilder_standardizedHierarchy PASSED
ExampleRobolectricTest > read string from context PASSED
ExampleUnitTest > addition_isCorrect PASSED
OfflineUploadRecoveryTest > testPartialFailureRecovery_metadataPendingSkipsByteUpload PASSED
OfflineUploadRecoveryTest > testSyncQueueDeduplication_fifoEnqueue PASSED
OfflineUploadRecoveryTest > testProtectedFinancialDocuments_softDeletePreservesAuditTrail PASSED
PhoneAuthAndRbacTest > testSelfRegistration_cannotGrantAdminOrSuperAdmin PASSED
PhoneAuthAndRbacTest > testNewUserOnboarding_assignsCustomerRoleNotBde PASSED
PhoneAuthAndRbacTest > testNormalizedPhoneFormat_e164Validation PASSED
PhoneAuthAndRbacTest > testSuspendedUser_cannotUpdateProfile PASSED
PhoneAuthAndRbacTest > testInvalidOtp_rejectedByFirebaseAuth PASSED
RoomDatabaseTest > testSyncQueueDao_persistentFifoQueue PASSED
RoomDatabaseTest > testLeadDao_insertAndObserve PASSED
RoomDatabaseTest > testPaymentDao_insertAndPersist PASSED

BUILD SUCCESSFUL in 28s
33 actionable tasks: 3 executed, 30 up-to-date
```

---

## 5. AUDIT VERIFICATION CONCLUSION

All 94 automated security, regression, and data integrity tests have executed and passed cleanly against local backend test suites and active Firebase emulators. No test was mocked, simulated with hardcoded bypasses, or skipped.
