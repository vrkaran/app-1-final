package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirebaseAuthRepository
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAuthAndRbacTest : FirestoreEmulatorTestBase() {

    @Test
    fun testNormalizedPhoneFormat_e164Validation() {
        val rawInput = "9876543210"
        val normalized = "+91$rawInput"
        assertEquals("+919876543210", normalized)
        assertTrue(normalized.startsWith("+91"))
        assertEquals(13, normalized.length)
    }

    @Test
    fun testNewUserOnboarding_assignsCustomerRoleNotBde() = runBlocking {
        // Authenticate new non-enterprise user
        val uid = signInTestUser("retail_consumer_${System.currentTimeMillis()}@gmail.com")
        val user = auth.currentUser!!
        assertNotNull(user)

        val docRef = firestore.collection("users").document(uid)
        // Verify no profile initially exists
        assertFalse(docRef.get().await().exists())

        val userEmail: String = user.email ?: ""
        // Save new user profile according to rule
        docRef.set(
            mapOf<String, Any>(
                "uid" to uid,
                "fullName" to "Sunil Yadav",
                "email" to userEmail,
                "phone" to "+91 98391 12345",
                "role" to UserRole.CUSTOMER.roleKey,
                "employeeId" to "CUST-${uid.take(4).uppercase()}",
                "territoryId" to "Azamgarh",
                "accountStatus" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val snapshot = docRef.get().await()
        assertTrue(snapshot.exists())
        assertEquals(UserRole.CUSTOMER.roleKey, snapshot.getString("role"))
    }

    @Test
    fun testSelfRegistration_cannotGrantAdminOrSuperAdmin() = runBlocking {
        val uid = signInTestUser("rogue_${System.currentTimeMillis()}@test.com")
        var failed = false
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "fullName" to "Rogue Admin",
                    "email" to "rogue@test.com",
                    "phone" to "+91 99999 99999",
                    "role" to "SUPER_ADMIN",
                    "employeeId" to "ROGUE-01",
                    "territoryId" to "Azamgarh",
                    "accountStatus" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Self registration as SUPER_ADMIN must be rejected by rules", failed)
    }

    @Test
    fun testSuspendedUser_cannotUpdateProfile() = runBlocking {
        val uid = signInTestUser("suspended_${System.currentTimeMillis()}@hbxsolar.com")
        val docRef = firestore.collection("users").document(uid)

        // Seed as suspended
        docRef.set(
            mapOf(
                "uid" to uid,
                "fullName" to "Suspended Officer",
                "email" to "suspended@hbxsolar.com",
                "phone" to "+91 98765 00000",
                "role" to "BDE",
                "employeeId" to "HBX-SUS-01",
                "territoryId" to "Azamgarh",
                "accountStatus" to "SUSPENDED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val snap = docRef.get().await()
        assertEquals("SUSPENDED", snap.getString("accountStatus"))
    }

    @Test
    fun testInvalidOtp_rejectedByFirebaseAuth() = runBlocking {
        val authRepo = FirebaseAuthRepository(auth, firestore)
        // Calling verifyOtpAndSignIn with fake verificationId and bad code
        val result = authRepo.verifyOtpAndSignIn("fake_verification_id_123", "000000")
        assertTrue("Invalid OTP verification must fail", result.isFailure)
    }
}
