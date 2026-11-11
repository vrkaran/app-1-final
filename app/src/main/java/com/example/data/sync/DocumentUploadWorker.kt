package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.SyncStatus
import com.example.data.storage.FirebaseStorageDataSource
import com.example.data.storage.StorageUploadState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "HBXDocUploadWorker"
const val WORK_NAME_DOCUMENT_UPLOAD = "hbx_document_upload_work"
const val PERIODIC_WORK_NAME = "hbx_periodic_sync_work"

class DocumentUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getInstance(applicationContext)
    private val storageDS = FirebaseStorageDataSource()
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(applicationContext.getString(R.string.firestore_database_id))
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting DocumentUploadWorker execution...")

        val pendingDocs = database.documentDao().getPendingUploads()
        if (pendingDocs.isEmpty()) {
            Log.d(TAG, "No pending document uploads found.")
            return@withContext Result.success()
        }

        Log.d(TAG, "Found ${pendingDocs.size} pending document upload(s).")
        var failureCount = 0

        for (doc in pendingDocs) {
            try {
                processDocumentUpload(doc.documentId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed processing upload for document ${doc.documentId}: ${e.message}")
                failureCount++
            }
        }

        if (failureCount > 0) {
            Log.w(TAG, "Worker completed with $failureCount failures. Requesting retry.")
            Result.retry()
        } else {
            Log.d(TAG, "All pending documents processed successfully.")
            Result.success()
        }
    }

    private suspend fun processDocumentUpload(documentId: String) {
        val doc = database.documentDao().getDocumentById(documentId) ?: return
        val localFilePath = doc.localFilePath ?: ""
        val file = File(localFilePath)

        var downloadUrl = doc.downloadUrl ?: ""

        // Phase 1: Upload Storage bytes (if not already uploaded)
        if (doc.uploadStatus != "UPLOADED" && doc.uploadStatus != "METADATA_PENDING" && doc.uploadStatus != "COMPLETED") {
            if (!file.exists()) {
                Log.e(TAG, "Local file not found for doc $documentId: $localFilePath")
                database.documentDao().updateUploadStatus(documentId, "FAILED", SyncStatus.FAILED, System.currentTimeMillis())
                return
            }

            database.documentDao().updateUploadStatus(documentId, "UPLOADING", SyncStatus.SYNCING, System.currentTimeMillis())

            val customMetadata = mapOf(
                "documentId" to documentId,
                "businessId" to doc.businessId,
                "parentEntityType" to doc.parentEntityType,
                "parentEntityId" to doc.parentEntityId,
                "uploadedByUid" to doc.uploadedByUid,
                "category" to doc.documentCategory,
                "checksum" to (doc.checksum ?: "")
            )

            storageDS.uploadFileWithProgress(doc.storagePath, file, doc.mimeType, customMetadata).collect { state ->
                when (state) {
                    is StorageUploadState.Success -> {
                        downloadUrl = state.downloadUrl
                        database.documentDao().updateUploadSuccess(
                            id = documentId,
                            status = "METADATA_PENDING",
                            storagePath = doc.storagePath,
                            downloadUrl = downloadUrl,
                            syncStatus = SyncStatus.PENDING_UPLOAD,
                            time = System.currentTimeMillis()
                        )
                    }
                    is StorageUploadState.Error -> {
                        database.documentDao().updateUploadStatus(documentId, "FAILED", SyncStatus.FAILED, System.currentTimeMillis())
                        throw state.exception
                    }
                    else -> {}
                }
            }
        }

        // Phase 2: Index Firestore Metadata (Idempotent)
        val metadataMap = hashMapOf<String, Any>(
            "documentId" to documentId,
            "businessId" to doc.businessId,
            "parentEntityType" to doc.parentEntityType,
            "parentEntityId" to doc.parentEntityId,
            "documentCategory" to doc.documentCategory,
            "documentType" to doc.documentType,
            "originalFileName" to doc.originalFileName,
            "storagePath" to doc.storagePath,
            "downloadUrl" to downloadUrl,
            "mimeType" to doc.mimeType,
            "fileSizeBytes" to doc.fileSizeBytes,
            "uploadedByUid" to doc.uploadedByUid,
            "uploadedByName" to doc.uploadedByName,
            "uploadedByRole" to doc.uploadedByRole,
            "uploadedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "checksum" to (doc.checksum ?: ""),
            "version" to doc.version,
            "isDeleted" to doc.isDeleted
        )

        firestore.collection("documents").document(documentId).set(metadataMap).await()

        database.documentDao().updateUploadSuccess(
            id = documentId,
            status = "COMPLETED",
            storagePath = doc.storagePath,
            downloadUrl = downloadUrl,
            syncStatus = SyncStatus.SYNCED,
            time = System.currentTimeMillis()
        )

        // Dequeue from sync queue if present
        database.syncQueueDao().removeOperation(documentId)
        Log.d(TAG, "Document $documentId successfully verified and synced via WorkManager.")
    }

    companion object {
        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DocumentUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag("document_upload")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_DOCUMENT_UPLOAD,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<DocumentUploadWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("periodic_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
