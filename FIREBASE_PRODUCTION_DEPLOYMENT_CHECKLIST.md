# HBX SOLAR ONE — FIREBASE PRODUCTION DEPLOYMENT CHECKLIST & STAGING RUNBOOK

**Target Project:** `gen-lang-client-0270919196`  
**Application ID:** `com.aistudio.hbxsolar.krvxmp`  
**Backend Framework:** Cloud Functions for Firebase (v2, Node.js 20, TypeScript)  
**Security Level:** Zero-Trust Enterprise Solar Infrastructure  
**Document Purpose:** Staged pre-deployment verification, configuration, IAM checklist, deployment commands, and rollback procedures.  

> **CRITICAL POLICY NOTICE:**  
> In compliance with strict Phase 3.3.1 guidelines, **NO production deployment has been executed by this audit**. This checklist provides the validated, immutable runbook for the operations team to execute deployment to controlled staging/production environments.

---

## 1. PRE-DEPLOYMENT PREREQUISITES

### A. Billing & Plan Verification
- [ ] **Blaze (Pay-as-you-go) Plan:** Confirm that project `gen-lang-client-0270919196` has an active Google Cloud Billing Account linked. Cloud Functions (Node.js 20 runtime on Cloud Run) strictly require the Blaze plan.
  ```bash
  gcloud billing projects describe gen-lang-client-0270919196
  ```

### B. Required Google Cloud APIs
Ensure the following APIs are enabled in project `gen-lang-client-0270919196`:
- [ ] Cloud Functions API (`cloudfunctions.googleapis.com`)
- [ ] Cloud Build API (`cloudbuild.googleapis.com`)
- [ ] Cloud Run Admin API (`run.googleapis.com`)
- [ ] Artifact Registry API (`artifactregistry.googleapis.com`)
- [ ] Cloud Logging API (`logging.googleapis.com`)
- [ ] Identity and Access Management (IAM) API (`iam.googleapis.com`)
- [ ] Cloud Storage API (`storage.googleapis.com`)

Enabling command:
```bash
gcloud services enable \
  cloudfunctions.googleapis.com \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  logging.googleapis.com \
  iam.googleapis.com \
  storage.googleapis.com \
  --project gen-lang-client-0270919196
```

### C. IAM Roles & Service Account Permissions
The default Cloud Functions / App Engine Service Account (typically `gen-lang-client-0270919196@appspot.gserviceaccount.com` or compute service account) requires:
- [ ] `roles/firebase.admin`: For reading/writing Firestore, managing Auth custom claims, and token revocation.
- [ ] `roles/storage.admin`: For managing Storage objects.
- [ ] `roles/iam.serviceAccountTokenCreator`: **Mandatory for v4 Signed URL Generation**. Cloud Functions generating signed URLs via the Google Cloud Storage SDK require token creator permissions on their own service account.

Grant command:
```bash
PROJECT_ID="gen-lang-client-0270919196"
SERVICE_ACCOUNT="${PROJECT_ID}@appspot.gserviceaccount.com"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/iam.serviceAccountTokenCreator"
```

---

## 2. STAGED DEPLOYMENT SEQUENCE

### Step 1: Pre-Deployment Build Verification
Compile TypeScript functions and execute local test suites:
```bash
# In functions directory
npm --prefix functions run build
npm --prefix functions test

# Root security rules tests
node --test firestore.test.js
node --test storage.test.js
```
*Gate: Proceed only if all tests return exit code 0.*

### Step 2: Deploy Cloud Firestore Rules & Indexes
Deploy Firestore security rules and composite indexes:
```bash
firebase deploy \
  --only firestore:rules,firestore:indexes \
  --project gen-lang-client-0270919196
```

### Step 3: Deploy Cloud Storage Security Rules
Deploy Cloud Storage security rules:
```bash
firebase deploy \
  --only storage:rules \
  --project gen-lang-client-0270919196
```

### Step 4: Deploy Cloud Functions (v2)
Deploy trusted backend functions:
```bash
firebase deploy \
  --only functions \
  --project gen-lang-client-0270919196
```

Functions deployed:
1. `provisionStaff` (`https.onCall`, region: `asia-south1` or `us-central1`)
2. `onUserAccountStatusChanged` (`firestore.onDocumentUpdated("users/{uid}")`)
3. `getSecureStorageUrl` (`https.onCall`)

---

## 3. POST-DEPLOYMENT SANITY CHECKS

1. **Verify Security Rules In Effect:**
   - In Firebase Console > Firestore Database > Rules, confirm rules version matches `/firestore.rules` containing `isStaff()` and `isCallerActive()`.
   - In Firebase Console > Storage > Rules, confirm rules version matches `/storage.rules`.

2. **Verify Cloud Functions Health:**
   - Check Google Cloud Console > Cloud Run / Cloud Functions for green operational status.
   - Test `provisionStaff` callable endpoint with an unauthenticated curl to verify `UNAUTHENTICATED` error response (confirming endpoint is reachable and rejecting unauthenticated calls).

3. **Verify Initial SUPER_ADMIN Provisioning:**
   - A single root bootstrap seed for the initial `SUPER_ADMIN` must be created via Firebase Admin SDK script in Google Cloud Shell (never from client UI).

---

## 4. ROLLBACK & DISASTER RECOVERY PROCEDURES

In the event of an unexpected regression or deployment disruption:

### Quick Rollback: Rules
```bash
# Rollback Firestore rules to previous release via Firebase CLI
firebase firestore:rules:releases --project gen-lang-client-0270919196
# Re-deploy backup rules snapshot
firebase deploy --only firestore:rules --project gen-lang-client-0270919196
```

### Quick Rollback: Cloud Functions
```bash
# Cloud Functions v2 runs on Cloud Run; traffic can be instantly rolled back to the previous revision
gcloud run services update-traffic provisionstaff \
  --to-revisions=PREVIOUS_REVISION=100 \
  --project gen-lang-client-0270919196
```
