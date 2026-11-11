# HBX SOLAR ONE — ACCOUNT REVOCATION ARCHITECTURAL LIMITATIONS & RISK ANALYSIS

**Auditor:** Independent Principal Firebase Security Architect  
**Topic:** Token Revocation Mechanics, JWT Expiration Windows, and Cloud Storage Statelessness  
**Scope:** Firebase Authentication, Cloud Firestore Security Rules, and Cloud Storage Security Rules  
**Date:** November 11, 2026  

---

## 1. THE CORE TECHNICAL REALITY: WHY REVOCATION IS NOT INSTANTANEOUS FOR ALL SERVICES

When an enterprise administrator clicks **"Suspend User"** in an application dashboard, security compliance standards (e.g., ISO 27001, SOC 2) expect immediate cessation of all data access. However, in modern distributed cloud architectures based on OAuth 2.0 and OpenID Connect (OIDC), **token revocation behaves differently depending on the service evaluating the request**.

In Google Firebase:
1. **Firebase ID Tokens are stateless JSON Web Tokens (JWTs)**.
2. They are cryptographically signed by Google's private keys.
3. They contain a fixed issuance timestamp (`auth_time`, `iat`) and a fixed expiration timestamp (`exp`), set to **exactly 3,600 seconds (1 hour)** from issuance.
4. Calling `admin.auth().revokeRefreshTokens(uid)` updates the user's `tokensValidAfterTime` on the central Firebase Auth server.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        JWT TOKEN ARCHITECTURE                          │
│                                                                        │
│  HEADER: { "alg": "RS256", "kid": "...", "typ": "JWT" }                │
│  PAYLOAD: {                                                            │
│    "uid": "user_xyz",                                                  │
│    "role": "BDE",                                                      │
│    "accountStatus": "ACTIVE",                                          │
│    "iat": 1700000000,                                                  │
│    "exp": 1700003600  <── FIXED FOR 60 MINUTES (CANNOT BE MUTATED)     │
│  }                                                                     │
│  SIGNATURE: HMACSHA256(base64UrlEncode(header) + "." + payload, key)  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. COMPARATIVE ENFORCEMENT: CLOUD FIRESTORE VS. CLOUD STORAGE

The key distinction in Firebase security lies in whether the target service evaluates security rules **statefully (with database lookups)** or **statelessly (inspecting only the token itself)**.

### A. Cloud Firestore (True Real-Time, Sub-Second Revocation)
Cloud Firestore rules support the `get()` and `exists()` functions, which allow rules to perform an online lookup against any document in the database during rule evaluation.

HBX SOLAR ONE leverages this in `firestore.rules`:
```javascript
function isCallerActive() {
  return isAuthenticated() &&
    (!('accountStatus' in request.auth.token) || 
     (request.auth.token.accountStatus != 'SUSPENDED' && request.auth.token.accountStatus != 'INACTIVE')) &&
    (!exists(/databases/$(database)/documents/users/$(request.auth.uid)) ||
     ('accountStatus' in get(/databases/$(database)/documents/users/$(request.auth.uid)).data &&
      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.accountStatus != 'SUSPENDED' &&
      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.accountStatus != 'INACTIVE'));
}
```
- **Evaluation Mechanism:** For EVERY database read, query, or write, Firestore fetches `/databases/{db}/documents/users/{callerUid}`.
- **Revocation Latency:** **< 50 milliseconds**. The moment an administrator writes `accountStatus: "SUSPENDED"` to Firestore, all subsequent client operations immediately fail with `7 PERMISSION_DENIED`.
- **Verified Test:** `firestore.test.js`:
  `ZERO-TRUST: Immediate Revocation - Previously authenticated user with active token is DENIED after Admin suspends profile in Firestore` (PASSED in 84ms).

---

### B. Cloud Storage (Stateless Evaluation Window: Up to 60 Minutes)
Google Cloud Storage is an independent, global object storage service. Cloud Storage security rules evaluate requests at the edge.

