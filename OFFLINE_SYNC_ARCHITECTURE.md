# HBX SOLAR ONE — OFFLINE-FIRST SYNCHRONIZATION ENGINE

## 1. Architectural Philosophy
In rural Purvanchal (Azamgarh, Mau, Sagri, Bilariyaganj), mobile network connectivity can drop unexpectedly. Field officers must never be blocked by latency or dropped cellular towers when recording solar site surveys, customer intake proposals, or offline tokens.

```
+-------------------------------------------------------------+
|                      COMPOSE UI LAYER                       |
|  (BdeDashboardScreen, CrmLeadsScreen, SiteSurveyScreen)      |
+-------------------------------------------------------------+
                              | (StateFlow)
                              v
+-------------------------------------------------------------+
|                    REPOSITORY LAYER                         |
|     (LeadRepository, PaymentRepository, ProjectRepository)  |
+-------------------------------------------------------------+
           |                                       |
    (Immediate Read/Write)               (Persistent Sync Queue)
           v                                       v
+------------------------+             +----------------------+
|     ROOM DATABASE      |             |   SYNC ENGINE WORKER |
| (AppDatabase / SQLite) |             |  (ConnectivityManager|
+------------------------+             +----------------------+
                                                   | (Network Available)
                                                   v
                                       +----------------------+
                                       |   CLOUD FIRESTORE    |
                                       |  (Enterprise Backend)|
                                       +----------------------+
```

## 2. Synchronization Guarantees

1. **Immediate Local Responsiveness (Zero Lag):**
   When a BDE clicks "+ Add New Customer Proposal", the record is written to Room immediately with `SyncStatus.PENDING_UPLOAD`. The UI updates instantly at 60 FPS without waiting for network ACK.

2. **Persistent Queue (Process-Death Resistant):**
   Operations are queued in the Room `sync_queue` table. If the phone battery dies or the Android OS kills the app in the background, pending records remain safely stored in SQLite.

3. **Automatic Network Resumption:**
   The `SyncEngine` registers a callback with Android's `ConnectivityManager`. When network connectivity returns, pending operations are processed sequentially in FIFO order.

4. **Idempotency:**
   Every sync payload carries deterministic entity IDs (e.g. `LEAD-xxx`, `RCP-xxx`) and writes via Firestore `set(merge = true)`, preventing duplicate document creation on retries.

5. **Conflict Resolution:**
   - **Timestamp Comparison:** If remote `updatedAt` is newer than local base timestamp, remote wins for factual fields.
   - **Financial Invariance:** Offline payment submissions remain `Pending Verification` until audited by an authenticated administrator. Client offline state cannot force a payment into `Verified` status.
   - **Role Escalation Block:** Client offline updates cannot modify user roles or security claims.
