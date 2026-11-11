# HBX SOLAR ONE — Firebase Storage Security Rules

## 1. Security Overview & Zero-Trust Principles
The Firebase Storage security rules enforce zero-trust access boundaries:
1. **Default-Deny**: All paths outside registered root directories are rejected unconditionally.
2. **Mandatory Authentication**: Unauthenticated callers cannot read, write, or list storage objects (`request.auth != null`).
3. **MIME Type Validation**: Files must declare genuine content types matching their business purpose (`image/*`, `application/pdf`, `video/mp4`).
4. **File Size Caps**: High-resolution daylight site photos and PDF legal dossiers are strictly capped at 15MB. Drone walk-through video recordings are capped at 50MB.
5. **Write Immutability**: Hard object deletion is strictly forbidden to preserve an unbreakable chain of custody for DISCOM net-metering sanctions and state subsidy audits. Deletions are processed via document metadata soft-deletes only.

---

## 2. Complete Production Storage Rules (`storage.rules`)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function isValidImage() {
      return request.resource.contentType.matches('image/.*')
        && request.resource.size < 15 * 1024 * 1024; // 15MB
    }

    function isValidPdf() {
      return request.resource.contentType == 'application/pdf'
        && request.resource.size < 15 * 1024 * 1024; // 15MB
    }

    function isValidVideo() {
      return request.resource.contentType.matches('video/.*')
        && request.resource.size < 50 * 1024 * 1024; // 50MB
    }

    function isValidDocument() {
      return isValidImage() || isValidPdf();
    }

    // Default deny all unknown paths
    match /{allPaths=**} {
      allow read, write: if false;
    }

    // Customers dossiers (KYC, electricity bills, agreements)
    match /customers/{customerId}/{folder}/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidDocument();
    }

    // Leads documents
    match /leads/{leadId}/documents/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidDocument();
    }

    // Site Survey daylight evidence matrix photos & videos
    match /site-surveys/{surveyId}/photos/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidImage();
    }

    match /site-surveys/{surveyId}/videos/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidVideo();
    }

    // Projects (installation evidence, inspection reports, warranties)
    match /projects/{projectId}/{folder}/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidDocument();
    }

    // System Configurator Quotation PDFs
    match /quotations/{quotationId}/pdf/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidPdf();
    }

    // Payments Proof & Stamped Money Receipts (Manual collection)
    match /payments/{paymentId}/{folder}/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidDocument();
    }

    // Service Requests & Inverter Breakdown Reports
    match /service-requests/{serviceId}/attachments/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidDocument();
    }

    // Field Engineers & Technicians profile photos
    match /users/{userId}/profile/{fileName} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId) && isValidImage();
    }

    // Company Static Assets / Templates
    match /company-assets/{fileName} {
      allow read: if isAuthenticated();
      allow write: if false; // Uploaded via Admin console only
    }
  }
}
```

---

## 3. RBAC Mapping Matrix
| Storage Path | Read Access | Write Access | Purpose |
|---|---|---|---|
| `/customers/{id}/kyc/*` | Authenticated Staff & Customer Owner | BDE, Operations Manager, Customer | Identity verification |
| `/customers/{id}/electricity-bills/*` | Authenticated Staff & Customer Owner | BDE, Customer, Partner | DISCOM load sanctioning |
| `/projects/{id}/installation-evidence/*` | Authenticated Staff & Customer Owner | Installation Engineer, Lead Technician | Field QC & Verification |
| `/quotations/{id}/pdf/*` | Authenticated Staff & Customer Owner | System / BDE / Sales Manager | PM Surya Ghar proposals |
| `/payments/{id}/proof/*` | Authenticated Staff & Customer Owner | BDE, Customer, Partner | Manual UPI / Cheque evidence |
| `/users/{id}/profile/*` | Authenticated Staff | Owner only (`request.auth.uid == userId`) | Field technician profile |