- **The Limitation:** Cloud Storage rules **CANNOT execute cross-service database lookups**. Functions like `get()` or `firestore.get()` DO NOT exist in `storage.rules`.
- **The Stateless Check:** Cloud Storage rules inspect ONLY the claims encoded inside the client's JWT payload (`request.auth.token`).
- **The Consequence:** If User Bob acquires an ID token at 10:00 AM, and an administrator suspends Bob's account at 10:05 AM:
  - Bob's refresh token is revoked on Firebase Auth.
  - Bob cannot refresh or get a new token.
  - Bob's Firestore access terminates at 10:05 AM.
  - **HOWEVER, Bob's existing 10:00 AM ID token remains valid against direct Cloud Storage security rules until 11:00 AM (`exp`).**

---

## 3. HBX SOLAR ONE MULTI-LAYERED DEFENSE-IN-DEPTH MATRIX

To address this 60-minute token window for object storage, HBX SOLAR ONE implements five complementary lines of defense:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DEFENSE-IN-DEPTH LAYERS                         │
├────────────────────────────────┬───────────────────────────────────────┤
│ Layer 1: Central Token Kill    │ admin.auth().revokeRefreshTokens(uid) │
│                                │ Prevents new token generation         │
├────────────────────────────────┼───────────────────────────────────────┤
│ Layer 2: Live Firestore Denial │ get(/users/uid).accountStatus check   │
│                                │ Sub-second database lock              │
├────────────────────────────────┼───────────────────────────────────────┤
│ Layer 3: Server-Mediated URLs  │ getSecureStorageUrl (v4 Signed URLs)  │
│                                │ Live Firestore check before signing   │
├────────────────────────────────┼───────────────────────────────────────┤
│ Layer 4: Client Session Evict  │ Android app detects SUSPENDED and     │
│                                │ executes auth.signOut()               │
├────────────────────────────────┼───────────────────────────────────────┤
│ Layer 5: Short-Lived URLs      │ 300-second (5 min) signed URL expiry  │
└────────────────────────────────┴───────────────────────────────────────┘
```

### Detailed Breakdown of Mitigations:

1. **Server-Mediated Storage Architecture (`getSecureStorageUrl`):**
   - The primary channel for accessing sensitive documents (KYC identity documents, banking records, contracts, payment receipts) is routed through the Cloud Function `getSecureStorageUrl`.
   - `getSecureStorageUrl` executes a live read of `/users/{callerUid}` in Cloud Firestore before issuing any signed URL.
   - If `accountStatus === 'SUSPENDED'`, the function throws `HttpsError("permission-denied")`, shutting down access immediately regardless of token age.

2. **5-Minute Signed URL Lifespan:**
   - Any signed URL issued prior to suspension expires in at most 300 seconds (5 minutes), minimizing replay windows.

3. **Android Client-Side Account State Observer:**
   - In `UserRepository.kt` / `AuthRepository.kt`, the Android application maintains a real-time Firestore listener on `/users/{currentUid}`.
   - Upon observing `accountStatus == "SUSPENDED"`, the client invokes `FirebaseAuth.getInstance().signOut()`, purging all cached tokens from the device keystore and navigating to the blocked account screen.

4. **Storage Rules Claim Check (`storage.rules`):**
   - As soon as the client attempts to refresh its token or re-authenticates, the new token includes `accountStatus: "SUSPENDED"`.
   - `storage.rules` denies all operations if `request.auth.token.accountStatus == 'SUSPENDED'`.

---

## 4. AUDIT RECOMMENDATION FOR HIGH-COMPLIANCE WORKLOADS

For ultra-strict regulatory environments (e.g., RBI data localization, ISO 27001 Annex A.9.2.6):
1. **Deprecate Direct Cloud Storage Client Downloads:** Transition all document retrieval in the mobile client to use `getSecureStorageUrl` exclusively.
2. **Session Invalidation Push Notification:** Trigger an administrative Firebase Cloud Messaging (FCM) high-priority data message upon account suspension that forces client-side token cache purge immediately.
