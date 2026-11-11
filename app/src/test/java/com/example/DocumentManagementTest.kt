package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DocumentAuditEntity
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.SyncStatus
import com.example.model.DocumentCategory
import com.example.model.StoragePathBuilder
import com.example.util.FileProcessingUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DocumentManagementTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDocumentDao_insertAndQueryByParent() = runBlocking {
        val doc = DocumentEntity(
            documentId = "DOC-SRV-001",
            businessId = "HBX-SRV-2026-084",
            parentEntityType = "SITE_SURVEY",
            parentEntityId = "HBX-SRV-2026-084",
            documentCategory = DocumentCategory.SITE_PHOTO.name,
            documentType = "Rooftop Overview",
            originalFileName = "rooftop_panoramic.jpg",
            storageFileName = "site_photo_001.jpg",
            storagePath = "site-surveys/HBX-SRV-2026-084/photos/site_photo_001.jpg",
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            fileSizeBytes = 2450000L,
            uploadedByUid = "BDE_ALICE",
            uploadedByName = "Alice Sharma",
            uploadedByRole = "BDE",
            uploadStatus = "COMPLETED",
            syncStatus = SyncStatus.SYNCED,
            checksum = "a1b2c3d4e5f6071829",
            version = 1
        )

        db.documentDao().upsertDocument(doc)

        val retrieved = db.documentDao().getDocumentById("DOC-SRV-001")
        assertNotNull(retrieved)
        assertEquals("HBX-SRV-2026-084", retrieved?.businessId)
        assertEquals("image/jpeg", retrieved?.mimeType)

        val parentList = db.documentDao().getDocumentsForParentFlow("SITE_SURVEY", "HBX-SRV-2026-084").first()
        assertEquals(1, parentList.size)
        assertEquals("rooftop_panoramic.jpg", parentList[0].originalFileName)
    }

    @Test
    fun testDocumentDao_softDeleteExcludesFromActiveQueries() = runBlocking {
        val doc = DocumentEntity(
            documentId = "DOC-DELETE-001",
            businessId = "RCP-8801",
            parentEntityType = "PAYMENT",
            parentEntityId = "RCP-8801",
            documentCategory = DocumentCategory.PAYMENT_PROOF.name,
            documentType = "UPI Screenshot",
            originalFileName = "upi_token_8801.jpg",
            storageFileName = "upi_token_8801.jpg",
            storagePath = "payments/RCP-8801/proof/upi_token_8801.jpg",
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            fileSizeBytes = 450000L,
            uploadedByUid = "BDE_ALICE",
            uploadedByName = "Alice Sharma",
            uploadedByRole = "BDE",
            uploadStatus = "COMPLETED",
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )

        db.documentDao().upsertDocument(doc)
        var activeDocs = db.documentDao().getDocumentsForParentFlow("PAYMENT", "RCP-8801").first()
        assertEquals(1, activeDocs.size)

        // Perform enterprise soft delete
        db.documentDao().softDeleteDocument("DOC-DELETE-001", "Superseded by updated bank stamp", System.currentTimeMillis())

        activeDocs = db.documentDao().getDocumentsForParentFlow("PAYMENT", "RCP-8801").first()
        assertEquals(0, activeDocs.size)

        val recordInDb = db.documentDao().getDocumentById("DOC-DELETE-001")
        assertNotNull(recordInDb)
        assertTrue(recordInDb!!.isDeleted)
        assertEquals("Superseded by updated bank stamp", recordInDb.deletionReason)
    }

    @Test
    fun testDocumentDao_pendingUploadsDetection() = runBlocking {
        val queuedDoc = DocumentEntity(
            documentId = "DOC-OFFLINE-01",
            businessId = "LEAD-001",
            parentEntityType = "LEAD",
            parentEntityId = "LEAD-001",
            documentCategory = DocumentCategory.ELECTRICITY_BILL.name,
            documentType = "Latest Bill",
            originalFileName = "offline_bill.pdf",
            storageFileName = "offline_bill.pdf",
            storagePath = "customers/LEAD-001/electricity-bills/offline_bill.pdf",
            mimeType = "application/pdf",
            fileExtension = "pdf",
            fileSizeBytes = 800000L,
            uploadedByUid = "BDE_OFFLINE",
            uploadedByName = "Field Officer",
            uploadedByRole = "BDE",
            uploadStatus = "QUEUED",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        db.documentDao().upsertDocument(queuedDoc)

        val pending = db.documentDao().getPendingUploadDocuments()
        assertEquals(1, pending.size)
        assertEquals("DOC-OFFLINE-01", pending[0].documentId)
        assertEquals("QUEUED", pending[0].uploadStatus)
    }

    @Test
    fun testDocumentAuditDao_insertAndQueryLogs() = runBlocking {
        val auditLog = DocumentAuditEntity(
            auditId = "AUD-001",
            documentId = "DOC-CHALLAN-8821",
            businessEntityId = "HBX-PRJ-2026-0042",
            actorUid = "ADMIN_USER",
            actorRole = "OPERATIONS_MANAGER",
            eventType = "DOCUMENT_VIEWED",
            timestamp = System.currentTimeMillis(),
            details = "Verified by Lucknow Central Hub dispatcher"
        )

        db.documentAuditDao().insertAuditLog(auditLog)

        val logs = db.documentAuditDao().getAuditLogsForDocument("DOC-CHALLAN-8821").first()
        assertEquals(1, logs.size)
        assertEquals("DOCUMENT_VIEWED", logs[0].eventType)
        assertEquals("ADMIN_USER", logs[0].actorUid)
    }

    @Test
    fun testStoragePathBuilder_standardizedHierarchy() {
        // Customer KYC
        val kycPath = StoragePathBuilder.buildPath(
            parentType = "CUSTOMER",
            parentId = "CUST-9921",
            category = DocumentCategory.KYC,
            sanitizedFileName = "kyc_aadhaar.pdf"
        )
        assertEquals("customers/CUST-9921/kyc/kyc_aadhaar.pdf", kycPath)

        // Site Survey Photos
        val photoPath = StoragePathBuilder.buildPath(
            parentType = "SITE_SURVEY",
            parentId = "SRV-084",
            category = DocumentCategory.SITE_PHOTO,
            sanitizedFileName = "rooftop.jpg"
        )
        assertEquals("site-surveys/SRV-084/photos/rooftop.jpg", photoPath)

        // Payment Proof
        val paymentPath = StoragePathBuilder.buildPath(
            parentType = "PAYMENT",
            parentId = "RCP-8801",
            category = DocumentCategory.PAYMENT_PROOF,
            sanitizedFileName = "upi_receipt.jpg"
        )
        assertEquals("payments/RCP-8801/proof/upi_receipt.jpg", paymentPath)

        // Unique storage file name generation
        val uniqueName = StoragePathBuilder.generateUniqueStorageFileName(
            documentType = "SITE_PHOTO",
            businessId = "PRJ-102",
            extension = "jpg"
        )
        assertTrue(uniqueName.startsWith("site_photo_PRJ-102_"))
        assertTrue(uniqueName.endsWith(".jpg"))
    }

    @Test
    fun testFileProcessingUtil_checksumAndMimeType() {
        assertEquals("application/pdf", FileProcessingUtil.getMimeTypeFromExtension(".pdf"))
        assertEquals("image/jpeg", FileProcessingUtil.getMimeTypeFromExtension("jpg"))
        assertEquals("image/png", FileProcessingUtil.getMimeTypeFromExtension(".png"))

        val testFile = File(context.cacheDir, "test_file.txt")
        FileOutputStream(testFile).use { it.write("HBX Solar One Enterprise".toByteArray()) }

        val md5 = FileProcessingUtil.calculateMD5Checksum(testFile)
        assertNotNull(md5)
        assertEquals(32, md5?.length)
    }
}
