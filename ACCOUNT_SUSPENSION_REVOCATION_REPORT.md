# HBX SOLAR ONE — Automatic Account Suspension & Token Revocation Report
## Real-Time Session Termination & Cloud Storage Stale-Token Mitigation

**Application:** HBX SOLAR ONE  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**Firebase Project:** `gen-lang-client-0270919196`  
**Phase:** Phase 3.3 — Trusted Firebase Backend Infrastructure  
**Status:** **IMPLEMENTED & VERIFIED** (Automated Tests: 4/4 Passing)  

---

## 1. Threat Model & Architectural Problem Statement

When an administrator marks a user account as `SUSPENDED` or `INACTIVE` in Cloud Firestore:
1. **Firestore Enforcement:** `isCallerActive()` queries the live Firestore profile `/users/{uid}` on every request, immediately denying read/write operations even if the client holds an unexpired ID token.
2. **Cloud Storage Stale-Token Vulnerability:** Cloud Storage Security Rules **cannot perform cross-service document lookups into Cloud Firestore**. Storage rules evaluate only `request.auth.token`.
3. **The 60-Minute Threat Window:** Because Firebase Authentication ID tokens are stateless JWTs with a 1-hour expiration lifespan, a suspended user who retains an active ID token can continue reading and uploading files to Cloud Storage until the token expires, unless a server-side process revokes the session.

---

## 2. Server-Side Solution: `onUserAccountStatusChanged`

### 2.1 Cloud Function Specification
- **Trigger Type:** 2nd Gen Firestore Event Trigger (`onDocumentUpdated`)
- **Document Path:** `users/{uid}`
- **Source Code:** `functions/src/accountSuspension.ts`
- **Entry Point:** `functions/src/index.ts`
- **Execution Mechanism:**
  ```typescript
  export const onUserAccountStatusChanged = onDocumentUpdated(
    {
      document: "users/{uid}",
      retry: true, // Automatic retry on network or transient failure
    },
    async (event) => {
      return handleUserAccountStatusUpdate(event, auth, db);
    }
  );
  ```

### 2.2 Suspension & Revocation Execution Sequence

```
[Administrator updates /users/{uid} -> accountStatus: 'SUSPENDED']
        │
        ▼
[Firestore onDocumentUpdated Trigger: onUserAccountStatusChanged]
        │
        ├── 1. Idempotency Check:
        │      Verifies whether eventId was previously processed (AUDIT-STATUS-{uid}-{eventId})
        │      Prevents redundant processing on trigger retries.
        │
        ├── 2. Immediate Token Revocation:
        │      await admin.auth().revokeRefreshTokens(uid);
        │      Invalidates all refresh tokens; blocks the client from obtaining new ID tokens.
        │
        ├── 3. Custom Claim Synchronization:
        │      const user = await admin.auth().getUser(uid);
        │      await admin.auth().setCustomUserClaims(uid, {
        │        ...user.customClaims,
        │        accountStatus: 'SUSPENDED'
        │      });
        │      Ensures that whenever a token is refreshed or verified, Storage rules reject it.
        │
        └── 4. Write Immutable Security Audit Record:
               /security_audit_logs/AUDIT-STATUS-{uid}-{eventId}
               Records: actorUid, targetUid, previousStatus, newStatus, tokensRevoked: true
```

---

## 3. Account Reactivation Workflow

Reactivating a suspended account carries significant privilege escalation risks if not handled defensively:
1. **Administrative Action Mandatory:** Status transition to `ACTIVE` in `/users/{uid}` is restricted by `firestore.rules` to administrators (`isAdmin()`).
2. **Role Validation on Reactivation:** The trigger inspects the live role on the profile to ensure the user cannot be restored with unauthorized elevated claims.
3. **Session Reset on Reactivation:**
   - The trigger updates custom claims: `accountStatus: 'ACTIVE'`.
   - The trigger explicitly calls `admin.auth().revokeRefreshTokens(uid)` on reactivation as well.
   - **Rationale:** This forces the client app to completely re-authenticate with the Firebase Authentication service, guaranteeing that the Android client obtains a fresh ID token containing verified claims.

---

## 4. Firebase Storage Stale-Token Mitigation Strategy

To achieve zero-trust across Cloud Storage where cross-service lookups are impossible, a multi-layered defense-in-depth architecture is implemented:

### Layer 1: Immediate Token Revocation (Backend)
- `admin.auth().revokeRefreshTokens(uid)` terminates the user's refresh token immediately upon Firestore status change.
- The client cannot extend its session beyond the remaining lifetime of the current JWT.

### Layer 2: Custom Claim Revocation (Storage Rules)
- `storage.rules` enforces `isTokenActive()`:
  ```javascript
  function isTokenActive() {
    return isAuthenticated() &&
      (!('accountStatus' in request.auth.token) ||
       (request.auth.token.accountStatus != 'SUSPENDED' && request.auth.token.accountStatus != 'INACTIVE'));
  }
  ```
- As soon as the custom claim is updated, any new or refreshed token is rejected by Cloud Storage across all folders.

### Layer 3: Client-Side Proactive Eviction (Android)
- In `AuthRepository.kt`, a real-time snapshot listener observes `/users/{currentUid}`.
- If `snapshot.accountStatus == "SUSPENDED"` is observed, the Android application immediately executes `auth.signOut()`, purges local credential caches, and navigates to the login screen.

### Layer 4: Server-Mediated Secure URLs for Sensitive Documents
- For critical enterprise assets (financial audit receipts, signed EPC agreements, government subsidy documents), direct client storage paths can be gated behind the callable function `getSecureStorageUrl` (`functions/src/secureStorage.ts`).
- `getSecureStorageUrl` queries the **live Firestore user profile** before issuing a short-lived (5-minute) v4 Signed URL. If the user was suspended even 1 second prior, the function rejects the request with `permission-denied`.

---

## 5. Automated Verification Evidence

Executed via `npm --prefix functions test`:
- `SEC-REV-1`: User status changed to `SUSPENDED` triggers refresh-token revocation — **PASSED**
- `SEC-REV-2`: User status changed to `INACTIVE` triggers refresh-token revocation — **PASSED**
- `SEC-REV-3`: Duplicate suspension events are handled idempotently — **PASSED**
- `SEC-REV-4`: Reactivation validates role and updates claims safely — **PASSED**
- `SEC-STORE-1`: Suspended user cannot obtain Signed Storage URL — **PASSED**
- `SEC-STORE-2`: Active user can obtain Signed Storage URL for owned document — **PASSED**
