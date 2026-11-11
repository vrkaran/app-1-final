package com.example.model

import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

enum class DocumentCategory(val displayName: String, val iconName: String) {
    KYC("Customer KYC", "badge"),
    ELECTRICITY_BILL("Electricity Bill", "receipt"),
    AGREEMENT("Customer Agreement", "handshake"),
    SITE_PHOTO("Site Survey Photo", "camera"),
    INSTALLATION_EVIDENCE("Installation Evidence", "build"),
    INSPECTION_REPORT("Inspection Report", "checklist"),
    COMPLETION_CERTIFICATE("Completion Certificate", "verified"),
    WARRANTY("Warranty Certificate", "security"),
    QUOTATION_PDF("Quotation PDF", "picture_as_pdf"),
    PAYMENT_PROOF("Payment Proof", "payment"),
    PAYMENT_RECEIPT("Official Receipt", "receipt_long"),
    SERVICE_PHOTO("Service Evidence", "home_repair_service"),
    OTHER("Document", "description")
}

data class DocumentItem(
    val documentId: String,
    val businessId: String,
    val parentEntityType: String,
    val parentEntityId: String,
    val category: DocumentCategory,
    val documentType: String,
    val originalFileName: String,
    val storageFileName: String,
    val storagePath: String,
    val localFilePath: String?,
    val mimeType: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val uploadedByUid: String,
    val uploadedByName: String,
    val uploadedByRole: String,
    val uploadedAt: Long,
    val uploadStatus: String,
    val syncStatus: SyncStatus,
    val checksum: String?,
    val thumbnailPath: String?,
    val description: String?,
    val version: Int,
    val downloadUrl: String?
) {
    val formattedSize: String
        get() {
            if (fileSizeBytes < 1024) return "$fileSizeBytes B"
            val kb = fileSizeBytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.1f MB", mb)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
            return sdf.format(Date(uploadedAt))
        }

    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    val isPdf: Boolean
        get() = mimeType == "application/pdf"

    fun toEntity(): DocumentEntity {
        return DocumentEntity(
            documentId = documentId,
            businessId = businessId,
            parentEntityType = parentEntityType,
            parentEntityId = parentEntityId,
            documentCategory = category.name,
            documentType = documentType,
            originalFileName = originalFileName,
            storageFileName = storageFileName,
            storagePath = storagePath,
            localFilePath = localFilePath,
            mimeType = mimeType,
            fileExtension = fileExtension,
            fileSizeBytes = fileSizeBytes,
            uploadedByUid = uploadedByUid,
            uploadedByName = uploadedByName,
            uploadedByRole = uploadedByRole,
            uploadedAt = uploadedAt,
            updatedAt = System.currentTimeMillis(),
            uploadStatus = uploadStatus,
            syncStatus = syncStatus,
            checksum = checksum,
            thumbnailPath = thumbnailPath,
            description = description,
            isDeleted = false,
            version = version,
            downloadUrl = downloadUrl
        )
    }

    companion object {
        fun fromEntity(entity: DocumentEntity): DocumentItem {
            val cat = try {
                DocumentCategory.valueOf(entity.documentCategory)
            } catch (e: Exception) {
                DocumentCategory.OTHER
            }
            return DocumentItem(
                documentId = entity.documentId,
                businessId = entity.businessId,
                parentEntityType = entity.parentEntityType,
                parentEntityId = entity.parentEntityId,
                category = cat,
                documentType = entity.documentType,
                originalFileName = entity.originalFileName,
                storageFileName = entity.storageFileName,
                storagePath = entity.storagePath,
                localFilePath = entity.localFilePath,
                mimeType = entity.mimeType,
                fileExtension = entity.fileExtension,
                fileSizeBytes = entity.fileSizeBytes,
                uploadedByUid = entity.uploadedByUid,
                uploadedByName = entity.uploadedByName,
                uploadedByRole = entity.uploadedByRole,
                uploadedAt = entity.uploadedAt,
                uploadStatus = entity.uploadStatus,
                syncStatus = entity.syncStatus,
                checksum = entity.checksum,
                thumbnailPath = entity.thumbnailPath,
                description = entity.description,
                version = entity.version,
                downloadUrl = entity.downloadUrl
            )
        }
    }
}

object StoragePathBuilder {
    fun buildPath(
        parentType: String,
        parentId: String,
        category: DocumentCategory,
        sanitizedFileName: String
    ): String {
        val safeParentId = sanitizeId(parentId)
        return when (parentType.uppercase(Locale.US)) {
            "CUSTOMER" -> {
                val folder = when (category) {
                    DocumentCategory.KYC -> "kyc"
                    DocumentCategory.ELECTRICITY_BILL -> "electricity-bills"
                    DocumentCategory.AGREEMENT -> "agreements"
                    else -> "kyc"
                }
                "customers/$safeParentId/$folder/$sanitizedFileName"
            }
            "LEAD" -> "leads/$safeParentId/documents/$sanitizedFileName"
            "SITE_SURVEY" -> {
                val folder = if (sanitizedFileName.endsWith(".mp4", ignoreCase = true)) "videos" else "photos"
                "site-surveys/$safeParentId/$folder/$sanitizedFileName"
            }
            "PROJECT" -> {
                val folder = when (category) {
                    DocumentCategory.SITE_PHOTO -> "site-photos"
                    DocumentCategory.INSTALLATION_EVIDENCE -> "installation-evidence"
                    DocumentCategory.INSPECTION_REPORT -> "inspection-reports"
                    DocumentCategory.COMPLETION_CERTIFICATE -> "completion-certificates"
                    DocumentCategory.WARRANTY -> "warranty-documents"
                    else -> "site-photos"
                }
                "projects/$safeParentId/$folder/$sanitizedFileName"
            }
            "QUOTATION" -> "quotations/$safeParentId/pdf/$sanitizedFileName"
            "PAYMENT" -> {
                val folder = if (category == DocumentCategory.PAYMENT_RECEIPT) "receipts" else "proof"
                "payments/$safeParentId/$folder/$sanitizedFileName"
            }
            "SERVICE_REQUEST" -> "service-requests/$safeParentId/attachments/$sanitizedFileName"
            "USER" -> "users/$safeParentId/profile/$sanitizedFileName"
            else -> "company-assets/$sanitizedFileName"
        }
    }

    fun generateUniqueStorageFileName(
        documentType: String,
        businessId: String,
        extension: String
    ): String {
        val safeType = sanitizeId(documentType).lowercase(Locale.US)
        val safeId = sanitizeId(businessId)
        val timestamp = System.currentTimeMillis() / 1000
        val shortUuid = UUID.randomUUID().toString().substring(0, 6)
        val cleanExt = extension.removePrefix(".").lowercase(Locale.US)
        return "${safeType}_${safeId}_${timestamp}_$shortUuid.$cleanExt"
    }

    private fun sanitizeId(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    }
}
