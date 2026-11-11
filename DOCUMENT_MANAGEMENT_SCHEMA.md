# HBX SOLAR ONE — Document Management Schema

## 1. Architecture Overview
The document management layer is designed with strict dual-tier consistency across Room SQLite (local persistence) and Cloud Firestore (cloud synchronization and indexing).

---

## 2. Room SQLite Schema (`AppDatabase` Version 2)

### 2.1 Table: `documents`
Represents the local source of truth for all operational files and attachments.

```sql
CREATE TABLE IF NOT EXISTS `documents` (
    `documentId` TEXT NOT NULL PRIMARY KEY,
    `businessId` TEXT NOT NULL,
    `parentEntityType` TEXT NOT NULL,
    `parentEntityId` TEXT NOT NULL,
    `documentCategory` TEXT NOT NULL,
    `documentType` TEXT NOT NULL,
    `originalFileName` TEXT NOT NULL,
    `storageFileName` TEXT NOT NULL,
    `storagePath` TEXT NOT NULL,
    `localFilePath` TEXT,
    `mimeType` TEXT NOT NULL,
    `fileExtension` TEXT NOT NULL,
    `fileSizeBytes` INTEGER NOT NULL,
    `uploadedByUid` TEXT NOT NULL,
    `uploadedByName` TEXT NOT NULL,
    `uploadedByRole` TEXT NOT NULL,
    `uploadedAt` INTEGER NOT NULL,
    `updatedAt` INTEGER NOT NULL,
    `uploadStatus` TEXT NOT NULL,
    `syncStatus` TEXT NOT NULL,
    `checksum` TEXT NOT NULL,
    `thumbnailPath` TEXT,
    `description` TEXT NOT NULL,
    `isDeleted` INTEGER NOT NULL DEFAULT 0,
    `deletedAt` INTEGER,
    `deletedByUid` TEXT,
    `version` INTEGER NOT NULL DEFAULT 1,
    `downloadUrl` TEXT
);

CREATE INDEX IF NOT EXISTS `index_documents_parent` ON `documents` (`parentEntityType`, `parentEntityId`);
CREATE INDEX IF NOT EXISTS `index_documents_category` ON `documents` (`documentCategory`);
CREATE INDEX IF NOT EXISTS `index_documents_uploadStatus` ON `documents` (`uploadStatus`);
CREATE INDEX IF NOT EXISTS `index_documents_syncStatus` ON `documents` (`syncStatus`);
```

### 2.2 Table: `document_audit_logs`
An append-only immutable audit trail recording every state mutation, soft deletion, upload, and download action.

```sql
CREATE TABLE IF NOT EXISTS `document_audit_logs` (
    `logId` TEXT NOT NULL PRIMARY KEY,
    `documentId` TEXT NOT NULL,
    `action` TEXT NOT NULL,
    `actorUid` TEXT NOT NULL,
    `actorName` TEXT NOT NULL,
    `actorRole` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `detailsJson` TEXT NOT NULL,
    `syncStatus` TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS `index_audit_documentId` ON `document_audit_logs` (`documentId`);
```

---

## 3. Cloud Firestore Schema

### 3.1 Collection: `/documents/{documentId}`
Primary metadata index synced from client devices.
```json
{
  "documentId": "DOC-7701",
  "businessId": "PRJ-102",
  "parentEntityType": "PROJECT",
  "parentEntityId": "PRJ-102",
  "documentCategory": "SITE_PHOTO",
  "documentType": "HOUSE_FRONT",
  "originalFileName": "front_elevation.jpg",
  "storageFileName": "site_photo_PRJ-102_1762851600_a1b2c3.jpg",
  "storagePath": "projects/PRJ-102/site-photos/site_photo_PRJ-102_1762851600_a1b2c3.jpg",
  "mimeType": "image/jpeg",
  "fileExtension": "jpg",
  "fileSizeBytes": 1452800,
  "uploadedByUid": "user_bde_01",
  "uploadedByName": "Rahul Kumar",
  "uploadedByRole": "BDE",
  "uploadedAt": 1762851600000,
  "updatedAt": 1762851600000,
  "uploadStatus": "UPLOADED",
  "checksum": "e2fc714c4727ee9395f324cd2e7f331f",
  "thumbnailPath": "thumbnails/site_photo_PRJ-102_1762851600_a1b2c3_thumb.jpg",
  "description": "Daylight front elevation with GPS geotag verified",
  "isDeleted": false,
  "deletedAt": null,
  "deletedByUid": null,
  "version": 1,
  "downloadUrl": "https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0270919196.firebasestorage.app/o/..."
}
```

### 3.2 Collection: `/document_audit_logs/{logId}`
Immutable audit collection for legal compliance and DISCOM dispute resolution.
```json
{
  "logId": "AUD-99120",
  "documentId": "DOC-7701",
  "action": "UPLOAD",
  "actorUid": "user_bde_01",
  "actorName": "Rahul Kumar",
  "actorRole": "BDE",
  "timestamp": 1762851600000,
  "detailsJson": "{\"storagePath\":\"projects/PRJ-102/...\",\"fileSize\":1452800,\"checksum\":\"e2fc...\"}"
}
```
