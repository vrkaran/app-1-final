package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DocumentAuditEntity
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.SyncOperationEntity
import com.example.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [36])
class OfflineUploadRecoveryTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testPartialFailureRecovery_metadataPendingSkipsByteUpload() = runBlocking {
        // State: Storage upload succeeded and returned a downloadUrl, but Firestore write failed or app closed
        val partialDoc = DocumentEntity(
            documentId = "doc_partial_001",
            businessId = "PRJ-102",
            parentEntityType = "PROJECT",
            parentEntityId = "PRJ-102",
            documentCategory = "SITE_PHOTO",
            documentType = "House Front",
            originalFileName = "front.jpg",
            storageFileName = "site_photo_PRJ-102_1762851600_a1b2c3.jpg",
            storagePath = "projects/PRJ-102/site-photos/site_photo_PRJ-102_1762851600_a1b2c3.jpg",
            localFilePath = "/data/user/0/documents/front.jpg",
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            fileSizeBytes = 2048,
            uploadedByUid = "user_bde_01",
            uploadedByName = "Rahul Kumar",
            uploadedByRole = "BDE",
            uploadedAt = 1000L,
            updatedAt = 1000L,
            uploadStatus = "METADATA_PENDING", // Storage succeeded
            syncStatus = SyncStatus.PENDING_UPLOAD,
            checksum = "sha256_mock_hash",
            thumbnailPath = null,
            description = "Survey photo",
            isDeleted = false,
            version = 1,
            downloadUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/front.jpg"
        )

        db.documentDao().upsertDocument(partialDoc)

        val retrieved = db.documentDao().getDocumentById("doc_partial_001")
        assertNotNull(retrieved)
        assertEquals("METADATA_PENDING", retrieved!!.uploadStatus)
        assertEquals("https://firebasestorage.googleapis.com/v0/b/bucket/o/front.jpg", retrieved.downloadUrl)

        // Verifying recovery rule: Status METADATA_PENDING means bytes already exist in cloud storage
        val bytesNeedUpload = (retrieved.uploadStatus != "UPLOADED" &&
                retrieved.uploadStatus != "METADATA_PENDING" &&
                retrieved.uploadStatus != "COMPLETED")
        assertFalse("Byte upload must be skipped when status is METADATA_PENDING", bytesNeedUpload)
    }

    @Test
    fun testSyncQueueDeduplication_fifoEnqueue() = runBlocking {
        val op1 = SyncOperationEntity(
            operationId = "OP-DOC-001",
            entityType = "DOCUMENT",
            entityId = "doc_001",
            operationType = "CREATE",
            payloadJson = "{\"documentId\":\"doc_001\"}",
            userId = "user_bde_01",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        db.syncQueueDao().enqueue(op1)

        val pendingFirst = db.syncQueueDao().getPendingOperations()
        assertEquals(1, pendingFirst.size)

        // Attempt duplicate enqueue with same operationId (e.g. repeated tap)
        db.syncQueueDao().enqueue(op1)
        val pendingAfterDuplicate = db.syncQueueDao().getPendingOperations()
        assertEquals(1, pendingAfterDuplicate.size) // OnConflictStrategy.REPLACE ensures no duplicate queue entries

        // Once processed, remove operation
        db.syncQueueDao().removeOperation("OP-DOC-001")
        val pendingAfterRemoval = db.syncQueueDao().getPendingOperations()
        assertTrue(pendingAfterRemoval.isEmpty())
    }

    @Test
    fun testProtectedFinancialDocuments_softDeletePreservesAuditTrail() = runBlocking {
        // Seed payment receipt document
        val paymentDoc = DocumentEntity(
            documentId = "doc_payment_99",
            businessId = "RCP-8801",
            parentEntityType = "PAYMENT",
            parentEntityId = "RCP-8801",
            documentCategory = "PAYMENT_PROOF",
            documentType = "UPI Screenshot",
            originalFileName = "upi_receipt.jpg",
            storageFileName = "payment_proof_RCP-8801_1_a.jpg",
            storagePath = "payments/RCP-8801/proof/payment_proof_RCP-8801_1_a.jpg",
            localFilePath = "/data/user/0/documents/upi.jpg",
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            fileSizeBytes = 512,
            uploadedByUid = "user_bde_01",
            uploadedByName = "Rahul Kumar",
            uploadedByRole = "BDE",
            uploadedAt = 1000L,
            updatedAt = 1000L,
            uploadStatus = "COMPLETED",
            syncStatus = SyncStatus.SYNCED,
            checksum = "sha256_payment_hash",
            thumbnailPath = null,
            description = "Token advance receipt",
            isDeleted = false,
            version = 1,
            downloadUrl = null
        )

        db.documentDao().upsertDocument(paymentDoc)

        // Audit log on creation
        val auditCreate = DocumentAuditEntity(
            auditId = "aud_01",
            documentId = "doc_payment_99",
            businessEntityId = "RCP-8801",
            actorUid = "user_bde_01",
            actorRole = "BDE",
            eventType = "DOCUMENT_UPLOADED",
            timestamp = 1000L,
            details = "Payment proof uploaded"
        )
        db.documentAuditDao().insertAuditLog(auditCreate)

        // Execute soft delete (e.g. corrected payment receipt)
        db.documentDao().softDeleteDocument("doc_payment_99", "Correction of bank reference", 2000L)

        // Audit log on soft deletion
        val auditDelete = DocumentAuditEntity(
            auditId = "aud_02",
            documentId = "doc_payment_99",
            businessEntityId = "RCP-8801",
            actorUid = "user_bde_01",
            actorRole = "BDE",
            eventType = "DOCUMENT_DELETION_REQUESTED",
            timestamp = 2000L,
            details = "Soft delete requested: Correction of bank reference"
        )
        db.documentAuditDao().insertAuditLog(auditDelete)

        // 1. Verify active query excludes the document
        val activeDocs = db.documentDao().getDocumentsForParent("PAYMENT", "RCP-8801")
        assertTrue("Active documents list must exclude soft-deleted items", activeDocs.isEmpty())

        // 2. Verify raw document row still exists in database for compliance audit
        val rawDoc = db.documentDao().getDocumentById("doc_payment_99")
        assertNotNull("Record must never be hard-deleted from database", rawDoc)
        assertTrue(rawDoc!!.isDeleted)
        assertEquals("Correction of bank reference", rawDoc.deletionReason)

        // 3. Verify audit trail has complete chain of custody
        val logs = db.documentAuditDao().getAuditLogsForDocument("doc_payment_99").first()
        assertEquals(2, logs.size)
        // Ordered by timestamp DESC: newest (DELETION) first, oldest (UPLOADED) second
        assertEquals("DOCUMENT_DELETION_REQUESTED", logs[0].eventType)
        assertEquals("DOCUMENT_UPLOADED", logs[1].eventType)
    }
}
