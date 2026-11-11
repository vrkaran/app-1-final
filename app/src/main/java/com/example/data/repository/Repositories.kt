package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.converters.HBXTypeConverters
import com.example.data.local.entities.LeadEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.PipelineStageEntity
import com.example.data.local.entities.BomItemEntity
import com.example.data.local.entities.SyncStatus
import com.example.data.sync.SyncEngine
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LeadRepository(
    private val database: AppDatabase,
    private val syncEngine: SyncEngine
) {
    constructor(context: Context) : this(
        AppDatabase.getInstance(context),
        SyncEngine(context)
    )

    private val converters = HBXTypeConverters()

    fun getAllLeads(): Flow<List<Lead>> {
        return database.leadDao().getAllLeads().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getLeadsByStage(stage: LeadStage): Flow<List<Lead>> {
        return database.leadDao().getLeadsByStage(stage.name).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun searchLeads(query: String): Flow<List<Lead>> {
        return database.leadDao().searchLeads(query).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun createLead(
        name: String,
        phone: String,
        village: String,
        capacity: String,
        priority: LeadPriority,
        assignedTo: String,
        userId: String
    ) {
        val leadId = "LEAD-${(100..999).random()}"
        val entity = LeadEntity(
            id = leadId,
            name = name,
            phone = phone,
            village = village,
            stage = LeadStage.NEW_LEADS.name,
            priority = priority.name,
            capacity = capacity,
            systemType = "On-Grid (PM Surya Ghar)",
            assignedTo = assignedTo,
            assignedToUid = userId,
            followUpTime = "Tomorrow 10:00 AM",
            quoteRef = "#HBX-${(1000..9999).random()}",
            valueEstimate = "₹1,98,000",
            notesJson = "[\"Created via Mobile Field CRM\"]",
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        // 1. Immediate local write (Offline-First)
        database.leadDao().upsertLead(entity)

        // 2. Persistent queue enqueue & sync
        syncEngine.enqueueLeadCreate(entity, userId)
    }

    suspend fun seedInitialLeadsIfEmpty(userId: String) {
        if (database.leadDao().getCount() == 0) {
            val initial = listOf(
                LeadEntity(
                    id = "LEAD-001",
                    name = "Amit Singh",
                    phone = "+91 98391 44210",
                    village = "Vill. Harraiya, Azamgarh",
                    stage = LeadStage.QUOTATION_SHARED.name,
                    priority = LeadPriority.HIGH.name,
                    capacity = "3 kW On-Grid Rooftop",
                    systemType = "On-Grid (PM Surya Ghar)",
                    assignedTo = "Rahul Kumar (You)",
                    assignedToUid = userId,
                    followUpTime = "Today, 11:30 AM",
                    quoteRef = "#HBX-8821",
                    valueEstimate = "₹1,98,000",
                    notesJson = "[\"Customer confirmed 380 sq.ft RCC rooftop availability.\", \"Sanctioned load 4.0 kW PUVVNL.\"]",
                    userId = userId,
                    syncStatus = SyncStatus.SYNCED
                ),
                LeadEntity(
                    id = "LEAD-002",
                    name = "Rajeshwar Prasad",
                    phone = "+91 94152 77319",
                    village = "Phoolpur, Azamgarh",
                    stage = LeadStage.SURVEY_PENDING.name,
                    priority = LeadPriority.STANDARD.name,
                    capacity = "5 kW Hybrid System",
                    systemType = "Hybrid Battery Backup",
                    assignedTo = "Rahul Kumar (You)",
                    assignedToUid = userId,
                    followUpTime = "Tomorrow 10:00 AM",
                    quoteRef = "#HBX-8829",
                    valueEstimate = "₹3,40,000",
                    notesJson = "[\"Shadow obstacle near east parapet requires physical scan.\"]",
                    userId = userId,
                    syncStatus = SyncStatus.SYNCED
                ),
                LeadEntity(
                    id = "LEAD-003",
                    name = "Kisan Cold Storage (Dr. V. Verma)",
                    phone = "+91 97920 11982",
                    village = "Sagri, Azamgarh Industrial Sector",
                    stage = LeadStage.APPROVED.name,
                    priority = LeadPriority.COMMERCIAL.name,
                    capacity = "25 kW Commercial C&I",
                    systemType = "High Voltage 3-Phase Industrial",
                    assignedTo = "Key Accounts (Rahul / Deepak)",
                    assignedToUid = userId,
                    followUpTime = "15 Nov, 03:00 PM",
                    quoteRef = "#HBX-C&I-402",
                    valueEstimate = "₹18,50,000",
                    notesJson = "[\"Feasibility study passed 98.4%.\"]",
                    userId = userId,
                    syncStatus = SyncStatus.SYNCED
                ),
                LeadEntity(
                    id = "LEAD-004",
                    name = "Meena Devi",
                    phone = "+91 91254 88301",
                    village = "Mubarakpur, Azamgarh",
                    stage = LeadStage.NEW_LEADS.name,
                    priority = LeadPriority.GOVT_SUBSIDY.name,
                    capacity = "2 kW Rooftop",
                    systemType = "PM Surya Ghar Subsidized",
                    assignedTo = "Rahul Kumar (You)",
                    assignedToUid = userId,
                    followUpTime = "12 Nov, Morning",
                    quoteRef = "#HBX-8835",
                    valueEstimate = "₹1,30,000",
                    notesJson = "[\"Awaiting electricity bill upload.\"]",
                    userId = userId,
                    syncStatus = SyncStatus.SYNCED
                )
            )
            database.leadDao().upsertLeads(initial)
        }
    }

    private fun LeadEntity.toDomainModel(): Lead {
        val stageEnum = try {
            LeadStage.valueOf(stage)
        } catch (_: Exception) {
            LeadStage.NEW_LEADS
        }

        val priorityEnum = try {
            LeadPriority.valueOf(priority)
        } catch (_: Exception) {
            LeadPriority.STANDARD
        }

        return Lead(
            id = id,
            name = name,
            phone = phone,
            village = village,
            stage = stageEnum,
            priority = priorityEnum,
            capacity = capacity,
            systemType = systemType,
            assignedTo = assignedTo,
            followUpTime = followUpTime,
            quoteRef = quoteRef,
            valueEstimate = valueEstimate,
            notes = converters.toStringList(notesJson)
        )
    }
}

class PaymentRepository(
    private val database: AppDatabase,
    private val syncEngine: SyncEngine
) {
    constructor(context: Context) : this(
        AppDatabase.getInstance(context),
        SyncEngine(context)
    )

    fun getAllPayments(): Flow<List<PaymentRecord>> {
        return database.paymentDao().getAllPayments().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun addPayment(
        customerName: String,
        consumerId: String,
        amount: Double,
        method: String,
        referenceNo: String,
        userId: String,
        verifierRole: String
    ) {
        val receiptId = "RCP-2026-${(1000..9999).random()}"
        val entity = PaymentEntity(
            receiptId = receiptId,
            customerName = customerName,
            consumerId = consumerId,
            amount = amount,
            method = method,
            referenceNo = referenceNo,
            status = "Pending Verification",
            verifiedBy = verifierRole,
            userId = userId,
            dateString = "Today",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        // 1. Immediate local write (Offline-First)
        database.paymentDao().upsertPayment(entity)

        // 2. Persistent queue enqueue & sync
        syncEngine.enqueuePaymentCreate(entity, userId)
    }

    suspend fun seedInitialPaymentsIfEmpty(userId: String) {
        if (database.paymentDao().getCount() == 0) {
            val initial = listOf(
                PaymentEntity(
                    receiptId = "RCP-2026-0941",
                    customerName = "Amit Singh",
                    consumerId = "7219084421",
                    amount = 25000.0,
                    method = "UPI",
                    referenceNo = "UPI/AXIS/4910291920",
                    status = "Verified",
                    verifiedBy = "Rahul Kumar",
                    userId = userId,
                    dateString = "10 Nov 2026",
                    syncStatus = SyncStatus.SYNCED
                ),
                PaymentEntity(
                    receiptId = "RCP-2026-0942",
                    customerName = "Rajesh Verma",
                    consumerId = "7219083310",
                    amount = 10000.0,
                    method = "Cash",
                    referenceNo = "CASH/REC-8821",
                    status = "Verified",
                    verifiedBy = "Rahul Kumar",
                    userId = userId,
                    dateString = "10 Nov 2026",
                    syncStatus = SyncStatus.SYNCED
                ),
                PaymentEntity(
                    receiptId = "RCP-2026-0943",
                    customerName = "Kisan Cold Storage",
                    consumerId = "7219089901",
                    amount = 250000.0,
                    method = "Bank Transfer",
                    referenceNo = "NEFT/SBIN000182/910",
                    status = "Verified",
                    verifiedBy = "Pooja Sharma (Finance)",
                    userId = userId,
                    dateString = "09 Nov 2026",
                    syncStatus = SyncStatus.SYNCED
                )
            )
            database.paymentDao().upsertPayments(initial)
        }
    }

    private fun PaymentEntity.toDomainModel(): PaymentRecord {
        return PaymentRecord(
            receiptId = receiptId,
            customerName = customerName,
            consumerId = consumerId,
            amount = amount,
            method = method,
            date = dateString,
            status = status,
            referenceNo = referenceNo,
            verifiedBy = verifiedBy
        )
    }
}

class ProjectRepository(
    private val database: AppDatabase
) {
    constructor(context: Context) : this(AppDatabase.getInstance(context))

    fun getAllStages(): Flow<List<PipelineStageInfo>> {
        return database.pipelineStageDao().getAllStages().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getAllBomItems(): Flow<List<BomItem>> {
        return database.bomItemDao().getAllItems().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun updateStageStatus(stageNumber: Int, status: StageStatus) {
        database.pipelineStageDao().updateStageStatus(stageNumber, status.name)
    }

    suspend fun seedInitialDataIfEmpty() {
        if (database.pipelineStageDao().getCount() == 0) {
            val stages = listOf(
                PipelineStageEntity(14, "Documentation Assistance", "Verified KYC, discom load certificate & site NOC", "04 Nov", "COMPLETED"),
                PipelineStageEntity(15, "Subsidy Portal Sanction", "National PM Surya Ghar Portal DBT linked", "Approved ₹78,000", "COMPLETED"),
                PipelineStageEntity(16, "Order Confirmation", "Vendor billing validated against inventory reserve", "Processed", "COMPLETED"),
                PipelineStageEntity(17, "Procurement & Dispatch", "Dispatched via UP-Logistics vehicle UP-53-AT-9012", "Lucknow Hub", "COMPLETED"),
                PipelineStageEntity(18, "Material Delivery Verification", "Awaiting site unload scan & recipient OTP verification", "Active Site", "ACTIVE"),
                PipelineStageEntity(19, "Structure & Module Mounting", "Foundation civil works & tilt bracket alignment", "12 Nov 2026", "UPCOMING")
            )
            database.pipelineStageDao().upsertStages(stages)
        }

        if (database.bomItemDao().getCount() == 0) {
            val bom = listOf(
                BomItemEntity("BOM-1", "HBX-Tier1 550W Mono PERC", "Bifacial High Efficiency • 6 Units", "6/6 Transit"),
                BomItemEntity("BOM-2", "HBX PureWave-3K Inverter", "3.3 kW On-Grid • Single Phase", "1/1 Packaged"),
                BomItemEntity("BOM-3", "Galvanized Iron HD Structure", "15° Tilt Angle • Anti-Corrosion kit", "Complete Set")
            )
            database.bomItemDao().upsertItems(bom)
        }
    }

    private fun PipelineStageEntity.toDomainModel(): PipelineStageInfo {
        val st = try {
            StageStatus.valueOf(status)
        } catch (_: Exception) {
            StageStatus.UPCOMING
        }
        return PipelineStageInfo(
            stageNumber = stageNumber,
            title = title,
            description = description,
            status = st,
            dateOrSubtext = dateOrInfo
        )
    }

    private fun BomItemEntity.toDomainModel(): BomItem {
        return BomItem(
            id = id,
            title = name,
            specs = specs,
            quantity = quantityStatus,
            status = quantityStatus,
            isVerified = true
        )
    }
}
