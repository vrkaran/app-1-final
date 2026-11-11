# HBX SOLAR ONE — PHASE 3.2.1 SECURITY REMEDIATION REPORT
## Corporate Privilege Escalation & Suspended Account Access Control

**Application:** HBX SOLAR ONE  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**Target Environment:** Android / Google Cloud Firebase (`gen-lang-client-0270919196`)  
**Security Status:** **RESOLVED & VERIFIED**  
**Automated Rules Tests:** **41 / 41 PASSED** (22 Firestore + 19 Storage)  
**Production Rules Deployment:** **BLOCKED** (Pending explicit administrative deployment approval)  

---

## 1. Executive Summary

During the Phase 3.2 Live OTP Connectivity and Architecture Audit, two critical security vulnerabilities were discovered in the backend authorization layer:
1. **SEC-07 (Corporate Email Domain Privilege Escalation):** Backend rules authorized staff permissions if the user's email matched corporate domain regexes (`hbxsolar.com`, `purvanchalsolar.in`, `hbx.internal`), allowing users who signed up with public Google accounts under matching unverified domains to claim staff privileges without administrative assignment.
2. **SEC-08 (Suspended and Inactive Account Access Control):** Backend Firestore and Storage security rules lacked status checks. If a suspended or inactive user bypassed client-side navigation restrictions (e.g. via direct REST/SDK calls with an active token), backend access was not denied.

Both vulnerabilities have been fully remediated in the backend security rules (`firestore.rules` and `storage.rules`) and verified via automated test suites in the local Firebase emulators.

---

## 2. Vulnerability Remediation Details

### SEC-07: Corporate Email Domain Privilege Escalation

#### Vulnerability Analysis
- **Root Cause:** `isStaff()` in both `firestore.rules` and `storage.rules` used regular expression matching on `request.auth.token.email`:
  ```javascript
  // VULNERABLE IMPLEMENTATION (REMOVED)
  function isStaff() {
    return isAuthenticated() && (
      request.auth.token.role in ['SUPER_ADMIN', 'ADMIN', ...] ||
      (request.auth.token.email != null && request.auth.token.email.matches('.*@(hbxsolar\\.com|purvanchalsolar\\.in|hbx\\.internal)'))
    );
  }
  ```
- **Exploit Scenario:** Any user authenticating via Google OAuth or unverified email registration with a domain containing `@hbxsolar.com` or `@purvanchalsolar.in` received full staff privileges (reading all customer KYC documents, accessing unassigned leads, downloading site photos).

#### Source Code Remediation
- **Files Modified:** `/firestore.rules` (lines 15–28) and `/storage.rules` (lines 15–28).
- **Remediated Authorization Logic:**
  ```javascript
  // HARDENED IMPLEMENTATION: Trusted server-assigned Custom Claims ONLY
  function isStaff() {
    return isAuthenticated() && (
      'role' in request.auth.token &&
      request.auth.token.role in [
        'SUPER_ADMIN',
        'ADMIN',
        'OPERATIONS_MANAGER',
        'SALES_MANAGER',
        'TEAM_LEADER',
        'BDE',
        'INSTALLATION_ENGINEER',
        'SERVICE_ENGINEER'
      ]
    );
  }

  function isAdmin() {
    return isAuthenticated() && (
      'role' in request.auth.token &&
      request.auth.token.role in ['SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER']
    );
  }
  ```
- **Enforcement Principle:** Email domain inspection is completely eradicated. Only identities carrying cryptographic Firebase Custom Claims (`request.auth.token.role`) minted by an authenticated administrative process or Firebase Admin SDK can access staff boundaries.

---

### SEC-08: Suspended and Inactive Account Access Control

#### Vulnerability Analysis
- **Root Cause:** Account status checks existed solely in the Android client application (`AuthRepository.kt`). If a suspended or inactive user held an unexpired JWT token, backend Firestore collections (`leads`, `payments`, `documents`) and Firebase Storage paths allowed read/write operations without verifying account state.

#### Source Code Remediation
1. **Firestore Rules Hardening (`/firestore.rules`):**
   ```javascript
   // SEC-08: Backend enforcement of user account status
   function isCallerActive() {
     return isAuthenticated() &&
       (!('accountStatus' in request.auth.token) || (request.auth.token.accountStatus != 'SUSPENDED' && request.auth.token.accountStatus != 'INACTIVE')) &&
       (!exists(/databases/$(database)/documents/users/$(request.auth.uid)) ||
        !('accountStatus' in get(/databases/$(database)/documents/users/$(request.auth.uid)).data) ||
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.accountStatus != 'SUSPENDED' &&
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.accountStatus != 'INACTIVE'));
   }
   ```
   - Applied `isCallerActive()` to all protected collections:
     - `/leads/{leadId}`: read, create, update, delete
     - `/payments/{paymentId}`: read, create, update
     - `/documents/{documentId}`: read, create, update
     - `/document_audit_logs/{logId}`: read, create
