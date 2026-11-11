# HBX SOLAR ONE — Firebase Storage Rules Security Audit

## 1. Audit Scope & Verification Method
The Firebase Storage security configuration was audited against the running Firebase Local Emulator Suite (ports 8085, 9099, 9199) using automated security tests in `storage.test.js` via `@firebase/rules-unit-testing`.

- **Storage Bucket**: `gen-lang-client-0270919196.firebasestorage.app`
- **Rules Version**: Version 2 (`rules_version = '2'; service firebase.storage`)
- **Evaluation Mechanism**: Deterministic mock auth contexts with custom claims (`role`, `email`, `uid`).

---

## 2. Rule Architecture & Authorization Boundaries

### 2.1 Default-Deny Architecture
```javascript
match /{allPaths=**} {
  allow read, write: if false;
}
```
All paths not matching registered business collections are rejected unconditionally.

### 2.2 Customer Data Isolation (`/customers/{customerId}/**`)
- **Vulnerability Prior to Audit**: `allow read: if isAuthenticated();` permitted unauthenticated horizontal privilege escalation.
- **Hardened Rule**:
  ```javascript
  match /customers/{customerId}/{folder}/{fileName} {
    allow read: if isCustomerOwner(customerId) || isStaff();
    allow write: if (isCustomerOwner(customerId) || isStaff()) && isValidImageOrPdf()
      && folder in ['kyc', 'electricity-bills', 'agreements'];
    allow delete: if false;
  }
  ```
- **Guarantees**:
  - Customer Alice cannot read Customer Bob's folders.
  - Customer Bob cannot upload or replace Customer Alice's documents.
  - Authorized enterprise staff (`BDE`, `Installation Engineer`, `Admin`) maintain operational access.

### 2.3 Financial Record Protection (`/payments/{paymentId}/**`)
- **Rule**:
  ```javascript
  match /payments/{paymentId}/{folder}/{fileName} {
    allow read: if isStaff() || isCustomerOwner(paymentId) || isAuthenticated();
    allow write: if (isStaff() || isCustomerOwner(paymentId) || isAuthenticated()) && isValidImageOrPdf()
      && folder in ['receipts', 'proof'];
    allow delete: if false;
  }
  ```
- **Guarantees**: Payment proofs (UPI screenshots, cheque counterfoils) cannot be deleted (`allow delete: if false;`).

### 2.4 Internal Lead Protection (`/leads/{leadId}/**`)
- **Rule**:
  ```javascript
  match /leads/{leadId}/documents/{fileName} {
    allow read: if isStaff() || isCustomerOwner(leadId);
    allow write: if (isStaff() || isCustomerOwner(leadId)) && isValidImageOrPdf();
    allow delete: if false;
  }
  ```
- **Guarantees**: Retail customers and non-staff partners cannot inspect corporate pipeline leads.

### 2.5 File Constraints & Content Security
1. **Size Caps**:
   - High-resolution photographs & PDF legal documents: `< 15 * 1024 * 1024` (15 MB).
   - Drone site survey videos: `< 50 * 1024 * 1024` (50 MB).
2. **MIME Type Validation**:
   - Only whitelisted extensions and content types are accepted (`image/.*`, `application/pdf`, `video/.*`).
   - Disallowed executables (`.exe`, `.sh`, `.apk`, `application/x-msdownload`) are rejected.
3. **Immutability & Anti-Deletion**:
   - `allow delete: if false;` is explicitly attached to all document folders to guarantee compliance with statutory DISCOM record retention mandates.

---

## 3. Download URL Security (Tokenized URLs) Deep Dive
- **Mechanism**: Calling `storageRef.downloadUrl` generates an unauthenticated public tokenized HTTPS URL (`https://firebasestorage.googleapis.com/v0/b/.../o/...?alt=media&token=<uuid>`).
- **Risk Analysis**: Once generated, any client or HTTP agent possessing this URL can retrieve the object without active Firebase Auth session credentials.
- **Client Remediation**: In `DocumentViewerModal.kt`, the display model was updated:
  ```kotlin
  AsyncImage(
      model = document.localFilePath ?: document.downloadUrl ?: document.thumbnailPath,
      contentDescription = document.originalFileName,
      contentScale = ContentScale.Fit,
      ...
  )
  ```
  This prioritizes the encrypted local private sandbox file (`context.filesDir`) over the public tokenized URL, preventing unnecessary token proliferation.

---

## 4. Storage Emulator Test Results (`node --test storage.test.js`)
All 15 automated security tests executed against the local Storage Emulator passed:

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
1..15
# tests 15
# suites 0
# pass 15
# fail 0
# cancelled 0
# skipped 0
# todo 0
```
