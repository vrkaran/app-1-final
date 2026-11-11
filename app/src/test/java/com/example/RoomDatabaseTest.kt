package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LeadEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.SyncOperationEntity
import com.example.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testLeadDao_insertAndObserve() = runBlocking {
        val lead = LeadEntity(
            id = "LEAD-TEST-001",
            name = "Ramesh Maurya",
            phone = "+91 98391 12345",
            village = "Harraiya, Azamgarh",
            stage = "NEW_LEADS",
            priority = "HIGH",
            capacity = "3.3 kW",
            systemType = "On-Grid",
            assignedTo = "Rahul Kumar",
            assignedToUid = "BDE_UID_01",
            followUpTime = "Tomorrow 10 AM",
            quoteRef = "#HBX-TEST",
            valueEstimate = "₹1,98,000",
            notesJson = "[\"Site visit requested\"]",
            userId = "USER_123",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        db.leadDao().upsertLead(lead)

        val retrieved = db.leadDao().getAllLeads().first()
        assertEquals(1, retrieved.size)
        assertEquals("Ramesh Maurya", retrieved[0].name)
        assertEquals(SyncStatus.PENDING_UPLOAD, retrieved[0].syncStatus)

        // Test search
        val searchResult = db.leadDao().searchLeads("Ramesh").first()
        assertEquals(1, searchResult.size)
        assertEquals("LEAD-TEST-001", searchResult[0].id)
    }

    @Test
    fun testPaymentDao_insertAndPersist() = runBlocking {
        val payment = PaymentEntity(
            receiptId = "RCP-TEST-99",
            customerName = "Kisan Cold Storage",
            consumerId = "7219089901",
            amount = 50000.0,
            method = "NEFT",
            referenceNo = "UTR99281920",
            status = "Pending Verification",
            verifiedBy = "Rahul Kumar",
            userId = "BDE_UID_01",
            dateString = "Today",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        db.paymentDao().upsertPayment(payment)

        val payments = db.paymentDao().getAllPayments().first()
        assertEquals(1, payments.size)
        assertEquals(50000.0, payments[0].amount, 0.01)
        assertEquals("Pending Verification", payments[0].status)
    }

    @Test
    fun testSyncQueueDao_persistentFifoQueue() = runBlocking {
        val op1 = SyncOperationEntity(
            operationId = "OP-1",
            entityType = "LEAD",
            entityId = "LEAD-001",
            operationType = "CREATE",
            payloadJson = "{\"name\":\"Amit Singh\"}",
            userId = "USER_1",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        val op2 = SyncOperationEntity(
            operationId = "OP-2",
            entityType = "PAYMENT",
            entityId = "RCP-001",
            operationType = "CREATE",
            payloadJson = "{\"amount\":25000}",
            userId = "USER_1",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        db.syncQueueDao().enqueue(op1)
        db.syncQueueDao().enqueue(op2)

        val pending = db.syncQueueDao().getPendingOperations()
        assertEquals(2, pending.size)
        assertEquals("OP-1", pending[0].operationId)

        // Remove processed item
        db.syncQueueDao().removeOperation("OP-1")
        val remaining = db.syncQueueDao().getPendingOperations()
        assertEquals(1, remaining.size)
        assertEquals("OP-2", remaining[0].operationId)
    }
}
