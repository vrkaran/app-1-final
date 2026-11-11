# HBX SOLAR ONE — STAGING ROLLBACK & DISASTER RECOVERY PLAN

**Component:** Cloud Functions, Firestore Security Rules, Cloud Storage Security Rules  
**Scope:** Staging & Production Deployment Emergency Rollbacks  
**Author:** Principal Firebase DevOps Engineer  
**Date:** November 11, 2026  

---

## 1. ROLLBACK OVERVIEW & GUIDING PRINCIPLES

In enterprise zero-trust deployments, any post-release anomaly (e.g., unexpected rule denials, function timeout spikes, or credential issues) requires an immediate, deterministic rollback mechanism.

### Fundamental Safety Principles:
1. **Never Automatically Delete Resources:** Deleting functions or collections destroys state and audit trails. Rollbacks must revert code and rule configurations to known-good revisions.
2. **Deterministic Version History:** All previous rules releases and Cloud Run revisions are retained in Google Cloud history.
3. **Decoupled Service Rollbacks:** Cloud Functions, Firestore Rules, and Storage Rules can and must be rolled back independently based on the failure domain.

---

## 2. COMPONENT-SPECIFIC ROLLBACK PROCEDURES

### Component A: Cloud Functions (v2 on Cloud Run)

#### Rollback Method 1: Instant Traffic Re-Routing via Cloud Run (Recommended - Zero Downtime)
Because Cloud Functions v2 runs on Google Cloud Run, deploying a new function creates an immutable Revision. Traffic can be shifted back to the previous stable revision in seconds without redeploying code.

```bash
TARGET_PROJECT="hbxsolar-staging-10293"

# 1. List revisions for the affected service (e.g., provisionstaff)
gcloud run revisions list \
  --service=provisionstaff \
  --region=asia-south1 \
  --project="${TARGET_PROJECT}"

# 2. Shift 100% of traffic back to the previous stable revision
gcloud run services update-traffic provisionstaff \
  --to-revisions=PREVIOUS_STABLE_REVISION=100 \
  --region=asia-south1 \
  --project="${TARGET_PROJECT}"
```
- **Execution Time:** ~5 seconds.
- **Impact Analysis:**
  - In-flight requests on the failed revision drain gracefully.
  - Subsequent requests instantly route to the previous stable revision.
  - Zero data loss in Firestore.

#### Rollback Method 2: Git Code Reversion & Re-Deploy
If the Cloud Run revision is corrupted:
```bash
git checkout HEAD~1 -- functions/
npm --prefix functions run build
firebase deploy --only functions --project "${TARGET_PROJECT}"
```

---

### Component B: Cloud Firestore Security Rules

#### Rollback Procedure:
Firestore maintains a revision history of all deployed rule sets in Google Cloud.

```bash
TARGET_PROJECT="hbxsolar-staging-10293"

# 1. View previous rules release names
firebase firestore:rules:releases --project "${TARGET_PROJECT}"

# 2. Rollback to the previous release snapshot
git checkout HEAD~1 -- firestore.rules
firebase deploy --only firestore:rules --project "${TARGET_PROJECT}"
```

#### Impact Analysis:
- **Propagation Time:** Instantaneous across all globally distributed database nodes (< 10 seconds).
- **Client Impact:** Mobile clients immediately evaluate queries under the reverted rule set. No mobile app restart or rebuild is required.
- **Data Integrity:** No data is modified or deleted by a rules rollback.

---

### Component C: Cloud Storage Security Rules

#### Rollback Procedure:
```bash
TARGET_PROJECT="hbxsolar-staging-10293"

# Revert storage.rules and redeploy
git checkout HEAD~1 -- storage.rules
firebase deploy --only storage:rules --project "${TARGET_PROJECT}"
```

#### Impact Analysis:
- **Propagation Time:** ~15–30 seconds globally.
- **Client Impact:** Upload and download permissions immediately revert to previous constraints.
- **Data Integrity:** Existing files in the Google Cloud Storage bucket are untouched.

---

## 3. INCIDENT CLASSIFICATION & DECISION TREE

```
┌─────────────────────────────────┐
│        INCIDENT DETECTED        │
└────────────────┬────────────────┘
                 │
       Is it a Rules Failure?
       (e.g., 7 PERMISSION_DENIED)
       ├─────────► YES: Revert firestore.rules / storage.rules (Step 2.B / 2.C)
       │                Propagation: < 30 seconds
       ▼ NO
       Is it a Cloud Function Error?
       (e.g., 500 INTERNAL / Timeout)
       ├─────────► YES: Shift traffic to previous Cloud Run revision (Step 2.A)
       │                Propagation: < 5 seconds
       ▼ NO
       Is it an Auth / Token Issue?
       └─────────► Check Admin SDK Custom Claims minting in Cloud Logs
```

---

## 4. EMERGENCY CONTACTS & ESCALATION PATH

In the event of an unresolvable rollback failure in staging or production:
1. **Immediate Action:** Set Cloud Functions `minInstances: 0` and route traffic to a maintenance response.
2. **Escalate to:** Principal Cloud Infrastructure Architect & Security Officer.
3. **Audit Log Inspection:** Query `/security_audit_logs/` in Firestore to inspect all recent administrative operations and identifying actors.
