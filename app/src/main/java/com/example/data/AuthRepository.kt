package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private const val TAG = "FirebaseAuthRepo"

sealed class PhoneVerificationEvent {
    object Idle : PhoneVerificationEvent()
    object Sending : PhoneVerificationEvent()
    data class CodeSent(val verificationId: String, val token: PhoneAuthProvider.ForceResendingToken?) : PhoneVerificationEvent()
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneVerificationEvent()
    data class Failed(val error: String) : PhoneVerificationEvent()
}

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    val userProfile: StateFlow<UserProfile?>
    val isAuthLoading: StateFlow<Boolean>

    // Google Sign-In
    suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfile>

    // Phone OTP Authentication
    fun sendPhoneOtp(
        activity: Activity,
        normalizedPhoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onEvent: (PhoneVerificationEvent) -> Unit
    )
    suspend fun verifyOtpAndSignIn(verificationId: String, smsCode: String): Result<UserProfile>

    // Account Sign-Out
    suspend fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    constructor(context: Context) : this(
        Firebase.auth,
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    override val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    override val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(true)
    override val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private var profileListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                listenToUserProfile(user.uid)
            } else {
                profileListener?.remove()
                profileListener = null
                _userProfile.value = null
                _isAuthLoading.value = false
            }
        }
    }

    private fun listenToUserProfile(uid: String) {
        profileListener?.remove()
        val docRef = db.collection("users").document(uid)
        profileListener = docRef.addSnapshotListener { snapshot, error ->
            _isAuthLoading.value = false
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                _userProfile.value = profile
            } else {
                // If the user profile does not yet exist in Firestore, resolve or wait for onboarding
                scope.launch {
                    val user = auth.currentUser
                    if (user != null && user.uid == uid) {
                        resolveOrCreateInitialProfile(user)
                    }
                }
            }
        }
    }

    private suspend fun resolveOrCreateInitialProfile(user: FirebaseUser): UserProfile {
        val docRef = db.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)!!
            _userProfile.value = existing
            return existing
        }

        // Standard new user initialization with strict CUSTOMER fallback
        val phone = user.phoneNumber ?: ""
        val email = user.email ?: ""
        // Zero-Trust Onboarding: All new self-registered users are strictly CUSTOMER.
        // Staff roles (BDE, Engineer, Admin) must be granted via trusted server-side provisionStaff Cloud Function.
        val initialRole = UserRole.CUSTOMER.roleKey

        val newProfile = UserProfile(
            uid = user.uid,
            fullName = user.displayName ?: if (phone.isNotBlank()) "User $phone" else "HBX User",
            email = email,
            phone = phone.ifBlank { "+91 98391 44210" },
            role = initialRole,
            employeeId = "CUST-${user.uid.take(4).uppercase()}",
            territoryId = "Azamgarh",
            accountStatus = "ACTIVE"
        )

        try {
            docRef.set(
                mapOf(
                    "uid" to newProfile.uid,
                    "fullName" to newProfile.fullName,
                    "email" to newProfile.email,
                    "phone" to newProfile.phone,
                    "role" to newProfile.role,
                    "employeeId" to newProfile.employeeId,
                    "territoryId" to newProfile.territoryId,
                    "accountStatus" to newProfile.accountStatus,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            _userProfile.value = newProfile
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
        return newProfile
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfile> {
        return try {
            _isAuthLoading.value = true
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            val profile = resolveOrCreateInitialProfile(user)
            _isAuthLoading.value = false
            Result.success(profile)
        } catch (e: Exception) {
            _isAuthLoading.value = false
            Log.e(TAG, "Google Sign-in error", e)
            Result.failure(e)
        }
    }

    override fun sendPhoneOtp(
        activity: Activity,
        normalizedPhoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        onEvent: (PhoneVerificationEvent) -> Unit
    ) {
        onEvent(PhoneVerificationEvent.Sending)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification instant auto-retrieval completed.")
                onEvent(PhoneVerificationEvent.AutoVerified(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Phone verification failed: ${e.message}", e)
                onEvent(PhoneVerificationEvent.Failed(e.localizedMessage ?: "Verification failed. Check network or SMS limits."))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "SMS code sent. verificationId: $verificationId")
                onEvent(PhoneVerificationEvent.CodeSent(verificationId, token))
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(normalizedPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    override suspend fun verifyOtpAndSignIn(verificationId: String, smsCode: String): Result<UserProfile> {
        return try {
            _isAuthLoading.value = true
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after phone sign in")
            val profile = resolveOrCreateInitialProfile(user)

            if (profile.accountStatus == "SUSPENDED" || profile.accountStatus == "INACTIVE") {
                auth.signOut()
                _userProfile.value = null
                _isAuthLoading.value = false
                return Result.failure(IllegalStateException("Account is suspended or inactive. Please contact administration."))
            }

            _isAuthLoading.value = false
            Result.success(profile)
        } catch (e: Exception) {
            _isAuthLoading.value = false
            Log.e(TAG, "OTP verification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            profileListener?.remove()
            profileListener = null
            auth.signOut()
            _userProfile.value = null
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
    }
}
