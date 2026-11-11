# HBX SOLAR ONE — STAGING DEPLOYMENT RUNBOOK

**Target Environment:** Isolated Firebase Staging Environment  
**Recommended Staging Project ID:** `hbxsolar-staging` (or `gen-lang-client-0270919196-stg`)  
**Production Project ID (FROZEN — DO NOT USE FOR STAGING):** `gen-lang-client-0270919196`  
**Author:** Principal Firebase DevOps Engineer  
**Document Status:** PENDING INFRASTRUCTURE PROVISIONING  

> **CRITICAL POLICY DIRECTIVE:**  
> **DO NOT DEPLOY TO `gen-lang-client-0270919196` WITHOUT EXPLICIT WRITTEN APPROVAL.**  
> Deploying staging code or test data to the production project risks overwriting production rules, corrupting live customer profiles, and violating data privacy boundaries.

---

## 1. STAGING PROJECT PROVISIONING WORKFLOW

Before executing any deployment command, the Cloud Administrator must provision an isolated staging project via the Google Cloud / Firebase Console.

### Step 1.1: Create Staging Project
1. Navigate to [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and name it `hbxsolar-staging`.
3. Disable Google Analytics for staging (optional).
4. Note the generated Project ID (e.g., `hbxsolar-staging-10293`).

### Step 1.2: Enable Blaze Billing (Mandatory for Cloud Functions)
1. In Firebase Console, upgrade the project to the **Blaze (Pay-as-you-go)** plan.
2. Link a valid corporate billing account.
3. Set a Google Cloud Budget Alert (e.g., ₹1,000 / $15) to prevent accidental charges during testing.

### Step 1.3: Enable Required Google Cloud APIs
From Google Cloud Shell or a workstation with `gcloud` installed:
```bash
STAGING_PROJECT="hbxsolar-staging-10293"

gcloud services enable \
  cloudfunctions.googleapis.com \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  logging.googleapis.com \
  iam.googleapis.com \
  storage.googleapis.com \
  firestore.googleapis.com \
  identitytoolkit.googleapis.com \
  --project "${STAGING_PROJECT}"
```

### Step 1.4: Configure IAM Roles
Grant token signing capabilities to the Cloud Functions service account:
```bash
STAGING_PROJECT="hbxsolar-staging-10293"
SERVICE_ACCOUNT="${STAGING_PROJECT}@appspot.gserviceaccount.com"

gcloud projects add-iam-policy-binding "${STAGING_PROJECT}" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/iam.serviceAccountTokenCreator"
```

### Step 1.5: Register Staging Android Application
1. In Firebase Console > Project Settings > General, click **Add App** > **Android**.
2. Package Name: `com.aistudio.hbxsolar.krvxmp`
3. Download the generated `google-services.json` and save as `app/src/staging/google-services.json`.

---

## 2. STAGED DEPLOYMENT EXECUTION SEQUENCE

Once the staging project is provisioned, execute the following commands strictly in order:

### Step 2.1: Initialize Staging Project Alias
In the project root, create `.firebaserc`:
```json
{
  "projects": {
    "default": "gen-lang-client-0270919196",
    "staging": "hbxsolar-staging-10293"
  }
}
```

### Step 2.2: Compile Backend TypeScript
```bash
npm --prefix functions install
npm --prefix functions run build
```
*Verification Check: Confirm `functions/lib/index.js` exists and exit code is 0.*

### Step 2.3: Deploy Security Rules to Staging
```bash
firebase deploy \
  --only firestore:rules,storage:rules \
  --project staging
```
*Verification Check: Visit Firebase Console > Firestore Database > Rules and verify rules match `firestore.rules`.*

### Step 2.4: Deploy Cloud Functions to Staging
```bash
firebase deploy \
  --only functions \
  --project staging
```
*Functions Deployed:*
- `provisionStaff` (`https.onCall`, region: `asia-south1`)
- `onUserAccountStatusChanged` (`firestore.onDocumentUpdated("users/{uid}")`)
- `getSecureStorageUrl` (`https.onCall`, region: `asia-south1`)

---

## 3. POST-DEPLOYMENT STAGING SANITY TESTS

Execute these non-destructive validation tests against the newly deployed staging environment:

1. **Unauthenticated Callable Invalidation:**
   Invoke `provisionStaff` without an authorization header:
   ```bash
   curl -X POST https://asia-south1-hbxsolar-staging-10293.cloudfunctions.net/provisionStaff \
     -H "Content-Type: application/json" \
     -d '{"data": {"targetUid": "dummy", "role": "BDE"}}'
   ```
   *Expected Response: HTTP 401 or `{"error": {"status": "UNAUTHENTICATED"}}`.*

2. **Root SUPER_ADMIN Bootstrap:**
   Execute a one-time Admin SDK script in Google Cloud Shell to grant the first administrator role:
   ```typescript
   // scripts/bootstrapSuperAdmin.ts
   import * as admin from 'firebase-admin';
   admin.initializeApp({ projectId: 'hbxsolar-staging-10293' });
   await admin.auth().setCustomUserClaims('STAGING_ADMIN_UID', {
     role: 'SUPER_ADMIN',
     accountStatus: 'ACTIVE'
   });
   ```

3. **Storage Signed URL Issuance:**
   Verify that `getSecureStorageUrl` generates valid v4 URLs with 300s expiration for authorized users.
