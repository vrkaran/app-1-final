# HBX SOLAR ONE — DATABASE SCHEMA

## 1. Overview
HBX Solar One utilizes a hybrid persistence architecture: **Cloud Firestore** as the authoritative enterprise cloud data store and **Room Database** (SQLite) as the device-local cache and offline synchronization store.

## 2. Core Collections (Firestore) & Tables (Room)

### 2.1 Users (`/users/{userId}`) / `users` Table
- `uid` (String, Primary Key): Matches Firebase Auth UID
- `fullName` (String): Full name of the employee or customer
- `email` (String): Contact email
- `phone` (String): Mobile contact (+91)
- `role` (String): One of 10 RBAC roles (`SUPER_ADMIN`, `ADMIN`, `OPERATIONS_MANAGER`, `SALES_MANAGER`, `TEAM_LEADER`, `BDE`, `INSTALLATION_ENGINEER`, `SERVICE_ENGINEER`, `PARTNER`, `CUSTOMER`)
- `employeeId` (String): HBX corporate ID (e.g. `HBX-AZM-xxxx`)
- `territoryId` (String): Regional operating hub (`Azamgarh`, `Mau`, `Varanasi`, `Gorakhpur`)
- `accountStatus` (String): `ACTIVE`, `PENDING_APPROVAL`, `SUSPENDED`, `INACTIVE`
- `createdAt` (Timestamp): Server timestamp
- `updatedAt` (Timestamp): Server timestamp

### 2.2 Leads & Opportunities (`/leads/{leadId}`) / `leads` Table
- `id` (String, Primary Key): e.g. `LEAD-001`
- `name` (String): Customer / Enterprise title
- `phone` (String): Contact phone
- `village` (String): Physical village / block address
- `stage` (String): `NEW_LEADS`, `SURVEY_PENDING`, `QUOTATION_SHARED`, `APPROVED`
- `priority` (String): `HIGH`, `STANDARD`, `GOVT_SUBSIDY`, `COMMERCIAL`
- `capacity` (String): e.g. `3.3 kW On-Grid Rooftop`
- `systemType` (String): `On-Grid (PM Surya Ghar)`, `Hybrid Battery Backup`
- `assignedTo` (String): Assignee display label
- `assignedToUid` (String): Assignee Firebase Auth UID
- `followUpTime` (String): Next scheduled action timestamp
- `quoteRef` (String): Quotation reference code
- `valueEstimate` (String): Estimated financial contract value
- `notes` (Array of Strings): Activity trail and site notes
- `userId` (String): Creator UID
- `syncStatus` (String): `SYNCED`, `PENDING_UPLOAD`, `FAILED`, `CONFLICT`
- `localUpdatedAt` (Long): System clock for local edits
- `serverUpdatedAt` (Long / Timestamp): Cloud update watermark

### 2.3 Payments Ledger (`/payments/{receiptId}`) / `payments` Table
- `receiptId` (String, Primary Key): Official receipt number
- `customerName` (String): Entity or customer name
- `consumerId` (String): 10-digit PUVVNL consumer ID
- `amount` (Double): INR amount
- `method` (String): `UPI`, `Cash`, `Bank Transfer`, `Cheque`
- `referenceNo` (String): UTR transaction or cash voucher code
- `status` (String): `Verified`, `Pending Verification`, `Rejected`
- `verifiedBy` (String): Auditor name or role
- `userId` (String): Field collector UID
- `dateString` (String): Human readable date
- `syncStatus` (String): `SYNCED`, `PENDING_UPLOAD`, `FAILED`

### 2.4 Site Surveys (`/site_surveys/{surveyId}`) / `site_surveys` Table
- `surveyId` (String, Primary Key)
- `leadId` (String, Foreign Key to Lead)
- `latitude` (Double) & `longitude` (Double): GPS coordinates
- `accuracyMeters` (Float): e.g. `±4.2m`
- `roofSubstrate` (String): `RCC_FLAT`, `TIN_SHED`, `TILED_ROOF`
- `usableAreaSqFt` (Int): Usable unshaded rooftop footprint
- `shadowAssessment` (String): Horizon obstruction notes
- `cableLengthMeters` (Int): Parapet to DB box conduit distance

### 2.5 Persistent Synchronization Queue (`sync_queue` Table - Local Room Only)
- `operationId` (String, Primary Key): UUID
- `entityType` (String): `LEAD`, `PAYMENT`, `SURVEY`, `PROJECT`
- `entityId` (String): Document ID being operated on
- `operationType` (String): `CREATE`, `UPDATE`, `DELETE`
- `payloadJson` (String): Idempotent operation payload
- `syncStatus` (String): `PENDING_UPLOAD`, `SYNCING`, `FAILED`, `CONFLICT`
- `retryCount` (Int): Exponential backoff retry counter
- `createdAt` (Long) & `updatedAt` (Long)
- `errorMessage` (String?): Failure reason if rejected
- `userId` (String): Authenticated caller UID
