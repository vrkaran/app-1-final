# HBX SOLAR ONE — In-App Document Viewer Architecture

## 1. Overview & User Experience Goals
Field supervisors, district engineers, and customers require instant in-app inspection of high-resolution evidence, Discom electricity bills, and signed project completion certificates without relying on external third-party apps that could compromise corporate data security.

The `DocumentViewerModal` component delivers an immersive, high-performance modal preview natively integrated into the Google Stitch UI/UX design system.

---

## 2. Component Architecture (`DocumentViewerModal.kt`)

### 2.1 Multi-Format Rendering Engine
- **Image Formats (`image/jpeg`, `image/png`, `image/webp`)**:
  - Rendered via Coil Compose's `AsyncImage` with memory disk caching and smooth crossfade.
  - Multi-touch gesture handling via Compose `Modifier.pointerInput`:
    - Dynamic pinch-to-zoom scaling from `1.0f` to `4.0f`.
    - 2D translation panning (`offset.x`, `offset.y`) bounded to image constraints.
    - Double-tap reset to original framing (`scale = 1.0f`, `offset = Offset.Zero`).
- **PDF Documents (`application/pdf`)**:
  - Displays an interactive corporate A4 dossier viewport with page indicators, DISCOM endorsement badges, and security watermarks.
  - Quick action buttons to launch system print or safe external viewing via Android `Intent.ACTION_VIEW` with `FLAG_GRANT_READ_URI_PERMISSION`.

### 2.2 Enterprise Metadata Inspector & Audit Drawer
A structured details card below the media canvas surfaces critical verification properties:
- **Category Badge**: `Site Evidence Matrix`, `KYC Dossier`, `Manual Payment Proof`, or `System Quotation`.
- **Integrity Checksum**: Cryptographic MD5 hash to confirm the file has not been altered since field capture.
- **File Metrics**: Human-readable file size (KB / MB) and storage path hierarchy.
- **Uploader Profile**: Full name, employee ID, and RBAC role (`BDE`, `Installation Engineer`, `Operations Manager`) of the uploading staff member.
- **Timestamp**: Localized ISO date-time of upload.

### 2.3 Actions & Handlers
1. **Download / Cache**: Downloads the remote object directly to the device's public download directory using Android `DownloadManager` or local cache.
2. **Share Intent**: Emits an Android Sharesheet `Intent` restricted to authorized internal channels.
3. **Soft Delete**: Triggers an alert dialog requiring confirmation, then marks `isDeleted = true` with a timestamp and user ID audit entry. Hard deletion is prevented.
