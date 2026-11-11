# HBX SOLAR ONE — REPOSITORY ARCHITECTURE

## 1. Architectural Pattern
HBX Solar One follows **Clean Architecture** and the **Repository Pattern** to decouple the presentation layer (Jetpack Compose UI) from local persistence (Room Database) and cloud data sources (Cloud Firestore & Firebase Auth).

```
+-----------------------------------------------------------------------+
|                         PRESENTATION LAYER                            |
|  - Jetpack Compose Screens (BdeDashboard, CrmLeads, SiteSurvey, etc.) |
|  - ViewModels & StateFlow Consumers (collectAsState())                |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                          REPOSITORY LAYER                             |
|  - LeadRepository                                                     |
|  - PaymentRepository                                                  |
|  - ProjectRepository                                                  |
|  - AuthRepository (FirebaseAuthRepository)                            |
|  - UserRepository                                                     |
|  - HBXRepository (Facade / Central Coordinator)                       |
+-----------------------------------------------------------------------+
                |                                      |
                v                                      v
+---------------------------------+   +---------------------------------+
|         LOCAL DATA SOURCE       |   |        REMOTE DATA SOURCE       |
|  - Room AppDatabase             |   |  - Cloud Firestore              |
|  - SQLite WAL Storage           |   |  - Firebase Authentication      |
|  - SyncQueueDao                 |   |  - ConnectivityManager Monitor  |
+---------------------------------+   +---------------------------------+
```

---

## 2. Core Repositories

### 2.1 `LeadRepository`
- **Reads:** Reads directly from Room's `LeadDao.getAllLeadsFlow()`. Converts database entities (`LeadEntity`) to domain models (`LeadItem`).
- **Writes:**
  1. Writes to Room SQLite immediately with `SyncStatus.PENDING_UPLOAD`.
  2. Enqueues a persistent `SyncOperationEntity` into `SyncQueueDao`.
  3. Triggers `SyncEngine.triggerSync()` to dispatch to Cloud Firestore if online.

### 2.2 `PaymentRepository`
- **Reads:** Observes `PaymentDao.getAllPaymentsFlow()`.
- **Writes:**
  1. Persists receipt locally with `SyncStatus.PENDING_UPLOAD` and `status = "Pending Verification"`.
  2. Enqueues payment payload into `sync_queue`.
  3. Dispatches via `SyncEngine` upon cellular reconnect.

### 2.3 `ProjectRepository`
- Manages the 23-stage solar installation engineering lifecycle:
  - Material delivery verification (Stage 18)
  - Civil foundations & module mounting (Stage 19)
  - Inverter commissioning & Net Meter synchronization (Stage 20-22)
  - PM Surya Ghar DBT subsidy clearance (Stage 23)
- Manages Bill of Materials (BOM) items and manifest dispatch numbers.

### 2.4 `AuthRepository` (`FirebaseAuthRepository`)
- Integrates Google Sign-In via Android Jetpack `CredentialManager` with `GetSignInWithGoogleOption`.
- Obtains web client ID dynamically via `R.string.default_web_client_id` (no hardcoded credentials).
- Manages Firebase Auth user state and signs in using Google credentials.
- Exposes `currentUser: StateFlow<FirebaseUser?>` and `userProfile: StateFlow<UserProfile?>`.

### 2.5 `UserRepository`
- Performs real-time snapshot synchronization with `/users/{uid}` in Cloud Firestore.
- Handles self-registration profile bootstrapping and RBAC assignment.

### 2.6 `HBXRepository` (Central Coordinator & State Facade)
- Aggregates `LeadRepository`, `PaymentRepository`, `ProjectRepository`, and `SyncEngine`.
- Coordinates role switching across the 10 RBAC views.
- Seeds initial baseline demonstration data into Room on first application launch if the SQLite tables are empty.

---

## 3. Data Flow Guarantees
- **Unidirectional Data Flow (UDF):** Room SQLite acts as the Single Source of Truth for the UI. Cloud Firestore updates Room via real-time snapshot listeners; Room then emits fresh Flow values to the UI.
- **Offline Resilience:** The application is completely functional without internet access. Field engineers can create proposals, perform site audits, and record payments while disconnected in rural villages.
