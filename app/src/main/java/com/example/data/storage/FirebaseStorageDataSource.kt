package com.example.data.storage

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

private const val TAG = "HBXStorageDS"

sealed class StorageUploadState {
    data class Progress(val bytesTransferred: Long, val totalBytes: Long, val fraction: Float) : StorageUploadState()
    data class Success(val storagePath: String, val downloadUrl: String, val sizeBytes: Long) : StorageUploadState()
    data class Error(val exception: Throwable) : StorageUploadState()
}

class FirebaseStorageDataSource(
    private val storage: FirebaseStorage = Firebase.storage
) {

    fun uploadFileWithProgress(
        storagePath: String,
        localFile: File,
        mimeType: String,
        customMetadata: Map<String, String> = emptyMap()
    ): Flow<StorageUploadState> = callbackFlow {
        val storageRef = storage.reference.child(storagePath)

        val metadataBuilder = StorageMetadata.Builder()
            .setContentType(mimeType)

        customMetadata.forEach { (key, value) ->
            metadataBuilder.setCustomMetadata(key, value)
        }

        val uploadTask = storageRef.putFile(Uri.fromFile(localFile), metadataBuilder.build())

        uploadTask.addOnProgressListener { taskSnapshot ->
            val totalBytes = taskSnapshot.totalByteCount
            val bytesTransferred = taskSnapshot.bytesTransferred
            val fraction = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes else 0f
            trySend(StorageUploadState.Progress(bytesTransferred, totalBytes, fraction))
        }.addOnSuccessListener { _ ->
            // Obtain download URL
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                trySend(StorageUploadState.Success(storagePath, uri.toString(), localFile.length()))
                close()
            }.addOnFailureListener { ex ->
                // Even if download URL fetch fails, upload succeeded
                trySend(StorageUploadState.Success(storagePath, "", localFile.length()))
                close()
            }
        }.addOnFailureListener { ex ->
            Log.e(TAG, "Storage upload failed for $storagePath: ${ex.message}")
            trySend(StorageUploadState.Error(ex))
            close(ex)
        }

        awaitClose {
            if (uploadTask.isInProgress) {
                uploadTask.cancel()
            }
        }
    }

    suspend fun getDownloadUrl(storagePath: String): String {
        return try {
            storage.reference.child(storagePath).downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed getting download URL for $storagePath: ${e.message}")
            ""
        }
    }

    suspend fun deleteFile(storagePath: String): Boolean {
        return try {
            storage.reference.child(storagePath).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting storage file $storagePath: ${e.message}")
            false
        }
    }
}
