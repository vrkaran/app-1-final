package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.DocumentRepository
import com.example.data.repository.LeadRepository
import com.example.data.repository.PaymentRepository
import com.example.data.repository.ProjectRepository
import com.example.data.sync.SyncEngine
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class HBXRepository(
    private val context: Context? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase? = context?.let { AppDatabase.getInstance(it) }
    private val syncEngine: SyncEngine? = context?.let { SyncEngine(it) }

    private val leadRepository: LeadRepository? = if (database != null && syncEngine != null) {
        LeadRepository(database, syncEngine)
    } else null

    private val paymentRepository: PaymentRepository? = if (database != null && syncEngine != null) {
        PaymentRepository(database, syncEngine)
    } else null

    private val projectRepository: ProjectRepository? = database?.let { ProjectRepository(it) }

    val documentRepository: DocumentRepository? = context?.let { DocumentRepository(it) }

    val allDocuments: Flow<List<DocumentItem>> = documentRepository?.getAllDocumentsFlow() ?: emptyFlow()

    fun getDocumentsForParent(parentType: String, parentId: String): Flow<List<DocumentItem>> {
        return documentRepository?.getDocumentsForParentFlow(parentType, parentId) ?: emptyFlow()
    }

    // Current User Persona / Role
    private val _currentRole = MutableStateFlow(UserRole.BDE)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun switchRole(role: UserRole) {
        _currentRole.value = role
    }

    // Attendance State
    private val _isClockedIn = MutableStateFlow(true)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn.asStateFlow()

    private val _isOnBreak = MutableStateFlow(false)
    val isOnBreak: StateFlow<Boolean> = _isOnBreak.asStateFlow()

    private val _attendanceLog = MutableStateFlow(
        listOf(
            "Clocked in: 08:30 AM (GPS Geofence Verified ±4.2m)",
            "Assigned to Bilariyaganj & Sagri Sectors"
        )
    )
    val attendanceLog: StateFlow<List<String>> = _attendanceLog.asStateFlow()

    fun toggleClockIn() {
        val willClockIn = !_isClockedIn.value
        _isClockedIn.value = willClockIn
        if (willClockIn) {
            _isOnBreak.value = false
            _attendanceLog.update { listOf("Clocked in at 08:45 AM (Geofence verified)") + it }
        } else {
            _attendanceLog.update { listOf("Shift egress recorded at 05:30 PM") + it }
        }
    }

    fun toggleBreak() {
        val nextBreak = !_isOnBreak.value
        _isOnBreak.value = nextBreak
        if (nextBreak) {
            _attendanceLog.update { listOf("Field break started (60m quota)") + it }
        } else {
            _attendanceLog.update { listOf("Field break concluded • Resumed operations") + it }
        }
    }

    // CRM Leads Flow
    private val _leads = MutableStateFlow<List<Lead>>(emptyList())
    val leads: StateFlow<List<Lead>> = _leads.asStateFlow()

    // Site Survey Parameters & Evidence
    private val _roofSubstrate = MutableStateFlow(RoofSubstrate.RCC_FLAT)
    val roofSubstrate: StateFlow<RoofSubstrate> = _roofSubstrate.asStateFlow()

    fun setRoofSubstrate(substrate: RoofSubstrate) {
        _roofSubstrate.value = substrate
    }

    private val _unshadedAreaSqFt = MutableStateFlow(380)
    val unshadedAreaSqFt: StateFlow<Int> = _unshadedAreaSqFt.asStateFlow()

    fun setUnshadedArea(area: Int) {
        _unshadedAreaSqFt.value = area
    }

    private val _evidencePhotos = MutableStateFlow(
        listOf(
            EvidencePhotoItem(1, "House Front", "Verified • Retake?", "GPS Match", true, false, "GPS Match"),
            EvidencePhotoItem(2, "Rooftop Overview", "Completed", "12:15 PM", true, false),
            EvidencePhotoItem(3, "Usable Roof Area", "Completed", "380 sq.ft", true, false, "380 sq.ft"),
            EvidencePhotoItem(4, "Shadows & Obstacles", "! Action Required", "TAP TO SHOOT", false, true),
            EvidencePhotoItem(5, "Electricity Meter", "Meter Reading", "Capture Dial", false, false),
            EvidencePhotoItem(6, "Latest Bill", "Pending", "UPPCL Document", false, false),
            EvidencePhotoItem(7, "Inverter & DB Box", "Pending", "DB & Cable Inset", false, false),
            EvidencePhotoItem(8, "Earthing Point", "Pending", "Grounding Path", false, false)
        )
    )
    val evidencePhotos: StateFlow<List<EvidencePhotoItem>> = _evidencePhotos.asStateFlow()

    fun capturePhoto(step: Int) {
        _evidencePhotos.update { list ->
            list.map {
                if (it.step == step) {
                    it.copy(subtitle = "Captured & Stored in Local Draft", isCompleted = true, requiresAction = false)
                } else it
            }
        }
    }

    // Project Pipeline Flow
    private val _pipelineStages = MutableStateFlow<List<PipelineStageInfo>>(
        listOf(
            PipelineStageInfo(14, "Documentation Assistance", "Verified KYC, discom load certificate & site NOC", StageStatus.COMPLETED, "04 Nov"),
            PipelineStageInfo(15, "Subsidy Portal Sanction", "National PM Surya Ghar Portal DBT linked", StageStatus.COMPLETED, "Approved ₹78,000"),
            PipelineStageInfo(16, "Order Confirmation", "Vendor billing validated against inventory reserve", StageStatus.COMPLETED, "Processed"),
            PipelineStageInfo(17, "Procurement & Dispatch", "Dispatched via UP-Logistics vehicle UP-53-AT-9012", StageStatus.COMPLETED, "Lucknow Hub"),
            PipelineStageInfo(18, "Material Delivery Verification", "Awaiting site unload scan & recipient OTP verification", StageStatus.ACTIVE, "Active Site"),
            PipelineStageInfo(19, "Structure & Module Mounting", "Foundation civil works & tilt bracket alignment", StageStatus.UPCOMING, "12 Nov 2026")
        )
    )
    val pipelineStages: StateFlow<List<PipelineStageInfo>> = _pipelineStages.asStateFlow()

    // Bill of Materials Flow
    private val _bomItems = MutableStateFlow<List<BomItem>>(
        listOf(
            BomItem("BOM-1", "HBX-Tier1 550W Mono PERC", "Bifacial High Efficiency • 6 Units", "6 Units", "6/6 Transit", true),
            BomItem("BOM-2", "HBX PureWave-3K Inverter", "3.3 kW On-Grid • Single Phase", "1 Unit", "1/1 Packaged", true),
            BomItem("BOM-3", "Galvanized Iron HD Structure", "15° Tilt Angle • Anti-Corrosion kit", "1 Set", "Complete Set", false)
        )
    )
    val bomItems: StateFlow<List<BomItem>> = _bomItems.asStateFlow()

    fun verifyBomItem(id: String) {
        _bomItems.update { list ->
            list.map { if (it.id == id) it.copy(isVerified = true, status = "Verified at Site") else it }
        }
    }

    // Payments Ledger Flow
    private val _payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    val payments: StateFlow<List<PaymentRecord>> = _payments.asStateFlow()

    // Operational Approvals
    private val _approvalQueue = MutableStateFlow(
        listOf(
            ApprovalItem(
                requestId = "APP-2026-881",
                domain = "BDA Recruitment",
                districtHub = "Bilariyaganj Sector",
                applicant = "Pradeep Yadav",
                applicantSub = "Score: 94% • Background Clear",
                valuation = "₹32,000/mo CTC",
                slaDeadline = "4h 12m left"
            ),
            ApprovalItem(
                requestId = "QTE-2026-402",
                domain = "C&I 25kW EPC",
                districtHub = "Sagri Block",
                applicant = "Kisan Cold Storage",
                applicantSub = "3-Phase Agro Sanction",
                valuation = "₹18,50,000",
                slaDeadline = "1h 05m left",
                isCritical = true
            ),
            ApprovalItem(
                requestId = "INC-2026-119",
                domain = "Field Incentives",
                districtHub = "Azamgarh Central Hub",
                applicant = "24 Field Sales Agents",
                applicantSub = "Fortnightly Target Tier-A",
                valuation = "₹3,42,000",
                slaDeadline = "8h 30m left"
            ),
            ApprovalItem(
                requestId = "SURV-2026-092",
                domain = "Complex Shadow Roof",
                districtHub = "Civil Lines, Azamgarh",
                applicant = "Amit Singh (3.3 kWp)",
                applicantSub = "LIDAR 3D Model Verified",
                valuation = "Structure: High-T",
                slaDeadline = "Ready to Sign"
            )
        )
    )
    val approvalQueue: StateFlow<List<ApprovalItem>> = _approvalQueue.asStateFlow()

    init {
        val currentUid = Firebase.auth.currentUser?.uid ?: "DEMO_USER_UID"
        scope.launch {
            leadRepository?.seedInitialLeadsIfEmpty(currentUid)
            paymentRepository?.seedInitialPaymentsIfEmpty(currentUid)
            seedInitialDocumentsIfEmpty(currentUid)

            // Observe Room leads
            leadRepository?.getAllLeads()?.collect { roomLeads ->
                _leads.value = roomLeads
            }
        }

        scope.launch {
            paymentRepository?.getAllPayments()?.collect { roomPayments ->
                _payments.value = roomPayments
            }
        }

        // Start Firestore real-time synchronization
        syncEngine?.startRealtimeListeners()
    }

    fun addLead(name: String, phone: String, village: String, capacity: String, priority: LeadPriority) {
        val uid = Firebase.auth.currentUser?.uid ?: "OFFLINE_BDE"
        scope.launch {
            if (leadRepository != null) {
                leadRepository.createLead(
                    name = name,
                    phone = phone,
                    village = village,
                    capacity = capacity,
                    priority = priority,
                    assignedTo = _currentRole.value.displayName,
                    userId = uid
                )
            } else {
                val newLead = Lead(
                    id = "LEAD-${(100..999).random()}",
                    name = name,
                    phone = phone,
                    village = village,
                    stage = LeadStage.NEW_LEADS,
                    priority = priority,
                    capacity = capacity,
                    systemType = "On-Grid (PM Surya Ghar)",
                    assignedTo = "Rahul Kumar (You)",
                    followUpTime = "Tomorrow 10:00 AM",
                    quoteRef = "#HBX-${(1000..9999).random()}",
                    valueEstimate = "₹1,98,000",
                    notes = listOf("Created via Mobile Field CRM")
                )
                _leads.update { listOf(newLead) + it }
            }
        }
    }

    fun addNoteToLead(leadId: String, note: String) {
        _leads.update { list ->
            list.map {
                if (it.id == leadId) {
                    it.copy(notes = listOf(note) + it.notes)
                } else it
            }
        }
    }

    fun addLeadNote(leadId: String, note: String) {
        addNoteToLead(leadId, note)
    }

    fun updatePipelineStage(stageNumber: Int, newStatus: StageStatus) {
        _pipelineStages.update { list ->
            list.map { if (it.stageNumber == stageNumber) it.copy(status = newStatus) else it }
        }
    }

    fun approveRequest(id: String) {
        _approvalQueue.update { list ->
            list.map { if (it.requestId == id) it.copy(status = "Approved") else it }
        }
    }

    fun addPayment(
        customerName: String,
        consumerId: String,
        amount: Double,
        method: String,
        referenceNo: String
    ) {
        val uid = Firebase.auth.currentUser?.uid ?: "OFFLINE_COLLECTOR"
        scope.launch {
            if (paymentRepository != null) {
                paymentRepository.addPayment(
                    customerName = customerName,
                    consumerId = consumerId,
                    amount = amount,
                    method = method,
                    referenceNo = referenceNo,
                    userId = uid,
                    verifierRole = _currentRole.value.displayName
                )
            } else {
                val newPayment = PaymentRecord(
                    receiptId = "RCP-2026-${(1000..9999).random()}",
                    customerName = customerName,
                    consumerId = consumerId,
                    amount = amount,
                    method = method,
                    date = "Today",
                    status = "Pending Verification",
                    referenceNo = referenceNo,
                    verifiedBy = _currentRole.value.displayName
                )
                _payments.update { listOf(newPayment) + it }
            }
        }
    }

    fun calculateSolarAdvice(sanctionedLoadKw: Double, monthlyBill: Double, areaSqFt: Int): String {
        val recommendedKw = (monthlyBill / 1000.0).coerceIn(1.0, sanctionedLoadKw)
        val subsidy = when {
            recommendedKw <= 1.0 -> 30000
            recommendedKw <= 2.0 -> 60000
            else -> 78000
        }
        val grossCost = recommendedKw * 60000
        val netPayable = grossCost - subsidy
        val monthlyGenerationKwh = recommendedKw * 120.0
        val monthlySavings = monthlyGenerationKwh * 7.5

        return """
            💡 HBX AI Recommendation:
            • Recommended Plant: %.1f kWp (Covers ~%.0f kWh/mo)
            • Gross Turnkey: ₹%,.0f
            • Govt Central Subsidy: ₹%,d (PM Surya Ghar Direct DBT)
            • Net Customer Cost: ₹%,.0f
            • Est. Monthly Power Savings: ₹%,.0f
            • Required Roof Area: ~%.0f sq.ft (Available: %d sq.ft - PASSED)
        """.trimIndent().format(
            recommendedKw,
            monthlyGenerationKwh,
            grossCost,
            subsidy,
            netPayable,
            monthlySavings,
            recommendedKw * 85,
            areaSqFt
        )
    }

    fun attachDocumentFromBytes(
        fileName: String,
        category: DocumentCategory,
        parentType: String,
        parentId: String,
        description: String,
        bytes: ByteArray
    ) {
        val ctx = context ?: return
        val uid = Firebase.auth.currentUser?.uid ?: "OFFLINE_BDE"
        val user = UserProfile(
            uid = uid,
            fullName = _currentRole.value.displayName,
            role = _currentRole.value.name
        )

        scope.launch {
            try {
                val tempDir = File(ctx.cacheDir, "upload_temp")
                if (!tempDir.exists()) tempDir.mkdirs()
                val tempFile = File(tempDir, fileName)
                FileOutputStream(tempFile).use { it.write(bytes) }

                documentRepository?.enqueueDocumentUpload(
                    fileUri = android.net.Uri.fromFile(tempFile),
                    originalFileName = fileName,
                    category = category,
                    parentEntityType = parentType,
                    parentEntityId = parentId,
                    businessId = parentId,
                    description = description,
                    userProfile = user
                )
            } catch (e: Exception) {
                android.util.Log.e("HBXRepository", "Failed attaching document: ${e.message}")
            }
        }
    }

    fun softDeleteDocument(documentId: String, reason: String) {
        val uid = Firebase.auth.currentUser?.uid ?: "OFFLINE_BDE"
        val user = UserProfile(
            uid = uid,
            fullName = _currentRole.value.displayName,
            role = _currentRole.value.name
        )
        scope.launch {
            documentRepository?.softDeleteDocument(documentId, reason, user)
        }
    }

    private suspend fun seedInitialDocumentsIfEmpty(uid: String) {
        val db = database ?: return
        if (db.documentDao().getDocumentCount() > 0) return

        val initialDocs = listOf(
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-CHALLAN-8821",
                businessId = "HBX-PRJ-2026-0042",
                parentEntityType = "PROJECT",
                parentEntityId = "HBX-PRJ-2026-0042",
                documentCategory = "QUOTATION_PDF",
                documentType = "Dispatch Challan",
                originalFileName = "Dispatch_Challan_CH-8821.pdf",
                storageFileName = "challan_CH-8821_1794418210_e6b77f.pdf",
                storagePath = "projects/HBX-PRJ-2026-0042/installation-evidence/Dispatch_Challan_CH-8821.pdf",
                mimeType = "application/pdf",
                fileExtension = "pdf",
                fileSizeBytes = 1468006, // 1.4 MB
                uploadedByUid = uid,
                uploadedByName = "Lucknow Logistics Hub",
                uploadedByRole = "OPERATIONS_MANAGER",
                uploadedAt = System.currentTimeMillis() - 86400000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "a8f92cd310f847b2c91823901bceaa42",
                description = "Official dispatch manifest for UP-Logistics vehicle UP-53-AT-9012"
            ),
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-SITE-SURVEY-01",
                businessId = "HBX-SRV-2026-084",
                parentEntityType = "SITE_SURVEY",
                parentEntityId = "HBX-SRV-2026-084",
                documentCategory = "SITE_PHOTO",
                documentType = "House Front",
                originalFileName = "House_Front_Azamgarh.jpg",
                storageFileName = "site_photo_SRV-084_house_front.jpg",
                storagePath = "site-surveys/HBX-SRV-2026-084/photos/House_Front_Azamgarh.jpg",
                mimeType = "image/jpeg",
                fileExtension = "jpg",
                fileSizeBytes = 2516582, // 2.4 MB
                uploadedByUid = uid,
                uploadedByName = "Rahul Kumar",
                uploadedByRole = "BDE",
                uploadedAt = System.currentTimeMillis() - 43200000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "b7e12d4a51e628990abcf12431980312",
                description = "GPS tagged house elevation with ±4m precision"
            ),
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-SITE-SURVEY-02",
                businessId = "HBX-SRV-2026-084",
                parentEntityType = "SITE_SURVEY",
                parentEntityId = "HBX-SRV-2026-084",
                documentCategory = "SITE_PHOTO",
                documentType = "Rooftop Overview",
                originalFileName = "Rooftop_Overview_380sqft.jpg",
                storageFileName = "site_photo_SRV-084_rooftop.jpg",
                storagePath = "site-surveys/HBX-SRV-2026-084/photos/Rooftop_Overview_380sqft.jpg",
                mimeType = "image/jpeg",
                fileExtension = "jpg",
                fileSizeBytes = 3250585, // 3.1 MB
                uploadedByUid = uid,
                uploadedByName = "Rahul Kumar",
                uploadedByRole = "BDE",
                uploadedAt = System.currentTimeMillis() - 43100000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "c9a33e5512b988771199aaccbb223344",
                description = "RCC Flat substrate 380 sq.ft unshaded boundary"
            ),
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-BILL-UPPCL-01",
                businessId = "LEAD-001",
                parentEntityType = "LEAD",
                parentEntityId = "LEAD-001",
                documentCategory = "ELECTRICITY_BILL",
                documentType = "Latest Bill",
                originalFileName = "UPPCL_Bill_Aug2024.pdf",
                storageFileName = "uppcl_bill_aug2024.pdf",
                storagePath = "customers/LEAD-001/electricity-bills/UPPCL_Bill_Aug2024.pdf",
                mimeType = "application/pdf",
                fileExtension = "pdf",
                fileSizeBytes = 870400, // 850 KB
                uploadedByUid = uid,
                uploadedByName = "Rahul Kumar",
                uploadedByRole = "BDE",
                uploadedAt = System.currentTimeMillis() - 172800000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "d4f55a1234bcde567890f1234567890a",
                description = "Extracted: Tariffs Domestic LM-1, 4kW sanctioned load & bill cycle"
            ),
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-KYC-AADHAAR-01",
                businessId = "LEAD-001",
                parentEntityType = "LEAD",
                parentEntityId = "LEAD-001",
                documentCategory = "KYC",
                documentType = "Aadhaar Card",
                originalFileName = "Amit_Singh_Aadhaar_Verified.pdf",
                storageFileName = "aadhaar_amit_singh.pdf",
                storagePath = "customers/LEAD-001/kyc/Amit_Singh_Aadhaar_Verified.pdf",
                mimeType = "application/pdf",
                fileExtension = "pdf",
                fileSizeBytes = 1153433, // 1.1 MB
                uploadedByUid = uid,
                uploadedByName = "Rahul Kumar",
                uploadedByRole = "BDE",
                uploadedAt = System.currentTimeMillis() - 172800000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "e6b77f9876543210fedcba0987654321",
                description = "Customer Identity & Address proof for National Portal PM Surya Ghar"
            ),
            com.example.data.local.entities.DocumentEntity(
                documentId = "DOC-PAYMENT-PROOF-01",
                businessId = "RCP-2026-8801",
                parentEntityType = "PAYMENT",
                parentEntityId = "RCP-2026-8801",
                documentCategory = "PAYMENT_PROOF",
                documentType = "UPI Screenshot",
                originalFileName = "UPI_Advance_Payment_UTR9839.jpg",
                storageFileName = "upi_token_8801.jpg",
                storagePath = "payments/RCP-2026-8801/proof/UPI_Advance_Payment_UTR9839.jpg",
                mimeType = "image/jpeg",
                fileExtension = "jpg",
                fileSizeBytes = 460800, // 450 KB
                uploadedByUid = uid,
                uploadedByName = "Rahul Kumar",
                uploadedByRole = "BDE",
                uploadedAt = System.currentTimeMillis() - 7200000L,
                uploadStatus = "COMPLETED",
                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                checksum = "f1c88a44556677889900aabbccddeeff",
                description = "Token advance ₹10,000 reference UTR-98391-44210 logged at Azamgarh"
            )
        )
        db.documentDao().upsertDocuments(initialDocs)
    }
}
