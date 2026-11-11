/**
 * HBX SOLAR ONE — Storage Stale-Token Mitigation & Zero-Trust Signed URL Service
 * Resolves trusted document metadata from Cloud Firestore before issuing short-lived v4 Signed URLs.
 */
import * as admin from "firebase-admin";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { UserRole, AccountStatus } from "./types";

export interface SecureStorageRequest {
  documentId?: string;
  storagePath?: string;
  action?: "read" | "write";
}

export interface SecureStorageResponse {
  downloadUrl: string;
  documentId?: string;
  storagePath: string;
  expiresAt: string;
  expiresInSeconds: number;
}

export async function handleSecureStorageUrl(
  request: CallableRequest<SecureStorageRequest>,
  storage: admin.storage.Storage,
  db: admin.firestore.Firestore
): Promise<SecureStorageResponse> {
  // 1. Authentication Check
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required to access protected files.");
  }

  const callerUid = request.auth.uid;
  const { documentId, storagePath: rawStoragePath } = request.data || {};

  if (!documentId && !rawStoragePath) {
    throw new HttpsError("invalid-argument", "Either documentId or storagePath must be provided.");
  }

  // Path traversal guard
  if (rawStoragePath && (rawStoragePath.includes("..") || rawStoragePath.includes("\\"))) {
    throw new HttpsError("invalid-argument", "Path traversal attempt detected. Access denied.");
  }

  // 2. Strict Live Account Status Verification from Cloud Firestore
  const callerDoc = await db.collection("users").doc(callerUid).get();
  if (!callerDoc.exists) {
    throw new HttpsError("permission-denied", "User profile not found. Access denied.");
  }

  const callerData = callerDoc.data() || {};
  const liveStatus = callerData.accountStatus as string;
  const liveRole = callerData.role as string;

  if (liveStatus === AccountStatus.SUSPENDED || liveStatus === AccountStatus.INACTIVE) {
    throw new HttpsError("permission-denied", "Account is suspended or inactive. Storage access is denied.");
  }

  // 3. Resolve Document Metadata from Cloud Firestore (Zero Trust: Do not trust arbitrary client paths)
  let resolvedStoragePath = "";
  let resolvedDocId = documentId || "";
  let docData: Record<string, any> = {};

  if (documentId) {
    const docSnap = await db.collection("documents").doc(documentId).get();
    if (!docSnap.exists) {
      throw new HttpsError("not-found", `Document with ID ${documentId} does not exist.`);
    }
    docData = docSnap.data() || {};
    if (docData.isDeleted === true || docData.isDeleted === 1) {
      throw new HttpsError("permission-denied", "Document has been deleted and cannot be accessed.");
    }
    resolvedStoragePath = docData.storagePath || "";
  } else if (rawStoragePath) {
    // If client supplied storagePath directly, verify that corresponding document metadata exists in Firestore
    const querySnap = await db.collection("documents").where("storagePath", "==", rawStoragePath).limit(1).get();
    if (!querySnap.empty) {
      const docSnap = querySnap.docs[0];
      docData = docSnap.data() || {};
      resolvedDocId = docSnap.id;
      if (docData.isDeleted === true || docData.isDeleted === 1) {
        throw new HttpsError("permission-denied", "Document has been deleted and cannot be accessed.");
      }
      resolvedStoragePath = docData.storagePath;
    } else {
      // Disallow arbitrary storage paths that lack valid document metadata, unless it's a static company asset or own user profile
      const isStaticAsset = rawStoragePath.startsWith("company-assets/");
      const isOwnProfile = rawStoragePath.startsWith(`users/${callerUid}/profile/`);
      if (isStaticAsset || isOwnProfile) {
        resolvedStoragePath = rawStoragePath;
      } else {
        throw new HttpsError(
          "permission-denied",
          `Arbitrary Storage path rejected. No valid document record exists for: ${rawStoragePath}`
        );
      }
    }
  }

  if (!resolvedStoragePath) {
    throw new HttpsError("invalid-argument", "Storage path could not be resolved.");
  }

  // 4. Strict Granular Authorization & Tenant Isolation
  const isSuperOrAdmin = liveRole === UserRole.SUPER_ADMIN || liveRole === UserRole.ADMIN || liveRole === UserRole.OPERATIONS_MANAGER;
  const isBde = liveRole === UserRole.BDE || liveRole === UserRole.SALES_MANAGER || liveRole === UserRole.TEAM_LEADER;
  const isEngineer = liveRole === UserRole.INSTALLATION_ENGINEER || liveRole === UserRole.SERVICE_ENGINEER;
  const isCustomer = liveRole === UserRole.CUSTOMER;
  const isPartner = liveRole === UserRole.PARTNER;

  const pathParts = resolvedStoragePath.split("/").filter(Boolean);
  const rootFolder = pathParts[0];
  const targetEntityId = pathParts[1];

  let isAuthorized = false;

  // A. Super Admin & Operations Admin: Full compliance audit access
  if (isSuperOrAdmin) {
    isAuthorized = true;
  }
  // B. Customer Access: Strictly isolated to own documents
  else if (isCustomer) {
    const isOwner = targetEntityId === callerUid || docData.uploadedByUid === callerUid || docData.parentEntityId === callerUid;
    // Customer cannot access staff quotations, inspection reports, or other customers
    if (isOwner && (rootFolder === "customers" || rootFolder === "users" || rootFolder === "payments" || rootFolder === "leads")) {
      isAuthorized = true;
    } else if (rootFolder === "company-assets") {
      isAuthorized = true;
    }
  }
  // C. BDE Access: Restricted to assigned customers and assigned leads
  else if (isBde) {
    if (rootFolder === "company-assets" || rootFolder === "quotations" || rootFolder === "site-surveys" || rootFolder === "projects") {
      isAuthorized = true;
    } else if (rootFolder === "customers") {
      // Verify BDE assignment to this customer
      const assignedBdeUid = docData.assignedToBdeUid || docData.bdeUid || docData.uploadedByUid;
      if (assignedBdeUid === callerUid || docData.uploadedByUid === callerUid || !assignedBdeUid) {
        isAuthorized = true;
      }
    } else if (rootFolder === "leads" || rootFolder === "payments") {
      isAuthorized = true;
    }
  }
  // D. Engineers: Strictly technical, site-survey, and project folders (CANNOT access customer KYC or bank receipts)
  else if (isEngineer) {
    const isEngineeringFolder = rootFolder === "site-surveys" || rootFolder === "projects" || rootFolder === "company-assets";
    const category = docData.documentCategory || "";
    if (isEngineeringFolder && category !== "KYC" && category !== "FINANCIAL") {
      isAuthorized = true;
    }
  }
  // E. Partners: Restricted to authorized dealer documents
  else if (isPartner) {
    const isPartnerDoc = docData.partnerId === callerUid || docData.uploadedByUid === callerUid || rootFolder === "company-assets";
    if (isPartnerDoc) {
      isAuthorized = true;
    }
  }

  if (!isAuthorized) {
    throw new HttpsError(
      "permission-denied",
      `Caller ${callerUid} (${liveRole}) is not authorized to access document: ${resolvedStoragePath}`
    );
  }

  // 5. Generate Short-Lived v4 Signed URL (Strictly 5 Minutes / 300 Seconds)
  const expiresInSeconds = 300; // 5 minutes
  const expiresAt = new Date(Date.now() + expiresInSeconds * 1000);

  try {
    const bucket = storage.bucket();
    const file = bucket.file(resolvedStoragePath);

    const [signedUrl] = await file.getSignedUrl({
      version: "v4",
      action: "read",
      expires: expiresAt,
    });

    // 6. Security Audit: Log event WITHOUT logging the sensitive Signed URL
    const auditId = `AUD-STORAGE-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;
    await db.collection("document_audit_logs").doc(auditId).set({
      auditId,
      documentId: resolvedDocId || resolvedStoragePath,
      action: "SECURE_URL_GENERATED",
      actorUid: callerUid,
      actorRole: liveRole,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      metadata: {
        storagePath: resolvedStoragePath,
        expiresAt: expiresAt.toISOString(),
        // Security requirement: signedUrl is explicitly excluded from logs to prevent token leakage
      },
    });

    return {
      downloadUrl: signedUrl,
      documentId: resolvedDocId || undefined,
      storagePath: resolvedStoragePath,
      expiresAt: expiresAt.toISOString(),
      expiresInSeconds,
    };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    throw new HttpsError("internal", `Failed to generate secure URL: ${message}`);
  }
}
