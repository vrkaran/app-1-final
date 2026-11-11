package com.example.data

import android.content.Context
import com.example.R
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserRepository {
    fun observeUser(uid: String): Flow<UserProfile?>
    fun observeAllUsers(): Flow<List<UserProfile>>
    suspend fun updateUserRole(adminUid: String, targetUid: String, newRole: String): Result<Unit>
    suspend fun updateAccountStatus(adminUid: String, targetUid: String, newStatus: String): Result<Unit>
}

class FirestoreUserRepository(
    private val db: FirebaseFirestore
) : UserRepository {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
    )

    override fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun observeAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val colRef = db.collection("users")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateUserRole(adminUid: String, targetUid: String, newRole: String): Result<Unit> {
        return try {
            val adminDoc = db.collection("users").document(adminUid).get().await()
            val adminProfile = adminDoc.toObject(UserProfile::class.java)
            val adminRole = adminProfile?.role ?: ""
            if (adminRole != "SUPER_ADMIN" && adminRole != "ADMIN") {
                return Result.failure(SecurityException("Unauthorized: Only Admins can modify employee roles."))
            }

            db.collection("users").document(targetUid).update(
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    override suspend fun updateAccountStatus(adminUid: String, targetUid: String, newStatus: String): Result<Unit> {
        return try {
            val adminDoc = db.collection("users").document(adminUid).get().await()
            val adminProfile = adminDoc.toObject(UserProfile::class.java)
            val adminRole = adminProfile?.role ?: ""
            if (adminRole != "SUPER_ADMIN" && adminRole != "ADMIN" && adminRole != "OPERATIONS_MANAGER") {
                return Result.failure(SecurityException("Unauthorized: Insufficient privileges."))
            }

            db.collection("users").document(targetUid).update(
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }
}
