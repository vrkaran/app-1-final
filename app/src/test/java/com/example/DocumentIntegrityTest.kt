package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.SyncStatus
import com.example.util.FileProcessingUtil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [36])
class DocumentIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var tempFile: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        tempFile = File(context.cacheDir, "sample_solar_spec.pdf")
        tempFile.writeText("HBX SOLAR ONE - Discom Technical Sanction Spec Purvanchal 2026")
    }

    @After
    fun tearDown() {
        db.close()
        if (tempFile.exists()) tempFile.delete()
    }

    @Test
    fun testSha256Checksum_produces64CharHex() {
        val sha256 = FileProcessingUtil.calculateSHA256Checksum(tempFile)
        assertNotNull(sha256)
        assertEquals(64, sha256!!.length)
    }

    @Test
    fun testFileIntegrityVerification_detectsTampering() {
        val originalSha256 = FileProcessingUtil.calculateSHA256Checksum(tempFile)
        assertNotNull(originalSha256)

        // Verifying unaltered file returns true
        assertTrue(FileProcessingUtil.verifyFileIntegrity(tempFile, originalSha256!!))

        // Modify file content (tamper simulation)
        tempFile.appendText(" - TAMPERED UNAUTHORIZED EDIT")

        // Verifying tampered file returns false
        assertFalse(FileProcessingUtil.verifyFileIntegrity(tempFile, originalSha256))
    }

    @Test
    fun testDocumentVersioning_incrementsSequentially() = runBlocking {
        val docV1 = DocumentEntity(
            documentId = "doc_v1",
            businessId = "PRJ-102",
            parentEntityType = "PROJECT",
            parentEntityId = "PRJ-102",
            documentCategory = "SITE_PHOTO",
            documentType = "Rooftop Overview",
            originalFileName = "roof_v1.jpg",
            storageFileName = "site_photo_PRJ-102_1_a.jpg",
            storagePath = "projects/PRJ-102/site-photos/site_photo_PRJ-102_1_a.jpg",
            localFilePath = "/data/roof_v1.jpg",
            mimeType = "image/jpeg",
            fileExtension = "jpg",
            fileSizeBytes = 1024,
            uploadedByUid = "user_bde_01",
            uploadedByName = "Rahul Kumar",
            uploadedByRole = "BDE",
            uploadedAt = 1000L,
            updatedAt = 1000L,
            uploadStatus = "COMPLETED",
            syncStatus = SyncStatus.SYNCED,
            checksum = "sha256_mock_hash_1",
            thumbnailPath = null,
            description = "Initial survey",
            isDeleted = false,
            version = 1,
            downloadUrl = null
        )

        db.documentDao().upsertDocument(docV1)

        val retrievedV1 = db.documentDao().getDocumentsForParent("PROJECT", "PRJ-102")
        assertEquals(1, retrievedV1.size)
        assertEquals(1, retrievedV1[0].version)

        // Second revision (e.g. after customer cleared rooftop obstacles)
        val highestVersion = retrievedV1.filter { it.documentCategory == "SITE_PHOTO" }.maxOfOrNull { it.version } ?: 0
        val nextVersion = highestVersion + 1

        val docV2 = docV1.copy(
            documentId = "doc_v2",
            originalFileName = "roof_v2_cleared.jpg",
            storageFileName = "site_photo_PRJ-102_2_b.jpg",
            storagePath = "projects/PRJ-102/site-photos/site_photo_PRJ-102_2_b.jpg",
            uploadedAt = 2000L,
            updatedAt = 2000L,
            version = nextVersion
        )

        db.documentDao().upsertDocument(docV2)

        val allRevisions = db.documentDao().getDocumentsForParent("PROJECT", "PRJ-102")
        assertEquals(2, allRevisions.size)
        assertEquals(2, nextVersion)
    }
}
