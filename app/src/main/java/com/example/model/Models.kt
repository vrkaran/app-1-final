package com.example.model

enum class UserRole(val displayName: String, val badge: String, val roleKey: String) {
    SUPER_ADMIN("Dr. V. Verma", "Super Admin • Azamgarh HQ", "SUPER_ADMIN"),
    ADMIN("Pooja Sharma", "Administrator • UP East", "ADMIN"),
    OPERATIONS_MANAGER("Vikram Singh", "Operations Manager", "OPERATIONS_MANAGER"),
    SALES_MANAGER("Anil Tiwari", "Sales Manager", "SALES_MANAGER"),
    TEAM_LEADER("Deepak Rai", "Team Leader • Sector 4", "TEAM_LEADER"),
    BDE("Rahul Kumar", "Senior BDA • Azamgarh Hub", "BDE"),
    INSTALLATION_ENGINEER("Sandeep Maurya", "Lead Installation Engineer", "INSTALLATION_ENGINEER"),
    SERVICE_ENGINEER("Alok Gupta", "Quality & Service Inspector", "SERVICE_ENGINEER"),
    CHANNEL_PARTNER("Purvanchal Solar Works", "Authorized Dealer", "PARTNER"),
    CUSTOMER("Amit Singh", "Solar Consumer • 3.3 kW", "CUSTOMER");

    companion object {
        val PARTNER get() = CHANNEL_PARTNER
        fun fromKey(key: String): UserRole {
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.roleKey.equals(key, ignoreCase = true) } ?: BDE
        }
    }
}

enum class LeadStage(val displayName: String, val badgeBg: Long, val badgeColor: Long) {
    ALL("All", 0xFF0B1428, 0xFFFFFFFF),
    NEW_LEADS("New Leads", 0xFFE0E8FF, 0xFF182235),
    SURVEY_PENDING("Survey Pending", 0xFFFED489, 0xFF5D4202),
    QUOTATION_SHARED("Quotation Shared", 0xFFE8EDFF, 0xFF3977C5),
    APPROVED("Approved", 0xFFE8F5E9, 0xFF16865B)
}

enum class LeadPriority {
    HIGH,
    STANDARD,
    GOVT_SUBSIDY,
    COMMERCIAL
}

data class Lead(
    val id: String,
    val name: String,
    val phone: String,
    val village: String,
    val stage: LeadStage,
    val priority: LeadPriority,
    val capacity: String,
    val systemType: String,
    val assignedTo: String,
    val followUpTime: String,
    val quoteRef: String,
    val valueEstimate: String,
    val notes: List<String> = emptyList()
)

data class PipelineStageInfo(
    val stageNumber: Int,
    val title: String,
    val description: String,
    val status: StageStatus, // COMPLETED, ACTIVE, UPCOMING
    val dateOrSubtext: String
)

enum class StageStatus {
    COMPLETED,
    ACTIVE,
    UPCOMING
}

data class BomItem(
    val id: String,
    val title: String,
    val specs: String,
    val quantity: String,
    val status: String,
    val isVerified: Boolean = false
)

data class FieldPersonnel(
    val name: String,
    val role: String,
    val initials: String,
    val phone: String
)

enum class RoofSubstrate {
    RCC_FLAT,
    TIN_SHED,
    TILED_ROOF
}

data class EvidencePhotoItem(
    val step: Int,
    val title: String,
    val subtitle: String,
    val statusText: String,
    val isCompleted: Boolean,
    val requiresAction: Boolean = false,
    val badgeTag: String? = null
)

data class ApprovalItem(
    val requestId: String,
    val domain: String,
    val districtHub: String,
    val applicant: String,
    val applicantSub: String,
    val valuation: String,
    val slaDeadline: String,
    val isCritical: Boolean = false,
    val status: String = "Pending"
)

data class DistrictBenchmark(
    val hubName: String,
    val targetRealizedPct: Int,
    val commissionedKwp: String,
    val subsidyTat: String,
    val techEfficiency: String,
    val isHq: Boolean = false
)

data class PaymentRecord(
    val receiptId: String,
    val customerName: String,
    val consumerId: String,
    val amount: Double,
    val method: String, // "Cash", "UPI", "Bank Transfer", "Cheque"
    val date: String,
    val status: String, // "Verified", "Pending Verification"
    val referenceNo: String,
    val verifiedBy: String
)

data class SupportTicket(
    val ticketId: String,
    val customerName: String,
    val issue: String,
    val category: String,
    val status: String,
    val priority: String,
    val dateReported: String
)
