# HBX SOLAR ONE — Trusted Staff Provisioning Implementation
## Firebase Admin SDK Cryptographic Custom Claims & RBAC Architecture

**Application:** HBX SOLAR ONE  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**Firebase Project:** `gen-lang-client-0270919196`  
**Phase:** Phase 3.3 — Trusted Firebase Backend Infrastructure  
**Status:** **IMPLEMENTED & VERIFIED** (Automated Backend Unit Tests: 5/5 Passing)  

---

## 1. Architectural Mandate & Problem Statement

In Firebase security architecture, Android client applications cannot securely mint or modify their own Firebase Authentication ID token custom claims (`request.auth.token.role`). Prior audits (SEC-07 & Phase 3.2.2) established that:
1. Client-side self-registration must never grant staff privileges based on corporate email domains (`@hbxsolar.com` or `@purvanchalsolar.in`).
2. Clients must never be able to elevate their own roles in Cloud Firestore or Authentication.
3. Legitimate staff privileges must be minted exclusively by a trusted, server-side administrative process via the **Firebase Admin SDK** (`admin.auth().setCustomUserClaims`).

---

## 2. Staff Provisioning Implementation Details

### 2.1 Cloud Function Interface (`provisionStaff`)
- **Trigger Type:** 2nd Gen Firebase Callable HTTPS Function (`onCall`)
- **Location:** `functions/src/staffProvisioning.ts`
- **Entry Point:** `functions/src/index.ts`
- **Request Payload Interface:**
  ```typescript
  export interface ProvisionStaffRequest {
    targetUid: string;
    role: UserRole;
    employeeId?: string;
    territoryId?: string;
  }
  ```

### 2.2 Strict Role-Based Access Control (RBAC) Hierarchy

| Caller Role | Permitted Target Roles | Prohibited Target Roles | Self-Escalation |
|---|---|---|---|
| **Unauthenticated** | None | All | **DENIED** (`unauthenticated`) |
| **CUSTOMER** | None | All | **DENIED** (`permission-denied`) |
| **BDE / Technician** | None | All | **DENIED** (`permission-denied`) |
| **ADMIN** | `BDE`, `INSTALLATION_ENGINEER`, `SERVICE_ENGINEER`, `TEAM_LEADER`, `SALES_MANAGER`, `OPERATIONS_MANAGER`, `PARTNER`, `CUSTOMER` | `ADMIN`, `SUPER_ADMIN` | **DENIED** (`permission-denied`) |
| **SUPER_ADMIN** | All roles (including `ADMIN`, `SUPER_ADMIN`) | None (subject to target validity) | **DENIED** (Self-modification blocked) |

### 2.3 Verification & Provisioning Flow

```
[Administrator Android Client]
        │
        ▼ (HTTPS Callable with Firebase Auth Bearer Token)
[provisionStaff Cloud Function]
        │
        ├── 1. Verify caller authentication & token claims (role in [SUPER_ADMIN, ADMIN])
        ├── 2. Verify caller identity against live Firestore /users/{callerUid} record
        ├── 3. Validate targetUid exists in Firebase Authentication (auth.getUser)
        ├── 4. Validate requested role is on explicit allowlist
        ├── 5. Enforce hierarchy (ADMIN cannot provision ADMIN or SUPER_ADMIN)
        ├── 6. Block self-escalation (callerUid !== targetUid)
        │
        ├── 7. Mint Custom Claims via Firebase Admin SDK:
        │      auth.setCustomUserClaims(targetUid, { role, accountStatus: 'ACTIVE' })
        │
        ├── 8. Synchronize Firestore Profile (/users/{targetUid}):
        │      update({ role, updatedAt, provisionedBy: callerUid, provisionedAt })
        │
        └── 9. Write Immutable Audit Record to /security_audit_logs/{logId}
```

---

## 3. Immutable Security Audit Trail

Every provisioning execution automatically logs an append-only event into Firestore collection `/security_audit_logs/{logId}`:
- **`logId`:** Unique identifier (`SEC-{timestamp}-{random}`)
- **`action`:** `"STAFF_PROVISIONED"`
- **`actorUid`:** Caller's Firebase UID
- **`actorRole`:** Validated role of the provisioning administrator
- **`targetUid`:** Target user's Firebase UID
- **`previousRole`:** Role before provisioning
- **`assignedRole`:** Newly granted role
- **`timestamp`:** Server timestamp (`FieldValue.serverTimestamp()`)
- **`metadata`:** `employeeId`, `territoryId`, `targetEmail`

In accordance with `/firestore.rules`, client applications have **zero write access** to `/security_audit_logs`. Only the Firebase Admin SDK can create these records.

---

## 4. Zero-Trust Client Onboarding Integration

In `AuthRepository.kt` (`resolveOrCreateInitialProfile`):
- All new self-registering users (whether via Phone OTP or Google Sign-In) default unconditionally to `UserRole.CUSTOMER.roleKey` and `employeeId = "CUST-${user.uid.take(4).uppercase()}"`.
- Email domain inspection logic was completely removed.
- A user can only obtain staff permissions (`BDE`, `INSTALLATION_ENGINEER`, `ADMIN`) after an authorized administrator invokes `provisionStaff`.

---

## 5. Automated Verification Evidence

Executed via `npm --prefix functions test`:
- `SEC-PROV-1`: Unauthorized caller (no auth) cannot provision staff — **PASSED**
- `SEC-PROV-2`: Ordinary Customer CANNOT provision BDE — **PASSED**
- `SEC-PROV-3`: Staff BDE CANNOT provision ADMIN — **PASSED**
- `SEC-PROV-4`: ADMIN CANNOT self-escalate or provision SUPER_ADMIN — **PASSED**
- `SEC-PROV-5`: Authorized SUPER_ADMIN CAN provision BDE and ADMIN successfully — **PASSED**
