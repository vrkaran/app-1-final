package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirestoreUserRepository
import com.example.model.UserProfile
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfile_createAndObserve_success() = runBlocking {
        val email = "bde_${System.currentTimeMillis()}@hbxsolar.com"
        val uid = signInTestUser(email)
        auth.currentUser?.getIdToken(true)?.await()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val repository = FirestoreUserRepository(firestore)

        // Seed profile
        firestore.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "fullName" to "Alice Sharma",
                "email" to email,
                "phone" to "+91 9876543210",
                "role" to "BDE",
                "employeeId" to "HBX-AZM-001",
                "territoryId" to "Azamgarh",
                "accountStatus" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val profile = withTimeout(5000L) {
            repository.observeUser(uid).first()
        }

        assertNotNull(profile)
        assertEquals("Alice Sharma", profile?.fullName)
        assertEquals("BDE", profile?.role)
        assertEquals("Azamgarh", profile?.territoryId)
    }

    @Test
    fun unauthenticated_cannotReadUsers() = runBlocking {
        auth.signOut()
        var failed = false
        try {
            firestore.collection("users").get().await()
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Unauthenticated read must fail", failed)
    }

    @Test
    fun nonAdmin_cannotUpdateRole() = runBlocking {
        val nonAdminUid = signInTestUser("user_${System.currentTimeMillis()}@hbxsolar.com")
        val repository = FirestoreUserRepository(firestore)

        // Attempting to promote with non-admin caller
        val result = repository.updateUserRole(
            adminUid = nonAdminUid,
            targetUid = nonAdminUid,
            newRole = "SUPER_ADMIN"
        )

        assertTrue("Non-admin role update must fail", result.isFailure)
    }
}
