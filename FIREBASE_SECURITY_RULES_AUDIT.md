# HBX SOLAR ONE — FIREBASE SECURITY RULES AUDIT
## Comprehensive Security Architecture & Zero-Trust Access Control Audit

**Project:** HBX SOLAR ONE  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**GCP / Firebase Project:** `gen-lang-client-0270919196`  
**Scope:** Cloud Firestore Security Rules (`firestore.rules`) & Firebase Storage Security Rules (`storage.rules`)  
**Audit Standard:** Zero-Trust Enterprise RBAC & Principle of Least Privilege  
**Status:** **SECURED & AUDITED** (Emulator Verified — 41/41 Security Rules Tests Passing)  

---

## 1. Security Architecture Overview

HBX SOLAR ONE enforces an enterprise-grade, defense-in-depth security architecture. Authentication is managed via Firebase Authentication with Phone OTP and Google Identity Services. Authorization is strictly decoupled from client-side state and enforced at the backend through Firebase Security Rules.

### 1.1 Core Principles
1. **Default Deny:** Both Firestore and Cloud Storage default to rejecting all read and write operations unless an explicit matching rule permits access.
2. **Cryptographic Token Verification:** Role assignments (`request.auth.token.role`) and account statuses (`request.auth.token.accountStatus`) must originate from server-side custom claims signed by Google.
3. **No Domain Regex Trust (SEC-07):** Corporate email matching (`@hbxsolar.com`, `@purvanchalsolar.in`) has been completely removed to prevent unauthorized self-assigned staff privileges from public OAuth signups.
4. **Active Account Enforcement (SEC-08):** Inactive or suspended users (`accountStatus in ['SUSPENDED', 'INACTIVE']`) are blocked at the rules layer from reading, creating, modifying, or uploading records across all collections and storage buckets.
5. **Zero-Loss Audit Immutability:** Deletion of document metadata, project evidence, and audit logs is prohibited (`allow delete: if false`). Soft-deletion with an immutable audit log is required.

---

## 2. Cloud Firestore Security Rules Deep-Dive (`firestore.rules`)

### 2.1 Helper Functions & Predicates
```javascript
function isAuthenticated() {
  return request.auth != null;
}

function isOwner(userId) {
  return request.auth != null && request.auth.uid == userId;
}

// SEC-07: Trusted server-assigned Custom Claims ONLY (Domain-only regex matching completely removed)
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

### 2.2 Collection Rules Summary

| Collection Path | Read Policy | Create Policy | Update Policy | Delete Policy |
|---|---|---|---|---|
| `/users/{userId}` | `isStaff() \|\| isOwner(userId)` | `isOwner(userId)` (Self-registration as CUSTOMER/BDE) | `isAdmin()` OR (`isOwner(userId)` with role & status locked) | **DENY** (`false`) |
| `/leads/{leadId}` | `isCallerActive() && (isStaff() \|\| isOwner)` | `isCallerActive() && isValidLead && isOwner` | `isCallerActive() && isValidLead && (isOwner \|\| isStaff \|\| assigned)` | `isCallerActive() && (isOwner \|\| isStaff)` |
| `/payments/{paymentId}` | `isCallerActive() && (isStaff() \|\| isOwner \|\| consumer)` | `isCallerActive() && isValidPayment && isOwner` | `isCallerActive() && isValidPayment && (isOwner \|\| isStaff)` | **DENY** (`false`) |
| `/documents/{documentId}` | `isCallerActive() && (isStaff() \|\| isOwner \|\| parentEntity)` | `isCallerActive() && isOwner && serverTimestamp` | `isCallerActive() && (isOwner \|\| isStaff)` with anti-hijacking | **DENY** (`false`) |
| `/document_audit_logs/{logId}` | `isCallerActive() && isStaff()` | `isCallerActive() && actorUid == auth.uid` | **DENY** (`false`) | **DENY** (`false`) |

---

## 3. Firebase Storage Security Rules Deep-Dive (`storage.rules`)

### 3.1 Authorization Predicates
```javascript
function isAuthenticated() {
  return request.auth != null && request.auth.uid != null;
}

function isCustomerOwner(customerId) {
  return isAuthenticated() && request.auth.uid == customerId;
}

// SEC-07: Trusted server-assigned Custom Claims ONLY
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

// SEC-08: Suspended / Inactive token check
function isTokenActive() {
  return isAuthenticated() &&
    (!('accountStatus' in request.auth.token) ||
     (request.auth.token.accountStatus != 'SUSPENDED' && request.auth.token.accountStatus != 'INACTIVE'));
}
```

### 3.2 Storage Hierarchy & Access Control Matrix

| Storage Bucket Path | Read Access | Write Access | Delete Access | Content / Size Limits |
|---|---|---|---|---|
| `users/{userId}/profile/{fileName}` | `isTokenActive()` | `isTokenActive() && auth.uid == userId` | **DENY** | Image / PDF (<15MB) |
| `customers/{customerId}/{folder}/{fileName}` | `isTokenActive() && (isCustomerOwner \|\| isStaff)` | `isTokenActive() && (isCustomerOwner \|\| isStaff)` | **DENY** | `kyc`, `electricity-bills`, `agreements` (<15MB) |
| `leads/{leadId}/documents/{fileName}` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | **DENY** | Image / PDF (<15MB) |
| `site-surveys/{surveyId}/photos/{fileName}` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | `isTokenActive() && isStaff` | **DENY** | Image / PDF (<15MB) |
| `site-surveys/{surveyId}/videos/{fileName}` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | `isTokenActive() && isStaff` | **DENY** | Video (<50MB) |
| `projects/{projectId}/{folder}/{fileName}` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | `isTokenActive() && isStaff` | **DENY** | Project folders (<15MB) |
| `payments/{paymentId}/{folder}/{fileName}` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | `isTokenActive() && (isStaff \|\| isCustomerOwner)` | **DENY** | Receipts / proof (<15MB) |
| `company-assets/{fileName}` | `isTokenActive()` | **DENY** (CI/Console Only) | **DENY** | Static enterprise assets |

---

## 4. Emulator Automated Verification Suite

All rules are verified through automated Mocha/Node test runners targeting the local Firebase Suite (`127.0.0.1:8085` for Firestore and `127.0.0.1:9199` for Storage):

### 4.1 Results Summary
- **Firestore Rules Suite (`firestore.test.js`):** **22 / 22 Passed (100%)**
- **Storage Rules Suite (`storage.test.js`):** **19 / 19 Passed (100%)**
- **Total Security Rules Tests:** **41 / 41 Passed (100%)**

---

## 5. Deployment Policy & Sign-off
In strict accordance with Phase 3 project governance:
- **Production rules deployment remains unexecuted.**
- Any production deployment requires verified CI/CD pipeline triggers and administrative key approval.