2. **Storage Rules Hardening (`/storage.rules`):**
   ```javascript
   // SEC-08: Suspended / Inactive token check
   function isTokenActive() {
     return isAuthenticated() &&
       (!('accountStatus' in request.auth.token) ||
        (request.auth.token.accountStatus != 'SUSPENDED' && request.auth.token.accountStatus != 'INACTIVE'));
   }
   ```
   - Applied `isTokenActive()` across all storage buckets (`users`, `customers`, `leads`, `site-surveys`, `projects`, `quotations`, `payments`, `service-requests`, `company-assets`).
3. **Anti-Tampering on Account Status (`/firestore.rules` lines 135–147):**
   ```javascript
   allow update: if (
       isOwner(userId) &&
       isValidUser(request.resource.data) &&
       request.resource.data.role == resource.data.role &&
       request.resource.data.accountStatus == resource.data.accountStatus &&
       request.resource.data.uid == resource.data.uid &&
       request.resource.data.createdAt == resource.data.createdAt
     ) || (
       isAdmin() &&
       isValidUser(request.resource.data) &&
       request.resource.data.uid == resource.data.uid &&
       request.resource.data.createdAt == resource.data.createdAt
     );
   ```
   - Regular users CANNOT modify their own `accountStatus` or `role`.
   - Only administrators (`isAdmin()`) can modify user status (e.g. suspend, activate, reinstate).

---

## 3. Automated Test Evidence

### 3.1 Firestore Security Rules Test Suite (`firestore.test.js`)
- **Execution Command:** `node --test firestore.test.js`
- **Result:** **22 / 22 Tests PASSED (0 Failures)**
- **SEC-07 & SEC-08 Test Evidence:**
  - `ok 17 - SEC-07: User with corporate email domain @hbxsolar.com WITHOUT role custom claim CANNOT access staff leads`
  - `ok 18 - SEC-07: User with corporate email @purvanchalsolar.in WITHOUT custom claim CANNOT access document metadata`
  - `ok 19 - SEC-08: Suspended user (accountStatus: SUSPENDED in token) CANNOT create leads`
  - `ok 20 - SEC-08: Suspended user CANNOT read payments collection`
  - `ok 21 - SEC-08: User CANNOT update their own accountStatus from ACTIVE to SUSPENDED or vice versa`
  - `ok 22 - SEC-08: Admin CAN update another user accountStatus to SUSPENDED`

### 3.2 Firebase Storage Security Rules Test Suite (`storage.test.js`)
- **Execution Command:** `node --test storage.test.js`
- **Result:** **19 / 19 Tests PASSED (0 Failures)**
- **SEC-07 & SEC-08 Test Evidence:**
  - `ok 16 - SEC-07: Storage - User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT upload into project staff folders`
  - `ok 17 - SEC-07: Storage - User with corporate email @purvanchalsolar.in WITHOUT staff claim CANNOT read site survey photos`
  - `ok 18 - SEC-08: Storage - Suspended user (accountStatus: SUSPENDED in token) CANNOT upload documents`
  - `ok 19 - SEC-08: Storage - Suspended user CANNOT download or read files`

---

## 4. Security Verification Matrix

| Vulnerability ID | Description | Threat Vector | Prior Status | Remediated Status | Verification Test |
|---|---|---|---|---|---|
| **SEC-07** | Corporate Email Privilege Escalation | Spoofed/unverified `@hbxsolar.com` email claimed BDE/Staff rights | **FAIL** | **RESOLVED** | `firestore.test.js` (Tests 17, 18), `storage.test.js` (Tests 16, 17) |
| **SEC-08** | Suspended/Inactive Account Access | Suspended token bypassed UI check to read/write Firestore & Storage | **FAIL** | **RESOLVED** | `firestore.test.js` (Tests 19–22), `storage.test.js` (Tests 18, 19) |

---

## 5. Deployment Policy Compliance
As strictly instructed:
- **No rules were deployed to production.**
- Both `firestore.rules` and `storage.rules` remain version-controlled locally and verified against the local Firebase emulator environment.
- Any future production deployment must be initiated explicitly through authorized enterprise CI/CD channels.
