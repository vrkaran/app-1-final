# HBX SOLAR ONE — FIREBASE STAGING ENVIRONMENT AUDIT

**Component:** Environment Configuration & Multi-Tenant Staging Isolation  
**Target Project:** `gen-lang-client-0270919196`  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**DevOps Engineer:** Principal Firebase DevOps Engineer  
**Date:** November 11, 2026  

---

## 1. PROJECT CONFIGURATION INVENTORY & CONSISTENCY CHECK

A thorough forensic audit of all project configuration files was performed to identify any discrepancies or misconfigurations:

| Configuration File | Inspected Key | Found Value | Assessment |
|---|---|---|---|
| `app/google-services.json` | `project_info.project_id` | `gen-lang-client-0270919196` | Consistent with target project |
| `app/google-services.json` | `project_info.storage_bucket` | `gen-lang-client-0270919196.firebasestorage.app` | Standard Firebase Storage URL |
| `app/google-services.json` | `client[0].android_client_info.package_name` | `com.aistudio.hbxsolar.krvxmp` | Matches current app ID exactly |
| `app/google-services.json` | `client[1].android_client_info.package_name` | `com.aistudio.hbxsolar.qptwzx` | Legacy/pre-rename client ID |
| `firebase.json` | `firestore[0].database` | `ai-studio-android-hbxsolar-95deda1e-af92-4241-b763-79f9a656d324` | Matches applet config |
| `firebase_applet_config.xml` | `firestore_database_id` | `ai-studio-android-hbxsolar-95deda1e-af92-4241-b763-79f9a656d324` | Consistent with `firebase.json` |
| `firebase_applet_config.xml` | `recaptcha_site_key` | `6LemxhouAAAAAEWve4fSVFRZRGS4_t1Qh017l4wg` | Pre-configured reCAPTCHA Enterprise |
| `.firebaserc` | `projects.default` | *FILE MISSING* | Needs initialization |
| `functions/package.json` | `engines.node` | `>=20` | Modern LTS runtime compliant |

---

## 2. KEY AUDIT FINDINGS

### Finding 1: Dual Android Client Definitions in `google-services.json`
`app/google-services.json` contains two client definitions:
1. `com.aistudio.hbxsolar.krvxmp` (Active production applicationId)
2. `com.aistudio.hbxsolar.qptwzx` (Legacy application identifier)
- **Impact:** LOW. The Google Services Gradle plugin filters by current `applicationId` (`krvxmp`) at build time.
- **Recommendation:** Maintain both in staging configuration to avoid breaking existing development installations.

### Finding 2: Named Firestore Database vs. Default Database
- The project does not use the standard `(default)` Firestore database ID. Instead, it targets a named database:
  `ai-studio-android-hbxsolar-95deda1e-af92-4241-b763-79f9a656d324`
- **Impact:** HIGH for deployment.
  - Standard Firebase CLI commands (`firebase deploy --only firestore:rules`) default to `(default)`.
  - In `firebase.json`, line 17 explicitly specifies `"database": "ai-studio-android-hbxsolar-95deda1e-af92-4241-b763-79f9a656d324"`.
  - This ensures deployment targets the exact provisioned database without overwriting an unrelated `(default)` instance.

### Finding 3: Missing Dedicated Staging Firebase Project
- All configurations currently target `gen-lang-client-0270919196`.
- There is no separate `gen-lang-client-0270919196-staging` or `hbxsolar-staging` project.
- **Rule Violation Risk:** Deploying staging code directly to `gen-lang-client-0270919196` violates enterprise separation of environments and Phase 3.4 Safety Rule #2 ("Do not deploy to production").

### Finding 4: CLI Ambient Credential Routing
- The execution of `firebase projects:list` revealed that the environment's ambient gcloud/OAuth credentials resolve to host project `524568939263` where `firebase.googleapis.com` is disabled.
- Direct CLI deployments cannot be executed from inside this automated container until external authentication is provided.

---

## 3. RECOMMENDED STAGING ISOLATION ARCHITECTURE

To achieve zero-risk staging validation without polluting or corrupting production data:

### Strategy A: Dedicated Staging Project (Industry Best Practice - RECOMMENDED)
```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     STAGING FIREBASE PROJECT    │       │   PRODUCTION FIREBASE PROJECT   │
│   (e.g., hbxsolar-one-staging)  │       │   (gen-lang-client-0270919196)  │
├─────────────────────────────────┤       ├─────────────────────────────────┤
│ • Staging Auth (Test phone OTP) │       │ • Real Customer Authentication  │
│ • Staging Database & Rules      │       │ • Live Customer & Payment Data  │
│ • Staging Storage Bucket        │       │ • Live KYC & Engineering Assets │
│ • Staging Cloud Functions       │       │ • Production Cloud Functions    │
└─────────────────────────────────┘       └─────────────────────────────────┘
```

### Strategy B: Controlled In-Project Staging (If Staging Project Denied)
If creating a separate GCP project is organizationally prohibited:
1. **Named Database Isolation:** Maintain staging data in a dedicated Firestore database ID (e.g. `staging-db`).
2. **Dedicated Storage Sub-Buckets:** Prefix all staging objects under `staging/`.
3. **Dedicated Function Namespaces:** Suffix Cloud Functions with `_stg` (e.g., `provisionStaff_stg`).
*Note: Strategy B is less secure because IAM permissions and Firebase Authentication users remain shared.*

---

## 4. AUDIT CONCLUSION

The staging environment configuration is **NOT READY** for direct deployment because an isolated staging project has not been provisioned. Attempting deployment to `gen-lang-client-0270919196` directly is categorized as a high-risk production write and is blocked.
