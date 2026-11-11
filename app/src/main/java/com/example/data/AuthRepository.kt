package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    val userProfile: StateFlow<UserProfile?>
    val isAuthLoading: StateFlow<Boolean>
    suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfile>
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
                // First-time sign-in auto provision standard profile
                scope.launch {
                    val user = auth.currentUser
                    if (user != null && user.uid == uid) {
                        val initialProfile = UserProfile(
                            uid = uid,
                            fullName = user.displayName ?: "HBX Team Member",
                            email = user.email ?: "",
                            phone = user.phoneNumber ?: "+91 98391 00000",
                            role = UserRole.BDE.roleKey,
                            employeeId = "HBX-AZM-${uid.take(4).uppercase()}",
                            territoryId = "Azamgarh",
                            accountStatus = "ACTIVE"
                        )
                        try {
                            docRef.set(
                                mapOf(
                                    "uid" to initialProfile.uid,
                                    "fullName" to initialProfile.fullName,
                                    "email" to initialProfile.email,
                                    "phone" to initialProfile.phone,
                                    "role" to initialProfile.role,
                                    "employeeId" to initialProfile.employeeId,
                                    "territoryId" to initialProfile.territoryId,
                                    "accountStatus" to initialProfile.accountStatus,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        } catch (e: Exception) {
                            handleFirestoreError(e, OperationType.CREATE, docRef.path)
                        }
                    }
                }
            }
        }
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfile> {
        return try {
            _isAuthLoading.value = true
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            
            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            val profile = if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)!!
            } else {
                val newProfile = UserProfile(
                    uid = user.uid,
                    fullName = user.displayName ?: "HBX Field Officer",
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "+91 98391 44210",
                    role = UserRole.BDE.roleKey,
                    employeeId = "HBX-AZM-${user.uid.take(4).uppercase()}",
                    territoryId = "Azamgarh",
                    accountStatus = "ACTIVE"
                )
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
                newProfile
            }
            _userProfile.value = profile
            _isAuthLoading.value = false
            Result.success(profile)
        } catch (e: Exception) {
            _isAuthLoading.value = false
            Log.e("FirebaseAuthRepo", "Sign in error", e)
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
            Log.e("FirebaseAuthRepo", "Sign out error", e)
        }
    }
}
