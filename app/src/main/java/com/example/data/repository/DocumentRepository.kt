package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DocumentAuditEntity
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.SyncOperationEntity
import com.example.data.local.entities.SyncStatus
import com.example.data.storage.FirebaseStorageDataSource
import com.example.data.storage.StorageUploadState
import com.example.model.DocumentCategory
import com.example.model.DocumentItem
import com.example.model.StoragePathBuilder
import com.example.model.UserProfile
import com.example.util.FileProcessingUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val TAG = "HBXDocRepository"

class DocumentRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val storageDataSource: FirebaseStorageDataSource = FirebaseStorageDataSource(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
) {

    fun getDocumentsForParentFlow(parentType: String, parentId: String): Flow<List<DocumentItem>> {
        return database.documentDao().getDocumentsForParentFlow(parentType, parentId).map { entities ->
            entities.map { DocumentItem.fromEntity(it) }
        }
    }

    fun getAllDocumentsFlow(): Flow<List<DocumentItem>> {
        return database.documentDao().getAllDocumentsFlow().map { entities ->
            entities.map { DocumentItem.fromEntity(it) }
        }
    }

    fun getDocumentsByCategoryFlow(category: DocumentCategory): Flow<List<DocumentItem>> {
        return database.documentDao().getDocumentsByCategoryFlow(category.name).map { entities ->
            entities.map { DocumentItem.fromEntity(it) }
        }
    }

    suspend fun getDocumentById(documentId: String): DocumentItem? = withContext(Dispatchers.IO) {
        val entity = database.documentDao().getDocumentById(documentId) ?: return@withContext null
        DocumentItem.fromEntity(entity)
    }

    /**
     * Stage 1 of Upload Workflow:
     * Validates, processes, caches locally, writes to Room, enqueues to sync queue,
     * and attempts immediate upload if network is available.
     */
    suspend fun enqueueDocumentUpload(
        fileUri: Uri,
        originalFileName: String,
        category: DocumentCategory,
        parentEntityType: String,
        parentEntityId: String,
        businessId: String,
        description: String?,
        userProfile: UserProfile,
        onProgress: ((Float) -> Unit)? = null
    ): DocumentItem = withContext(Dispatchers.IO) {
        val documentId = "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        val fileExtension = originalFileName.substringAfterLast(".", "jpg")
        val mimeType = FileProcessingUtil.getMimeTypeFromExtension(fileExtension)

        val uniqueFileName = StoragePathBuilder.generateUniqueStorageFileName(
            documentType = category.name,
            businessId = businessId,
            extension = fileExtension
        )

        // 1. Copy to private storage
        val localFile = FileProcessingUtil.copyUriToPrivateFile(context, fileUri, uniqueFileName)
            ?: throw IllegalStateException("Failed copying file to secure local sandbox storage.")

        // 2. Image optimization & thumbnail generation (if image)
        val isImage = mimeType.startsWith("image/")
        val finalFile = if (isImage) {
            FileProcessingUtil.optimizeImageFile(context, localFile)
        } else {
            localFile
        }

        val thumbnailPath = if (isImage) {
            FileProcessingUtil.generateThumbnail(context, finalFile)
        } else null

        val checksum = FileProcessingUtil.calculateMD5Checksum(finalFile)
        val storagePath = StoragePathBuilder.buildPath(
            parentType = parentEntityType,
            parentId = parentEntityId,
            category = category,
            sanitizedFileName = uniqueFileName
        )

        val now = System.currentTimeMillis()

        // 3. Create Room entity in QUEUED status
        val entity = DocumentEntity(
            documentId = documentId,
            businessId = businessId,
            parentEntityType = parentEntityType,
            parentEntityId = parentEntityId,
            documentCategory = category.name,
            documentType = category.displayName,
            originalFileName = originalFileName,
            storageFileName = uniqueFileName,
            storagePath = storagePath,
            localFilePath = finalFile.absolutePath,
            mimeType = mimeType,
            fileExtension = fileExtension,
            fileSizeBytes = finalFile.length(),
            uploadedByUid = userProfile.uid,
            uploadedByName = userProfile.fullName,
            uploadedByRole = userProfile.role,
            uploadedAt = now,
            updatedAt = now,
            uploadStatus = "QUEUED",
            syncStatus = SyncStatus.PENDING_UPLOAD,
            checksum = checksum,
            thumbnailPath = thumbnailPath,
            description = description,
            isDeleted = false,
            version = 1,
            downloadUrl = null
        )

        database.documentDao().upsertDocument(entity)

        // 4. Record audit entry
        recordAuditLog(
            eventType = "DOCUMENT_UPLOADED",
            documentId = documentId,
            businessEntityId = businessId,
            actorUid = userProfile.uid,
            actorRole = userProfile.role,
            details = "Initial upload queued: $originalFileName (${entity.fileSizeBytes} bytes)"
        )

        // 5. Enqueue persistent sync operation
        val payloadJson = JSONObject().apply {
            put("documentId", documentId)
            put("businessId", businessId)
            put("parentEntityType", parentEntityType)
            put("parentEntityId", parentEntityId)
            put("documentCategory", category.name)
            put("documentType", category.displayName)
            put("originalFileName", originalFileName)
            put("storageFileName", uniqueFileName)
            put("storagePath", storagePath)
            put("localFilePath", finalFile.absolutePath)
            put("mimeType", mimeType)
            put("fileExtension", fileExtension)
            put("fileSizeBytes", finalFile.length())
            put("checksum", checksum ?: "")
            put("description", description ?: "")
        }

        val syncOp = SyncOperationEntity(
            operationId = UUID.randomUUID().toString(),
            entityType = "DOCUMENT",
            entityId = documentId,
            operationType = "CREATE",
            payloadJson = payloadJson.toString(),
            createdAt = now,
            updatedAt = now,
            userId = userProfile.uid,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        database.syncQueueDao().enqueue(syncOp)

        // 6. Attempt immediate execution if possible
        try {
            uploadDocumentDirect(documentId, finalFile, storagePath, mimeType, entity, userProfile, onProgress)
        } catch (e: Exception) {
            Log.w(TAG, "Immediate upload deferred for offline background sync: ${e.message}")
        }

        DocumentItem.fromEntity(database.documentDao().getDocumentById(documentId) ?: entity)
    }

    suspend fun uploadDocumentDirect(
        documentId: String,
        localFile: File,
        storagePath: String,
        mimeType: String,
        entity: DocumentEntity,
        userProfile: UserProfile,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (!localFile.exists()) {
            database.documentDao().updateUploadStatus(documentId, "FAILED", SyncStatus.FAILED, System.currentTimeMillis())
            return@withContext
        }

        database.documentDao().updateUploadStatus(documentId, "UPLOADING", SyncStatus.SYNCING, System.currentTimeMillis())

        val customMetadata = mapOf(
            "documentId" to documentId,
            "businessId" to entity.businessId,
            "parentEntityType" to entity.parentEntityType,
            "parentEntityId" to entity.parentEntityId,
            "uploadedByUid" to userProfile.uid,
            "category" to entity.documentCategory
        )

        var downloadUrl = ""

        storageDataSource.uploadFileWithProgress(storagePath, localFile, mimeType, customMetadata).collect { state ->
            when (state) {
                is StorageUploadState.Progress -> {
                    onProgress?.invoke(state.fraction)
                }
                is StorageUploadState.Success -> {
                    downloadUrl = state.downloadUrl
                    database.documentDao().updateUploadSuccess(
                        id = documentId,
                        status = "METADATA_PENDING",
                        storagePath = storagePath,
                        downloadUrl = downloadUrl,
                        syncStatus = SyncStatus.PENDING_UPLOAD,
                        time = System.currentTimeMillis()
                    )
                }
                is StorageUploadState.Error -> {
                    database.documentDao().updateUploadStatus(documentId, "FAILED", SyncStatus.FAILED, System.currentTimeMillis())
                    throw state.exception
                }
            }
        }

        // Write Firestore metadata
        val metadataMap = hashMapOf<String, Any>(
            "documentId" to documentId,
            "businessId" to entity.businessId,
            "parentEntityType" to entity.parentEntityType,
            "parentEntityId" to entity.parentEntityId,
            "documentCategory" to entity.documentCategory,
            "documentType" to entity.documentType,
            "originalFileName" to entity.originalFileName,
            "storagePath" to storagePath,
            "downloadUrl" to downloadUrl,
            "mimeType" to mimeType,
            "fileSizeBytes" to entity.fileSizeBytes,
            "uploadedByUid" to userProfile.uid,
            "uploadedByName" to userProfile.fullName,
            "uploadedByRole" to userProfile.role,
            "uploadedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "checksum" to (entity.checksum ?: ""),
            "version" to entity.version,
            "isDeleted" to false
        )

        firestore.collection("documents").document(documentId).set(metadataMap).await()

        database.documentDao().updateUploadSuccess(
            id = documentId,
            status = "COMPLETED",
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            syncStatus = SyncStatus.SYNCED,
            time = System.currentTimeMillis()
        )

        Log.d(TAG, "Document $documentId successfully uploaded and indexed in Firestore.")
    }

    suspend fun softDeleteDocument(
        documentId: String,
        reason: String,
        userProfile: UserProfile
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.documentDao().softDeleteDocument(documentId, reason, now)

        recordAuditLog(
            eventType = "DOCUMENT_DELETION_REQUESTED",
            documentId = documentId,
            businessEntityId = "",
            actorUid = userProfile.uid,
            actorRole = userProfile.role,
            details = "Soft delete: $reason"
        )

        try {
            firestore.collection("documents").document(documentId).update(
                mapOf(
                    "isDeleted" to true,
                    "deletionReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Log.d(TAG, "Document $documentId soft deleted in Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Remote soft delete deferred: ${e.message}")
        }
    }

    suspend fun recordAuditLog(
        eventType: String,
        documentId: String,
        businessEntityId: String,
        actorUid: String,
        actorRole: String,
        details: String?
    ) = withContext(Dispatchers.IO) {
        val auditId = "aud_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
        val entity = DocumentAuditEntity(
            auditId = auditId,
            documentId = documentId,
            businessEntityId = businessEntityId,
            actorUid = actorUid,
            actorRole = actorRole,
            eventType = eventType,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        database.documentAuditDao().insertAuditLog(entity)

        try {
            val map = hashMapOf<String, Any>(
                "auditId" to auditId,
                "documentId" to documentId,
                "businessEntityId" to businessEntityId,
                "actorUid" to actorUid,
                "actorRole" to actorRole,
                "eventType" to eventType,
                "timestamp" to FieldValue.serverTimestamp(),
                "details" to (details ?: "")
            )
            firestore.collection("document_audit_logs").document(auditId).set(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed posting audit log to Firestore: ${e.message}")
        }
    }

    fun getAllAuditLogsFlow(): Flow<List<DocumentAuditEntity>> {
        return database.documentAuditDao().getAllAuditLogs()
    }
}
