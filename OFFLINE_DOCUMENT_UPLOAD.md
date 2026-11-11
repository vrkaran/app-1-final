# HBX SOLAR ONE — Offline-First Document Upload & Synchronization Engine

## 1. The Rural Connectivity Challenge
Solar rooftop installations across Uttar Pradesh (Azamgarh, Mau, Jaunpur, Ghazipur, Ballia, and Varanasi) frequently operate in environments with intermittent 2G/3G signals, high network latency, or total blackout conditions.

HBX SOLAR ONE addresses this through a robust **offline-first local staging and resumable queueing architecture**.

---

## 2. Architecture & Components

### 2.1 Local Private Sandbox Storage
When a field technician captures a daylight site evidence photograph or attaches a payment slip:
1. The raw input stream is decoded and downscaled via `FileProcessingUtil` to a maximum dimension of 1600px with 82% JPEG quality.
2. An MD5 checksum is computed synchronously from the compressed byte array.
3. The processed bytes are written to the app's internal sandboxed storage:
   `context.filesDir.resolve("documents/$storageFileName")`
4. A companion 300px thumbnail is generated and written to:
   `context.filesDir.resolve("documents/thumbnails/$thumbName")`
5. The local file is accessible immediately within the UI without waiting for network connectivity.

### 2.2 Room Document Queueing
A `DocumentEntity` is inserted into the local SQLite database with:
- `uploadStatus = "QUEUED"`
- `syncStatus = "PENDING_UPLOAD"`
- `localFilePath = file.absolutePath`

Concurrently, an operation record is committed to `SyncQueueDao`:
- `entityType = "DOCUMENT"`
- `operationType = "UPLOAD"`
- `entityId = documentId`
- `payloadJson = { storagePath, localFilePath, mimeType, checksum, ... }`

### 2.3 Network Monitoring & Synchronization Triggering
The `SyncEngine` registers a network callback with Android's `ConnectivityManager`:
- When connectivity transitions to `ONLINE` (WiFi or Cellular):
  1. `SyncEngine` polls `documentDao.getPendingUploads()`.
  2. For each pending item, `FirebaseStorageDataSource.uploadFileFlow()` initiates a resumable upload task.
  3. Real-time progress (0% to 100%) is reported via Kotlin Flow to update progress bars on active screens (such as `SiteSurveyScreen`'s upload status indicator).
  4. On upload completion, the storage download URL is retrieved.
  5. The metadata is written to Cloud Firestore collection `/documents/{documentId}`.
  6. The local Room entity is transitioned to `uploadStatus = "UPLOADED"`, `syncStatus = "SYNCED"`, and `downloadUrl = url`.
  7. The operation is dequeued from `SyncQueueDao`.

---

## 3. Conflict Resolution & Network Flapping
- **Idempotent Storage Paths**: Storage file names incorporate unique timestamps and UUIDs (`site_photo_PRJ-102_1762851600_a1b2c3.jpg`). Retries never corrupt existing objects.
- **Resumable Upload Sessions**: Firebase Storage's `UploadTask` transparently resumes bytes transferred during temporary signal loss.
- **Offline Cache Integrity**: If the app process terminates while uploading, the file remains safely preserved in `context.filesDir`. On next launch, `SyncEngine` detects the `PENDING_UPLOAD` status and resumes synchronization.
