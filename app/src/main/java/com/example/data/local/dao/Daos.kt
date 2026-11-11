package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY localUpdatedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE stage = :stage ORDER BY localUpdatedAt DESC")
    fun getLeadsByStage(stage: String): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :leadId")
    suspend fun getLeadById(leadId: String): LeadEntity?

    @Query("SELECT * FROM leads WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR village LIKE '%' || :query || '%'")
    fun searchLeads(query: String): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLead(lead: LeadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeads(leads: List<LeadEntity>)

    @Query("UPDATE leads SET syncStatus = :status, serverUpdatedAt = :serverTime WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, serverTime: Long?)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: String)

    @Query("SELECT COUNT(*) FROM leads")
    suspend fun getCount(): Int
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY localTimestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayments(payments: List<PaymentEntity>)

    @Query("UPDATE payments SET syncStatus = :status, status = :verificationStatus WHERE receiptId = :receiptId")
    suspend fun updatePaymentStatus(receiptId: String, status: SyncStatus, verificationStatus: String)

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun getCount(): Int
}

@Dao
interface SiteSurveyDao {
    @Query("SELECT * FROM site_surveys WHERE leadId = :leadId LIMIT 1")
    fun getSurveyForLead(leadId: String): Flow<SiteSurveyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSurvey(survey: SiteSurveyEntity)
}

@Dao
interface PipelineStageDao {
    @Query("SELECT * FROM pipeline_stages ORDER BY stageNumber ASC")
    fun getAllStages(): Flow<List<PipelineStageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStages(stages: List<PipelineStageEntity>)

    @Query("UPDATE pipeline_stages SET status = :status WHERE stageNumber = :stageNumber")
    suspend fun updateStageStatus(stageNumber: Int, status: String)

    @Query("SELECT COUNT(*) FROM pipeline_stages")
    suspend fun getCount(): Int
}

@Dao
interface BomItemDao {
    @Query("SELECT * FROM bom_items")
    fun getAllItems(): Flow<List<BomItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<BomItemEntity>)

    @Query("SELECT COUNT(*) FROM bom_items")
    suspend fun getCount(): Int
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING_UPLOAD' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncOperationEntity)

    @Query("UPDATE sync_queue SET syncStatus = :status, errorMessage = :error, retryCount = retryCount + 1, updatedAt = :time WHERE operationId = :id")
    suspend fun updateOperationStatus(id: String, status: SyncStatus, error: String?, time: Long)

    @Query("DELETE FROM sync_queue WHERE operationId = :id")
    suspend fun removeOperation(id: String)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus = 'PENDING_UPLOAD'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE parentEntityType = :parentType AND parentEntityId = :parentId AND isDeleted = 0 ORDER BY uploadedAt DESC")
    fun getDocumentsForParentFlow(parentType: String, parentId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY uploadedAt DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 AND documentCategory = :category ORDER BY uploadedAt DESC")
    fun getDocumentsByCategoryFlow(category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE documentId = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE parentEntityType = :parentType AND parentEntityId = :parentId AND isDeleted = 0 ORDER BY uploadedAt DESC")
    suspend fun getDocumentsForParent(parentType: String, parentId: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE uploadStatus IN ('QUEUED', 'LOCAL_ONLY', 'FAILED', 'RETRY_PENDING', 'METADATA_PENDING') AND isDeleted = 0")
    suspend fun getPendingUploadDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE uploadStatus IN ('QUEUED', 'LOCAL_ONLY', 'FAILED', 'RETRY_PENDING', 'METADATA_PENDING') AND isDeleted = 0")
    suspend fun getPendingUploads(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Query("UPDATE documents SET uploadStatus = :status, storagePath = :storagePath, downloadUrl = :downloadUrl, syncStatus = :syncStatus, updatedAt = :time WHERE documentId = :id")
    suspend fun updateUploadSuccess(id: String, status: String, storagePath: String, downloadUrl: String?, syncStatus: SyncStatus, time: Long)

    @Query("UPDATE documents SET uploadStatus = :status, syncStatus = :syncStatus, updatedAt = :time WHERE documentId = :id")
    suspend fun updateUploadStatus(id: String, status: String, syncStatus: SyncStatus, time: Long)

    @Query("UPDATE documents SET isDeleted = 1, deletionReason = :reason, updatedAt = :time, syncStatus = 'PENDING_UPLOAD' WHERE documentId = :id")
    suspend fun softDeleteDocument(id: String, reason: String, time: Long)

    @Query("DELETE FROM documents WHERE documentId = :id")
    suspend fun deleteDocumentPermanent(id: String)

    @Query("SELECT COUNT(*) FROM documents WHERE isDeleted = 0")
    suspend fun getDocumentCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE parentEntityType = :parentType AND parentEntityId = :parentId AND isDeleted = 0")
    suspend fun getDocumentCountForParent(parentType: String, parentId: String): Int
}

@Dao
interface DocumentAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: DocumentAuditEntity)

    @Query("SELECT * FROM document_audit_logs WHERE documentId = :documentId ORDER BY timestamp DESC")
    fun getAuditLogsForDocument(documentId: String): Flow<List<DocumentAuditEntity>>

    @Query("SELECT * FROM document_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllAuditLogs(): Flow<List<DocumentAuditEntity>>
}
