# HBX SOLAR ONE — Document Integrity, Provenance & Checksum Audit

## 1. Audit Scope & Verification Objective
Legal validity of solar proposals, net-metering applications, CEIG commissioning reports, and bank payment proofs depends on cryptographic integrity and non-repudiation.

This audit evaluates:
1. Hash algorithms: MD5 vs SHA-256.
2. File integrity verification routines.
3. Revision tracking and version incrementing.
4. Soft deletion enforcement vs statutory retention.
5. Append-only audit provenance and server-side compliance requirements.

---

## 2. Cryptographic Checksum Evaluation

### 2.1 The MD5 Vulnerability
Initial implementations relied on MD5 (`MessageDigest.getInstance("MD5")`).
- **Cryptographic Weakness**: MD5 is vulnerable to chosen-prefix collision attacks.
- **Regulatory Standard**: The National Cyber Security Policy of India, CERT-In guidelines, and Ministry of New and Renewable Energy (MNRE) audit standards specify SHA-256 or higher for digital documents supporting direct benefit transfer (DBT) subsidy disbursements under PM Surya Ghar Muft Bijli Yojana.

### 2.2 SHA-256 Implementation & Backward Compatibility
`FileProcessingUtil` was enhanced with:
1. **`calculateSHA256Checksum(file: File): String?`**:
   - Streams bytes through a 8192-byte buffer into a SHA-256 `MessageDigest`.
   - Generates a canonical 64-character lowercase hexadecimal hash.
2. **`verifyFileIntegrity(file: File, expectedChecksum: String): Boolean`**:
   - Inspects the length of `expectedChecksum`:
     - If length == 64: Verifies using SHA-256.
     - If length == 32: Verifies using MD5 (preserving backward compatibility for existing synced documents).
   - Returns `true` if and only if the calculated file hash matches the stored checksum.

---

## 3. Automated Tampering Detection Verification
Automated test `DocumentIntegrityTest.testFileIntegrityVerification_detectsTampering`:
1. Generated a sample DISCOM technical sanction specification PDF.
2. Computed its SHA-256 hash.
3. Confirmed unaltered file verified successfully (`assertTrue`).
4. Injected an unauthorized payload into the file.
5. Confirmed the integrity check detected the tampering and rejected the file (`assertFalse`).

---

## 4. Sequential Revision Tracking
When solar customers address rooftop shading by trimming trees or relocating water tanks, field technicians capture updated site evidence.

In `DocumentRepository.kt`:
```kotlin
val existingDocs = database.documentDao().getDocumentsForParent(parentEntityType, parentEntityId)
val highestVersion = existingDocs.filter { it.documentCategory == category.name }.maxOfOrNull { it.version } ?: 0
val nextVersion = highestVersion + 1
```
- New attachments for the same slot/category automatically increment the version (`v1 -> v2 -> v3`).
- All revisions remain archived in the SQLite database and Storage bucket for longitudinal project auditing.

---

## 5. Zero-Loss Soft Deletion & Audit Trail

### 5.1 Soft Deletion Guarantee
- Operating queries in `DocumentDao` enforce `isDeleted = 0`.
- Calling `softDeleteDocument` flags `isDeleted = 1`, records a mandatory `deletionReason`, and logs an event in `document_audit_logs`.
- Hard deletions (`deleteDocumentPermanent` or raw Storage deletions) are blocked by security rules (`allow delete: if false;`).

### 5.2 Immutable Audit Log Schema & Provenance Risk Analysis
Each audit event captures:
- `auditId`: Unique UUID.
- `documentId`: Target document reference.
- `actorUid`: Firebase Auth UID of the actor.
- `actorRole`: Staff role at time of action.
- `eventType`: `DOCUMENT_UPLOADED`, `DOCUMENT_VIEWED`, `DOCUMENT_DELETION_REQUESTED`, `SYNC_COMPLETED`.
- `timestamp`: Server-enforced timestamp.
- `details`: Contextual metadata (file size, justification, network state).

**Audit Provenance Risk**:
Firestore rules enforce `allow update, delete: if false;`, making audit logs immutable once written. However, because events are posted from client devices, a compromised client could fabricate details or roles. For full regulatory non-repudiation, production systems must supplement client logging with Firebase Cloud Functions triggers (Storage `onObjectFinalized` and Firestore `onDocumentCreated`) that execute in a trusted server environment.
