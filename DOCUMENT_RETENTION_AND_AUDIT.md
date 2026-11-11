# HBX SOLAR ONE — Document Retention, Soft Deletion & Audit Trail Architecture

## 1. Regulatory Context & Compliance Mandate
Residential and commercial solar projects sanctioned under the **PM Surya Ghar Muft Bijli Yojana** and state utility (UPPCL / PuVVNL) net-metering schemes are subject to statutory verification audits by Discom inspectors, the Ministry of New and Renewable Energy (MNRE), and government banking partners for a minimum of five (5) to twenty-five (25) years.

Accidental or malicious deletion of site photos, electricity bills, or signed EPC agreements creates severe legal and financial liability.

---

## 2. Zero-Loss Soft Deletion Architecture

### 2.1 Enforcement at Storage & Firestore Levels
- **Storage Rules Constraint**: The Firebase Storage rules deliberately omit any `delete` permissions for client connections. Raw objects residing in `gen-lang-client-0270919196.firebasestorage.app` cannot be unlinked or erased by client apps.
- **Firestore Security Rules**: The `/documents/{documentId}` collection explicitly rejects hard deletes:
  ```javascript
  match /documents/{documentId} {
    allow delete: if false; // Hard deletion blocked
  }
  ```
- **Soft Deletion Mutation**: A deletion operation updates the metadata document:
  ```json
  {
    "isDeleted": true,
    "deletedAt": 1762852000000,
    "deletedByUid": "user_admin_01",
    "updatedAt": 1762852000000
  }
  ```

### 2.2 Room Database Query Filtering
All UI and operational queries in `DocumentDao` enforce the soft-delete filter:
```sql
SELECT * FROM documents 
WHERE parentEntityType = :parentType 
  AND parentEntityId = :parentId 
  AND isDeleted = 0 
ORDER BY uploadedAt DESC
```
This guarantees deleted documents immediately disappear from active field workflows while remaining permanently queryable by Super Admins and compliance auditors in dedicated compliance logs.

---

## 3. Append-Only Audit Logging (`document_audit_logs`)
Every document event triggers an immutable entry in the `document_audit_logs` table (and synced to Firestore `/document_audit_logs/{logId}`):

### 3.1 Tracked Audit Events
1. `UPLOAD`: Initial capture and storage staging.
2. `VIEW`: Inspection in `DocumentViewerModal`.
3. `DOWNLOAD`: Local export to storage.
4. `SOFT_DELETE`: Mark document as deleted with actor identity and deletion justification.
5. `SYNC`: Transition from offline queue to synced cloud storage.

### 3.2 Audit Log Record Structure
```kotlin
data class DocumentAuditLog(
    val logId: String,          // e.g. "AUD-1762851600-01"
    val documentId: String,     // Target document reference
    val action: String,         // UPLOAD | VIEW | DOWNLOAD | SOFT_DELETE | SYNC
    val actorUid: String,       // Firebase Auth UID of technician
    val actorName: String,      // Full staff name
    val actorRole: String,      // BDE | INSTALLATION_ENGINEER | ADMIN
    val timestamp: Long,        // System epoch milliseconds
    val detailsJson: String     // Contextual payload (MD5, reason, network state)
)
```
Firestore security rules permit `allow create: if isAuthenticated();` on `/document_audit_logs/{logId}` but forbid all `update` and `delete` operations (`allow update, delete: if false;`), ensuring tamper-proof legal integrity.
