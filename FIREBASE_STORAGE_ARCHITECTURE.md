# HBX SOLAR ONE — Firebase Cloud Storage Architecture

## 1. Storage Overview & Infrastructure
HBX SOLAR ONE utilizes Google Firebase Cloud Storage to provide enterprise-grade, high-availability object persistence for multi-tier solar project lifecycles across rural, commercial, and residential installations in Uttar Pradesh, India.

- **Primary Storage Bucket**: `gen-lang-client-0270919196.firebasestorage.app`
- **Region**: Cloud Multi-Region (asia-south1 / global edge CDN)
- **Client SDK**: `com.google.firebase:firebase-storage:21.0.1`
- **Image Pipeline**: Coil Compose (`io.coil-kt:coil-compose:2.7.0`) with disk and memory LRU caching.
- **Client Cache Strategy**: Two-tier caching combining an encrypted on-device SQLite document index (`AppDatabase` v2) with localized private file sandboxes (`context.filesDir/documents/`).

---

## 2. Standardized Enterprise Storage Hierarchy
To ensure zero collisions, strict multi-tenant boundary isolation, predictable backup cycles, and fine-grained Role-Based Access Control (RBAC), storage paths are deterministically structured via `StoragePathBuilder`:

```
gen-lang-client-0270919196.firebasestorage.app/
├── customers/{customerId}/
│   ├── kyc/                      # Aadhaar card, PAN card, Voter ID
│   ├── electricity-bills/        # Discom monthly bill PDFs and meter photos
│   └── agreements/               # Signed EPC contract & subsidy consent forms
│
├── leads/{leadId}/
│   └── documents/                # Initial inquiry attachments and roof sketches
│
├── site-surveys/{surveyId}/
│   ├── photos/                   # 8-slot site evidence daylight photos
│   └── videos/                   # Drone azimuth or 360-degree obstacle walk-throughs
│
├── projects/{projectId}/
│   ├── site-photos/              # Pre-installation, foundation, and structural staging
│   ├── installation-evidence/    # Inverter mounts, DC cable routing, earthing pits
│   ├── inspection-reports/       # Discom pre-commissioning & load sanction filings
│   ├── completion-certificates/  # CEIG / Discom commissioning & net-metering sign-offs
│   └── warranty-documents/       # Tier-1 module, inverter, and structure OEM certificates
│
├── quotations/{quotationId}/
│   └── pdf/                      # System configurator generated proposal PDFs
│
├── payments/{paymentId}/
│   ├── receipts/                 # Official stamped money receipt PDFs
│   └── proof/                    # Field-collected manual payment proofs (UPI/NEFT/Cheques)
│
├── service-requests/{serviceId}/
│   └── attachments/              # Breakdown photos, inverter fault codes, maintenance logs
│
└── users/{userId}/
    └── profile/                  # Field technician & engineer profile photographs
```

---

## 3. Storage File Naming Convention
Every uploaded file is transformed into an immutable, collision-resistant identifier:
`{documentType}_{businessId}_{epochSeconds}_{shortUuid}.{extension}`

Example generated identifiers:
- `site_photo_PRJ-102_1762851600_a1b2c3.jpg`
- `electricity_bill_CUST-9921_1762851600_d4e5f6.pdf`
- `payment_proof_RCP-8801_1762851600_789abc.png`

This ensures:
1. Files never overwrite existing revisions.
2. Metadata (type, entity, and upload chronology) can be identified in raw bucket audits.
3. Path sanitization strips dangerous URI characters and prevents path traversal vulnerabilities.

---

## 4. End-to-End Upload Lifecycle & Dual-Phase Consistency
```
[User Selects File] ──► [FileProcessingUtil] ──► [Save Local Sandbox] ──► [Room: DocumentEntity]
                              │                          │                       │
                       Sample & Resize               Compute MD5         uploadStatus = QUEUED
                       (Max 1600px, 82% JPG)        Generate Path        syncStatus = PENDING
                              │
                      [Connectivity Check]
                     /                    \
             (Offline)                    (Online)
                 │                            │
       Enqueue in WorkManager         [Firebase Storage Task]
       & SyncQueueDao (FIFO)                  │
                                      Progress Flow (0% - 100%)
                                              │
                                      Get Download URL
                                              │
                                      Write Firestore Doc Metadata
                                      (/documents/{docId})
                                              │
                                      Update Room: uploadStatus = UPLOADED
                                      syncStatus = SYNCED
```

---

## 5. Security & Constraint Matrix
| Category | Allowed Extensions | MIME Types | Max Size | Primary Roles Allowed |
|---|---|---|---|---|
| Site Photos | `.jpg`, `.jpeg`, `.png`, `.webp` | `image/jpeg`, `image/png`, `image/webp` | 15 MB | BDE, INSTALLATION_ENGINEER, SERVICE_ENGINEER, ADMIN |
| Survey Videos | `.mp4`, `.mov` | `video/mp4`, `video/quicktime` | 50 MB | INSTALLATION_ENGINEER, ADMIN |
| Legal / Bills | `.pdf`, `.jpg`, `.png` | `application/pdf`, `image/*` | 15 MB | BDE, OPERATIONS_MANAGER, CUSTOMER, ADMIN |
| Payment Proofs | `.jpg`, `.png`, `.pdf` | `image/jpeg`, `image/png`, `application/pdf` | 15 MB | BDE, OPERATIONS_MANAGER, CUSTOMER, ADMIN |
| OEM Warranties | `.pdf` | `application/pdf` | 15 MB | OPERATIONS_MANAGER, SUPER_ADMIN |
