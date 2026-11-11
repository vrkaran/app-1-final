package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountStatus(val rawValue: String) {
    ACTIVE("ACTIVE"),
    PENDING_APPROVAL("PENDING_APPROVAL"),
    SUSPENDED("SUSPENDED"),
    INACTIVE("INACTIVE")
}

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "BDE",
    val employeeId: String = "",
    val territoryId: String = "Azamgarh",
    val accountStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toUserRole(): UserRole {
        return try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            when (role.uppercase()) {
                "SUPER_ADMIN" -> UserRole.SUPER_ADMIN
                "ADMIN" -> UserRole.ADMIN
                "OPERATIONS_MANAGER" -> UserRole.OPERATIONS_MANAGER
                "SALES_MANAGER" -> UserRole.SALES_MANAGER
                "TEAM_LEADER" -> UserRole.TEAM_LEADER
                "BDE" -> UserRole.BDE
                "INSTALLATION_ENGINEER" -> UserRole.INSTALLATION_ENGINEER
                "SERVICE_ENGINEER" -> UserRole.SERVICE_ENGINEER
                "PARTNER" -> UserRole.PARTNER
                "CUSTOMER" -> UserRole.CUSTOMER
                else -> UserRole.BDE
            }
        }
    }
}
