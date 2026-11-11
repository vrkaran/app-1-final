package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    SYNCING,
    FAILED,
    CONFLICT
}

@Entity(
    tableName = "leads",
    indices = [
        Index(value = ["stage"]),
        Index(value = ["assignedToUid"]),
        Index(value = ["syncStatus"])
    ]
)
data class LeadEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val village: String,
    val stage: String,
    val priority: String,
    val capacity: String,
    val systemType: String,
    val assignedTo: String,
    val assignedToUid: String,
    val followUpTime: String,
    val quoteRef: String,
    val valueEstimate: String,
    val notesJson: String = "[]",
    val userId: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val serverUpdatedAt: Long? = null
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["consumerId"]),
        Index(value = ["syncStatus"])
    ]
)
data class PaymentEntity(
    @PrimaryKey val receiptId: String,
    val customerName: String,
    val consumerId: String,
    val amount: Double,
    val method: String,
    val referenceNo: String,
    val status: String,
    val verifiedBy: String,
    val userId: String,
    val dateString: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "site_surveys",
    indices = [
        Index(value = ["leadId"])
    ]
)
data class SiteSurveyEntity(
    @PrimaryKey val surveyId: String,
    val leadId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val roofSubstrate: String,
    val usableAreaSqFt: Int,
    val shadowAssessment: String,
    val cableLengthMeters: Int,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["syncStatus"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey val operationId: String,
    val entityType: String, // "LEAD", "PAYMENT", "SURVEY", "PROJECT"
    val entityId: String,
    val operationType: String, // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val errorMessage: String? = null,
    val userId: String
)

@Entity(
    tableName = "pipeline_stages"
)
data class PipelineStageEntity(
    @PrimaryKey val stageNumber: Int,
    val title: String,
    val description: String,
    val dateOrInfo: String,
    val status: String // "DONE", "ACTIVE", "IN_PROGRESS", "PENDING"
)

@Entity(
    tableName = "bom_items"
)
data class BomItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specs: String,
    val quantityStatus: String
)

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["parentEntityType", "parentEntityId"]),
        Index(value = ["documentCategory"]),
        Index(value = ["uploadStatus"]),
        Index(value = ["syncStatus"]),
        Index(value = ["isDeleted"])
    ]
)
data class DocumentEntity(
    @PrimaryKey val documentId: String,
    val businessId: String,
    val parentEntityType: String, // "CUSTOMER", "LEAD", "PROJECT", "SITE_SURVEY", "PAYMENT", "QUOTATION", "SERVICE_REQUEST"
    val parentEntityId: String,
    val documentCategory: String, // "KYC", "ELECTRICITY_BILL", "AGREEMENT", "SITE_PHOTO", "INSTALLATION_EVIDENCE", "INSPECTION_REPORT", "COMPLETION_CERTIFICATE", "WARRANTY", "QUOTATION_PDF", "PAYMENT_PROOF", "PAYMENT_RECEIPT", "SERVICE_PHOTO", "OTHER"
    val documentType: String,
    val originalFileName: String,
    val storageFileName: String,
    val storagePath: String,
    val localFilePath: String? = null,
    val mimeType: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val uploadedByUid: String,
    val uploadedByName: String,
    val uploadedByRole: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val uploadStatus: String = "COMPLETED", // "LOCAL_ONLY", "QUEUED", "UPLOADING", "UPLOADED", "METADATA_PENDING", "COMPLETED", "FAILED", "RETRY_PENDING", "CANCELLED", "DELETION_PENDING"
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val checksum: String? = null,
    val thumbnailPath: String? = null,
    val description: String? = null,
    val isDeleted: Boolean = false,
    val deletionReason: String? = null,
    val version: Int = 1,
    val downloadUrl: String? = null
)

@Entity(
    tableName = "document_audit_logs",
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["businessEntityId"]),
        Index(value = ["timestamp"])
    ]
)
data class DocumentAuditEntity(
    @PrimaryKey val auditId: String,
    val documentId: String,
    val businessEntityId: String,
    val actorUid: String,
    val actorRole: String,
    val eventType: String, // "DOCUMENT_UPLOADED", "DOCUMENT_DOWNLOAD_REQUESTED", "DOCUMENT_VIEWED", "DOCUMENT_REPLACED", "DOCUMENT_DELETION_REQUESTED", "DOCUMENT_DELETED", "DOCUMENT_UPLOAD_FAILED", "DOCUMENT_UPLOAD_RETRIED", "DOCUMENT_PERMISSION_DENIED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null
)
