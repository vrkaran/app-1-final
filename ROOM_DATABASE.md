# HBX SOLAR ONE — ROOM DATABASE ARCHITECTURE

## 1. Overview
HBX Solar One implements a high-performance, offline-first local persistence layer using **Android Jetpack Room Database** (SQLite abstraction) combined with **Kotlin Coroutines** and **Reactive Flows**.

All core business data (Leads, Payments, Site Surveys, Pipeline Stages, BOM Items, and Offline Sync Operations) are stored locally in the encrypted application sandbox database `hbx_solar_database`.

---

## 2. Database Schema & Entities

The Room Database version is `1` with non-destructive fallback migration enabled.

### 2.1 `LeadEntity` (`leads` table)
Stores customer leads, opportunities, and PM Surya Ghar government subsidy prospects.
```kotlin
@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val village: String,
    val stage: String,
    val priority: String,
    val capacity: String,
    val systemType: String,
    val assignedTo: String,
    val assignedToUid: String = "",
    val followUpTime: String,
    val quoteRef: String,
    val valueEstimate: String,
    val notes: List<String> = emptyList(),
    val userId: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val serverUpdatedAt: Long = 0L
)
```
- **Indexed Fields:** `id` (Primary Key), `phone`, `village`, `stage`.
- **Search Support:** Supports substring filtering by customer name, phone number, and village for fast field operations.

### 2.2 `PaymentEntity` (`payments` table)
Stores financial token collections, advance receipts, and bank UTR numbers.
```kotlin
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val receiptId: String,
    val customerName: String,
    val consumerId: String,
    val amount: Double,
    val method: String,
    val referenceNo: String,
    val status: String,
    val verifiedBy: String,
    val userId: String = "",
    val dateString: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localUpdatedAt: Long = System.currentTimeMillis()
)
```
- **Integrity Rule:** Offline payment tokens are marked as `Pending Verification`. Client-side offline state can never escalate an unverified payment to `Verified`.

### 2.3 `SiteSurveyEntity` (`site_surveys` table)
Stores technical engineering parameters, GPS coordinates, rooftop substrate, and shading metrics.
```kotlin
@Entity(tableName = "site_surveys")
data class SiteSurveyEntity(
    @PrimaryKey val surveyId: String,
    val leadId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val roofSubstrate: String,
    val usableAreaSqFt: Int,
    val shadowAssessment: String,
    val cableLengthMeters: Int,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localUpdatedAt: Long = System.currentTimeMillis()
)
```

### 2.4 `SyncOperationEntity` (`sync_queue` table)
Persistent, process-death-resistant FIFO operational queue.
```kotlin
@Entity(tableName = "sync_queue")
data class SyncOperationEntity(
    @PrimaryKey val operationId: String = UUID.randomUUID().toString(),
    val entityType: String,       // LEAD, PAYMENT, SURVEY, PROJECT
    val entityId: String,
    val operationType: String,    // CREATE, UPDATE, DELETE
    val payloadJson: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val userId: String = ""
)
```

### 2.5 `PipelineStageEntity` (`pipeline_stages` table) & `BomItemEntity` (`bom_items` table)
Persists the 23-stage solar installation engineering workflow and Bill of Materials items per project.

---

## 3. Data Access Objects (DAOs)

Every DAO provides reactive `kotlinx.coroutines.flow.Flow` streams for UI observation and suspend functions for transactional writes:

1. **`LeadDao`**:
   - `getAllLeadsFlow(): Flow<List<LeadEntity>>`
   - `getLeadByIdFlow(id: String): Flow<LeadEntity?>`
   - `searchLeads(query: String): Flow<List<LeadEntity>>`
   - `upsertLead(lead: LeadEntity)`
   - `upsertLeads(leads: List<LeadEntity>)`
   - `deleteLeadById(id: String)`

2. **`PaymentDao`**:
   - `getAllPaymentsFlow(): Flow<List<PaymentEntity>>`
   - `upsertPayment(payment: PaymentEntity)`
   - `upsertPayments(payments: List<PaymentEntity>)`

3. **`SiteSurveyDao`**:
   - `getSurveyByLeadFlow(leadId: String): Flow<SiteSurveyEntity?>`
   - `upsertSurvey(survey: SiteSurveyEntity)`

4. **`SyncQueueDao`**:
   - `getPendingOperations(): List<SyncOperationEntity>`
   - `enqueue(operation: SyncOperationEntity)`
   - `updateStatus(id: String, status: SyncStatus, retries: Int, error: String?)`
   - `deleteOperation(id: String)`
   - `clearSynced()`

---

## 4. Type Converters (`HBXTypeConverters`)
Room uses `HBXTypeConverters` with `org.json.JSONArray` for zero-overhead JSON serialization:
- `fromListString(list: List<String>): String`
- `toListString(data: String?): List<String>`
- `fromSyncStatus(status: SyncStatus): String`
- `toSyncStatus(value: String): SyncStatus`

---

## 5. Thread Safety & Concurrency
- All Room transactions run on `Dispatchers.IO`.
- Multiple coroutines can safely read and write to the database concurrently through Room's SQLite write-ahead logging (WAL).
- Flow emissions occur on background dispatchers and are collected via Compose `collectAsState()` on the main UI thread.
