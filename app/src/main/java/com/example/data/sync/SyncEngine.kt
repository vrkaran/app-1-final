package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.LeadEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.SyncOperationEntity
import com.example.data.local.entities.SyncStatus
import com.example.data.storage.FirebaseStorageDataSource
import com.example.data.storage.StorageUploadState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val TAG = "HBXSyncEngine"

class SyncEngine(
    private val context: Context,
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(
        context,
        AppDatabase.getInstance(context),
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var leadsListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null
    private var documentsListener: ListenerRegistration? = null

    init {
        registerNetworkCallback()
        observePendingQueue()
    }

    private fun registerNetworkCallback() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                Log.d(TAG, "Network available. Triggering sync queue processing.")
                triggerSync()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                Log.d(TAG, "Network lost. Switching to offline-first cache mode.")
            }
        })
    }

    private fun observePendingQueue() {
        scope.launch {
            database.syncQueueDao().observePendingCount().collect { count ->
                _pendingSyncCount.value = count
            }
        }
    }

    fun startRealtimeListeners() {
        val authUser = Firebase.auth.currentUser ?: return

        // 1. Listen to Leads Collection
        leadsListener?.remove()
        leadsListener = firestore.collection("leads")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Leads snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            reconcileRemoteLead(doc)
                        }
                    }
                }
            }

        // 2. Listen to Payments Collection
        paymentsListener?.remove()
        paymentsListener = firestore.collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Payments snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            reconcileRemotePayment(doc)
                        }
                    }
                }
            }

        // 3. Listen to Documents Collection
        documentsListener?.remove()
        documentsListener = firestore.collection("documents")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Documents snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            reconcileRemoteDocument(doc)
                        }
                    }
                }
            }
    }

    private suspend fun reconcileRemoteLead(doc: DocumentSnapshot) {
        val id = doc.id
        val localLead = database.leadDao().getLeadById(id)

        // Conflict check: if local has un-uploaded edits, do not blindly overwrite
        if (localLead != null && localLead.syncStatus == SyncStatus.PENDING_UPLOAD) {
            Log.w(TAG, "Local lead $id has pending upload. Postponing remote overwrite.")
            return
        }

        val name = doc.getString("name") ?: return
        val phone = doc.getString("phone") ?: ""
        val village = doc.getString("village") ?: ""
        val stage = doc.getString("stage") ?: "NEW_LEADS"
        val priority = doc.getString("priority") ?: "STANDARD"
        val capacity = doc.getString("capacity") ?: "3 kW"
        val systemType = doc.getString("systemType") ?: "On-Grid"
        val assignedTo = doc.getString("assignedTo") ?: ""
        val assignedToUid = doc.getString("assignedToUid") ?: ""
        val followUpTime = doc.getString("followUpTime") ?: ""
        val quoteRef = doc.getString("quoteRef") ?: ""
        val valueEstimate = doc.getString("valueEstimate") ?: ""
        val userId = doc.getString("userId") ?: ""
        val serverTime = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()

        val notesList = doc.get("notes") as? List<*> ?: emptyList<Any>()
        val jsonArray = JSONArray()
        notesList.forEach { jsonArray.put(it.toString()) }

        val remoteEntity = LeadEntity(
            id = id,
            name = name,
            phone = phone,
            village = village,
            stage = stage,
            priority = priority,
            capacity = capacity,
            systemType = systemType,
            assignedTo = assignedTo,
            assignedToUid = assignedToUid,
            followUpTime = followUpTime,
            quoteRef = quoteRef,
            valueEstimate = valueEstimate,
            notesJson = jsonArray.toString(),
            userId = userId,
            syncStatus = SyncStatus.SYNCED,
            localUpdatedAt = System.currentTimeMillis(),
            serverUpdatedAt = serverTime
        )

        database.leadDao().upsertLead(remoteEntity)
    }

    private suspend fun reconcileRemotePayment(doc: DocumentSnapshot) {
        val receiptId = doc.id
        val customerName = doc.getString("customerName") ?: return
        val consumerId = doc.getString("consumerId") ?: ""
        val amount = doc.getDouble("amount") ?: 0.0
        val method = doc.getString("method") ?: "UPI"
        val referenceNo = doc.getString("referenceNo") ?: ""
        val status = doc.getString("status") ?: "Verified"
        val verifiedBy = doc.getString("verifiedBy") ?: "HBX Finance"
        val userId = doc.getString("userId") ?: ""
        val dateString = doc.getString("date") ?: "Today"

        val entity = PaymentEntity(
            receiptId = receiptId,
            customerName = customerName,
            consumerId = consumerId,
            amount = amount,
            method = method,
            referenceNo = referenceNo,
            status = status,
            verifiedBy = verifiedBy,
            userId = userId,
            dateString = dateString,
            syncStatus = SyncStatus.SYNCED,
            localTimestamp = System.currentTimeMillis()
        )

        database.paymentDao().upsertPayment(entity)
    }

    fun triggerSync() {
        if (!_isOnline.value) return
        scope.launch {
            processPendingQueue()
        }
    }

    suspend fun processPendingQueue() = withContext(Dispatchers.IO) {
        val pendingOps = database.syncQueueDao().getPendingOperations()
        if (pendingOps.isEmpty()) return@withContext

        Log.d(TAG, "Processing ${pendingOps.size} pending offline operations.")

        for (op in pendingOps) {
            try {
                when (op.entityType) {
                    "LEAD" -> syncLeadOperation(op)
                    "PAYMENT" -> syncPaymentOperation(op)
                    "DOCUMENT" -> syncDocumentOperation(op)
                }
                database.syncQueueDao().removeOperation(op.operationId)
            } catch (e: Exception) {
                Log.e(TAG, "Sync operation failed for ${op.operationId}: ${e.message}")
                database.syncQueueDao().updateOperationStatus(
                    id = op.operationId,
                    status = SyncStatus.FAILED,
                    error = e.localizedMessage,
                    time = System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun syncLeadOperation(op: SyncOperationEntity) {
        val json = JSONObject(op.payloadJson)
        val leadId = op.entityId

        val docRef = firestore.collection("leads").document(leadId)

        val notesJsonArray = json.optJSONArray("notes") ?: JSONArray()
        val notes = mutableListOf<String>()
        for (i in 0 until notesJsonArray.length()) {
            notes.add(notesJsonArray.getString(i))
        }

        val map = hashMapOf<String, Any>(
            "id" to leadId,
            "name" to json.getString("name"),
            "phone" to json.getString("phone"),
            "village" to json.getString("village"),
            "stage" to json.getString("stage"),
            "priority" to json.getString("priority"),
            "capacity" to json.getString("capacity"),
            "systemType" to json.getString("systemType"),
            "assignedTo" to json.optString("assignedTo", ""),
            "assignedToUid" to json.optString("assignedToUid", op.userId),
            "followUpTime" to json.optString("followUpTime", ""),
            "quoteRef" to json.optString("quoteRef", ""),
            "valueEstimate" to json.optString("valueEstimate", ""),
            "notes" to notes,
            "userId" to op.userId,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(map).await()
        database.leadDao().updateSyncStatus(leadId, SyncStatus.SYNCED, System.currentTimeMillis())
        Log.d(TAG, "Successfully synced lead $leadId to Firestore.")
    }

    private suspend fun syncPaymentOperation(op: SyncOperationEntity) {
        val json = JSONObject(op.payloadJson)
        val receiptId = op.entityId

        val docRef = firestore.collection("payments").document(receiptId)

        val map = hashMapOf<String, Any>(
            "receiptId" to receiptId,
            "customerName" to json.getString("customerName"),
            "consumerId" to json.getString("consumerId"),
            "amount" to json.getDouble("amount"),
            "method" to json.getString("method"),
            "referenceNo" to json.getString("referenceNo"),
            "status" to "Pending Verification", // Offline payments remain pending until server verification
            "verifiedBy" to json.optString("verifiedBy", "Pending Review"),
            "userId" to op.userId,
            "date" to json.optString("date", "Today"),
            "createdAt" to FieldValue.serverTimestamp()
        )

        docRef.set(map).await()
        database.paymentDao().updatePaymentStatus(receiptId, SyncStatus.SYNCED, "Pending Verification")
        Log.d(TAG, "Successfully synced payment $receiptId to Firestore.")
    }

    suspend fun enqueueLeadCreate(lead: LeadEntity, userId: String) {
        val json = JSONObject().apply {
            put("id", lead.id)
            put("name", lead.name)
            put("phone", lead.phone)
            put("village", lead.village)
            put("stage", lead.stage)
            put("priority", lead.priority)
            put("capacity", lead.capacity)
            put("systemType", lead.systemType)
            put("assignedTo", lead.assignedTo)
            put("assignedToUid", lead.assignedToUid)
            put("followUpTime", lead.followUpTime)
            put("quoteRef", lead.quoteRef)
            put("valueEstimate", lead.valueEstimate)
            put("notes", JSONArray(lead.notesJson))
        }

        val op = SyncOperationEntity(
            operationId = UUID.randomUUID().toString(),
            entityType = "LEAD",
            entityId = lead.id,
            operationType = "CREATE",
            payloadJson = json.toString(),
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        database.syncQueueDao().enqueue(op)
        triggerSync()
    }

    suspend fun enqueuePaymentCreate(payment: PaymentEntity, userId: String) {
        val json = JSONObject().apply {
            put("receiptId", payment.receiptId)
            put("customerName", payment.customerName)
            put("consumerId", payment.consumerId)
            put("amount", payment.amount)
            put("method", payment.method)
            put("referenceNo", payment.referenceNo)
            put("verifiedBy", payment.verifiedBy)
            put("date", payment.dateString)
        }

        val op = SyncOperationEntity(
            operationId = UUID.randomUUID().toString(),
            entityType = "PAYMENT",
            entityId = payment.receiptId,
            operationType = "CREATE",
            payloadJson = json.toString(),
            userId = userId,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        database.syncQueueDao().enqueue(op)
        triggerSync()
    }

    private suspend fun syncDocumentOperation(op: SyncOperationEntity) {
        val json = JSONObject(op.payloadJson)
        val documentId = op.entityId
        val storagePath = json.getString("storagePath")
        val localFilePath = json.optString("localFilePath", "")
        val mimeType = json.optString("mimeType", "application/octet-stream")
        val file = File(localFilePath)

        val localDoc = database.documentDao().getDocumentById(documentId)
        if (localDoc == null) {
            database.syncQueueDao().removeOperation(op.operationId)
            return
        }

        var downloadUrl = localDoc.downloadUrl ?: ""

        // Only upload bytes if not already uploaded
        if (localDoc.uploadStatus != "UPLOADED" && localDoc.uploadStatus != "METADATA_PENDING" && localDoc.uploadStatus != "COMPLETED") {
            if (!file.exists()) {
                database.documentDao().updateUploadStatus(documentId, "FAILED", SyncStatus.FAILED, System.currentTimeMillis())
                return
            }

            database.documentDao().updateUploadStatus(documentId, "UPLOADING", SyncStatus.SYNCING, System.currentTimeMillis())
            val storageDS = FirebaseStorageDataSource()

            val customMetadata = mapOf(
                "documentId" to documentId,
                "businessId" to localDoc.businessId,
                "parentEntityType" to localDoc.parentEntityType,
                "parentEntityId" to localDoc.parentEntityId,
                "uploadedByUid" to op.userId,
                "category" to localDoc.documentCategory
            )

            storageDS.uploadFileWithProgress(storagePath, file, mimeType, customMetadata).collect { state ->
                if (state is StorageUploadState.Success) {
                    downloadUrl = state.downloadUrl
                }
            }
        }

        // Post Firestore metadata
        val metadataMap = hashMapOf<String, Any>(
            "documentId" to documentId,
            "businessId" to localDoc.businessId,
            "parentEntityType" to localDoc.parentEntityType,
            "parentEntityId" to localDoc.parentEntityId,
            "documentCategory" to localDoc.documentCategory,
            "documentType" to localDoc.documentType,
            "originalFileName" to localDoc.originalFileName,
            "storagePath" to storagePath,
            "downloadUrl" to downloadUrl,
            "mimeType" to mimeType,
            "fileSizeBytes" to localDoc.fileSizeBytes,
            "uploadedByUid" to op.userId,
            "uploadedByName" to localDoc.uploadedByName,
            "uploadedByRole" to localDoc.uploadedByRole,
            "uploadedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "checksum" to (localDoc.checksum ?: ""),
            "version" to localDoc.version,
            "isDeleted" to localDoc.isDeleted
        )

        firestore.collection("documents").document(documentId).set(metadataMap).await()

        database.documentDao().updateUploadSuccess(
            id = documentId,
            status = "COMPLETED",
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            syncStatus = SyncStatus.SYNCED,
            time = System.currentTimeMillis()
        )
        Log.d(TAG, "Successfully synced document $documentId to Cloud Storage and Firestore.")
    }

    private suspend fun reconcileRemoteDocument(doc: DocumentSnapshot) {
        val documentId = doc.id
        val localDoc = database.documentDao().getDocumentById(documentId)
        if (localDoc != null && (localDoc.syncStatus == SyncStatus.PENDING_UPLOAD || localDoc.uploadStatus == "UPLOADING")) {
            return
        }

        val businessId = doc.getString("businessId") ?: ""
        val parentEntityType = doc.getString("parentEntityType") ?: "CUSTOMER"
        val parentEntityId = doc.getString("parentEntityId") ?: ""
        val documentCategory = doc.getString("documentCategory") ?: "OTHER"
        val documentType = doc.getString("documentType") ?: "Document"
        val originalFileName = doc.getString("originalFileName") ?: "file"
        val storagePath = doc.getString("storagePath") ?: ""
        val downloadUrl = doc.getString("downloadUrl")
        val mimeType = doc.getString("mimeType") ?: "application/octet-stream"
        val fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0L
        val uploadedByUid = doc.getString("uploadedByUid") ?: ""
        val uploadedByName = doc.getString("uploadedByName") ?: ""
        val uploadedByRole = doc.getString("uploadedByRole") ?: ""
        val uploadedAt = doc.getTimestamp("uploadedAt")?.toDate()?.time ?: System.currentTimeMillis()
        val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val checksum = doc.getString("checksum")
        val version = doc.getLong("version")?.toInt() ?: 1

        val entity = DocumentEntity(
            documentId = documentId,
            businessId = businessId,
            parentEntityType = parentEntityType,
            parentEntityId = parentEntityId,
            documentCategory = documentCategory,
            documentType = documentType,
            originalFileName = originalFileName,
            storageFileName = storagePath.substringAfterLast("/"),
            storagePath = storagePath,
            localFilePath = localDoc?.localFilePath,
            mimeType = mimeType,
            fileExtension = originalFileName.substringAfterLast(".", "jpg"),
            fileSizeBytes = fileSizeBytes,
            uploadedByUid = uploadedByUid,
            uploadedByName = uploadedByName,
            uploadedByRole = uploadedByRole,
            uploadedAt = uploadedAt,
            updatedAt = updatedAt,
            uploadStatus = "COMPLETED",
            syncStatus = SyncStatus.SYNCED,
            checksum = checksum,
            thumbnailPath = localDoc?.thumbnailPath,
            description = doc.getString("description"),
            isDeleted = isDeleted,
            version = version,
            downloadUrl = downloadUrl
        )

        database.documentDao().upsertDocument(entity)
    }

    fun stop() {
        leadsListener?.remove()
        paymentsListener?.remove()
        documentsListener?.remove()
    }
}
