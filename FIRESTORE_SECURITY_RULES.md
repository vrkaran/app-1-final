# HBX SOLAR ONE — FIRESTORE SECURITY RULES & RBAC

## 1. Overview
HBX Solar One implements a **Default-Deny Zero-Trust Security Model** in Cloud Firestore (`firestore.rules`). Every read, write, update, and delete operation is strictly validated based on cryptographic Firebase Authentication tokens and verified user profiles in `/users/$(request.auth.uid)`.

---

## 2. 10-Tier Role-Based Access Control (RBAC)

The enterprise application defines 10 distinct operational roles:
1. `SUPER_ADMIN`: Full system administrative and infrastructure access.
2. `ADMIN`: Corporate executive with user management, payment verification, and configuration authority.
3. `OPERATIONS_MANAGER`: Regional operations and logistics oversight across district hubs.
4. `SALES_MANAGER`: Target tracking, commission allocations, and pipeline performance auditor.
5. `TEAM_LEADER`: Territory field supervisor managing BDEs and survey schedules.
6. `BDE` (Business Development Executive): Field sales, customer intake proposals, and token receipts.
7. `INSTALLATION_ENGINEER`: Field technician executing 23-stage installation pipelines and BOM delivery scans.
8. `SERVICE_ENGINEER`: Maintenance, breakdown tickets, and technical warranty audits.
9. `PARTNER`: Channel partners, EPC contractors, and local franchisees.
10. `CUSTOMER`: Retail and commercial end-users tracking their solar project and subsidy status.

---

## 3. Core Security Rules Architecture

### 3.1 Default Deny
All unmatched collections and documents are rejected by default:
```javascript
match /{document=**} {
  allow read, write: if false;
}
```

### 3.2 Helper Functions
```javascript
function isAuthenticated() {
  return request.auth != null && request.auth.uid != null;
}

function getUserData() {
  return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
}

function hasRole(role) {
  return isAuthenticated() && getUserData().role == role;
}

function isAdmin() {
  return hasRole('SUPER_ADMIN') || hasRole('ADMIN');
}
```

### 3.3 User Profile Security (`/users/{userId}`)
- **Read:** Any authenticated user can read profiles within the enterprise.
- **Create (Self-Registration):**
  - An authenticated user can only initialize their own document (`request.auth.uid == userId`).
  - Self-registrations **CANNOT** assign elevated roles:
    `request.resource.data.role in ['BDE', 'INSTALLATION_ENGINEER', 'CUSTOMER', 'PARTNER']`
  - Self-registrations for `ADMIN` or `SUPER_ADMIN` are rejected with `PERMISSION_DENIED`.
- **Update:**
  - Administrators can update any profile, including roles and statuses.
  - Normal users can update their profile information (e.g. phone, full name) **BUT CANNOT escalate their own role**:
    `request.resource.data.role == resource.data.role`
    `request.resource.data.accountStatus == resource.data.accountStatus`
- **Delete:** Strictly restricted to `SUPER_ADMIN`.

### 3.4 Leads & Proposals Security (`/leads/{leadId}`)
- **Read:** All authenticated enterprise staff can query leads.
- **Create:**
  - Must be authenticated.
  - Creator ID must match caller UID:
    `request.resource.data.userId == request.auth.uid`
  - Forging leads under other user IDs is blocked.
- **Update:**
  - Lead creator, assigned personnel, or administrators can update status, stages, and notes.

### 3.5 Payments Security (`/payments/{receiptId}`)
- **Read:** Authenticated staff.
- **Create:**
  - Field BDEs can log advance receipts with `status == 'Pending Verification'`.
  - Non-admins **CANNOT** create pre-verified payments.
- **Update:**
  - Only `ADMIN`, `SUPER_ADMIN`, or `OPERATIONS_MANAGER` can change status to `Verified`.

---

## 4. Automated Security Test Suite (`firestore.test.js`)
All security rules are verified locally against the Firebase Emulator suite:
1. `Unauthenticated user: cannot read users collection` → **PASSED**
2. `Unauthenticated user: cannot read leads collection` → **PASSED**
3. `Authenticated user: can create own profile as BDE` → **PASSED**
4. `Self-registration: cannot grant SUPER_ADMIN role to self` → **PASSED**
5. `Privilege Escalation: user cannot update their own role from BDE to SUPER_ADMIN` → **PASSED**
6. `Leads: authenticated user can create lead with their own userId` → **PASSED**
7. `Leads: user cannot forge lead with another user's userId` → **PASSED**
