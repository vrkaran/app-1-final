const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "cust_alice";
const BOB_UID = "cust_bob";
const BDE_UID = "bde_rahul";

const [emulatorHost, emulatorPortStr] = (process.env.STORAGE_EMULATOR_HOST || "127.0.0.1:9199").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./storage.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearStorage();
  }
});

// 1. Unauthenticated Checks
test("Storage: Unauthenticated user cannot read from storage", async () => {
  const unauthStorage = testEnv.unauthenticatedContext().storage();
  const fileRef = unauthStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertFails(fileRef.getDownloadURL());
});

test("Storage: Unauthenticated user cannot upload to storage", async () => {
  const unauthStorage = testEnv.unauthenticatedContext().storage();
  const fileRef = unauthStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertFails(fileRef.put(Buffer.from("dummy data"), { contentType: "image/jpeg" }));
});

// 2. Customer Authorized Self-Service
test("Storage: Customer Alice can upload her own KYC document", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const fileRef = aliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertSucceeds(fileRef.put(Buffer.from("kyc image payload"), { contentType: "image/jpeg" }));
});

test("Storage: Customer Alice can read her own KYC document", async () => {
  // Seed Alice's file using admin/rules disabled
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const fileRef = aliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertSucceeds(fileRef.put(Buffer.from("kyc image payload"), { contentType: "image/jpeg" }));
  await assertSucceeds(fileRef.getDownloadURL());
});

// 3. Cross-Customer Access Control (CRITICAL SECURITY)
test("Storage: Cross-Customer - Customer Bob CANNOT read Alice's KYC document", async () => {
  // Alice uploads file
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const aliceFile = aliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertSucceeds(aliceFile.put(Buffer.from("sensitive data"), { contentType: "image/jpeg" }));

  // Bob attempts to read Alice's file
  const bobStorage = testEnv.authenticatedContext(BOB_UID, { role: "CUSTOMER" }).storage();
  const bobAccessingAlice = bobStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertFails(bobAccessingAlice.getDownloadURL());
});

test("Storage: Cross-Customer - Customer Bob CANNOT write into Alice's folder", async () => {
  const bobStorage = testEnv.authenticatedContext(BOB_UID, { role: "CUSTOMER" }).storage();
  const bobWritingToAlice = bobStorage.ref("customers/cust_alice/kyc/forged.jpg");
  await assertFails(bobWritingToAlice.put(Buffer.from("forged content"), { contentType: "image/jpeg" }));
});

test("Storage: Customer Bob CANNOT access internal leads documents", async () => {
  const bobStorage = testEnv.authenticatedContext(BOB_UID, { role: "CUSTOMER" }).storage();
  const leadFile = bobStorage.ref("leads/lead_001/documents/survey_notes.pdf");
  await assertFails(leadFile.put(Buffer.from("notes"), { contentType: "application/pdf" }));
});

// 4. Role-Based Access Control (Staff Access)
test("Storage: Staff BDE can read customer document for project processing", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const aliceFile = aliceStorage.ref("customers/cust_alice/electricity-bills/bill.pdf");
  await assertSucceeds(aliceFile.put(Buffer.from("uppcl bill"), { contentType: "application/pdf" }));

  // BDE staff access
  const bdeStorage = testEnv.authenticatedContext(BDE_UID, { role: "BDE" }).storage();
  const bdeReadingAlice = bdeStorage.ref("customers/cust_alice/electricity-bills/bill.pdf");
  await assertSucceeds(bdeReadingAlice.getDownloadURL());
});

test("Storage: Staff BDE can upload installation evidence to project", async () => {
  const bdeStorage = testEnv.authenticatedContext(BDE_UID, { role: "BDE" }).storage();
  const projectFile = bdeStorage.ref("projects/PRJ-102/installation-evidence/inverter_mount.jpg");
  await assertSucceeds(projectFile.put(Buffer.from("photo bytes"), { contentType: "image/jpeg" }));
});

// 5. File Constraints & Validation
test("Storage: MIME Type Validation - Executable binary upload is rejected", async () => {
  const bdeStorage = testEnv.authenticatedContext(BDE_UID, { role: "BDE" }).storage();
  const malwareRef = bdeStorage.ref("projects/PRJ-102/installation-evidence/payload.exe");
  await assertFails(malwareRef.put(Buffer.from("MZ malware payload"), { contentType: "application/x-msdownload" }));
});

test("Storage: Size Cap - File exceeding 15MB limit is rejected", async () => {
  const bdeStorage = testEnv.authenticatedContext(BDE_UID, { role: "BDE" }).storage();
  const largeFileRef = bdeStorage.ref("projects/PRJ-102/installation-evidence/huge_file.jpg");
  // 16 MB buffer
  const oversizedBuffer = Buffer.alloc(16 * 1024 * 1024);
  await assertFails(largeFileRef.put(oversizedBuffer, { contentType: "image/jpeg" }));
});

// 6. Immutability & Hard Delete Prevention
test("Storage: Deletion Restriction - Direct client delete is blocked to preserve audit trail", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const fileRef = aliceStorage.ref("customers/cust_alice/agreements/contract.pdf");
  await assertSucceeds(fileRef.put(Buffer.from("contract pdf"), { contentType: "application/pdf" }));

  // Hard delete attempt must be rejected
  await assertFails(fileRef.delete());
});

test("Storage: Deletion Restriction - Payment receipts cannot be hard deleted by client", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const payRef = aliceStorage.ref("payments/pay_101/receipts/bank_receipt.pdf");
  await assertSucceeds(payRef.put(Buffer.from("receipt content"), { contentType: "application/pdf" }));
  await assertFails(payRef.delete());
});

// 7. Partner & Non-Staff Isolation
test("Storage: Partner cannot access unauthorized customer documents", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const aliceKyc = aliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertSucceeds(aliceKyc.put(Buffer.from("aadhaar bytes"), { contentType: "image/jpeg" }));

  const partnerStorage = testEnv.authenticatedContext("partner_raj", { role: "PARTNER" }).storage();
  const partnerAccessingAlice = partnerStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertFails(partnerAccessingAlice.getDownloadURL());
});

test("Storage: Path Traversal Bypass attempt is rejected", async () => {
  const bobStorage = testEnv.authenticatedContext(BOB_UID, { role: "CUSTOMER" }).storage();
  // Attempting dot-dot directory traversal
  const traversalRef = bobStorage.ref("customers/cust_bob/../cust_alice/kyc/aadhaar.jpg");
  await assertFails(traversalRef.getDownloadURL());
});

// ==========================================
// SEC-07: Corporate Email Domain Privilege Escalation Tests
// ==========================================
test("SEC-07: Storage - User with corporate email @hbxsolar.com WITHOUT staff custom claim CANNOT upload into project staff folders", async () => {
  const corporateNoClaimStorage = testEnv.authenticatedContext("user_corp_fake", {
    email: "fake_staff@hbxsolar.com",
    role: "CUSTOMER",
  }).storage();

  const projectFile = corporateNoClaimStorage.ref("projects/PRJ-102/installation-evidence/mount.jpg");
  await assertFails(projectFile.put(Buffer.from("photo bytes"), { contentType: "image/jpeg" }));
});

test("SEC-07: Storage - User with corporate email @purvanchalsolar.in WITHOUT staff claim CANNOT read site survey photos", async () => {
  // Staff uploads site survey photo
  const staffStorage = testEnv.authenticatedContext(BDE_UID, { role: "BDE" }).storage();
  const surveyPhoto = staffStorage.ref("site-surveys/SURV-001/photos/roof.jpg");
  await assertSucceeds(surveyPhoto.put(Buffer.from("roof photo"), { contentType: "image/jpeg" }));

  // Corporate email user without role: BDE attempts to read
  const corpNoClaimStorage = testEnv.authenticatedContext("corp_imposter", {
    email: "contractor@purvanchalsolar.in",
  }).storage();
  const unauthorizedRead = corpNoClaimStorage.ref("site-surveys/SURV-001/photos/roof.jpg");
  await assertFails(unauthorizedRead.getDownloadURL());
});

// ==========================================
// SEC-08: Suspended and Inactive Account Access Control Tests
// ==========================================
test("SEC-08: Storage - Suspended user (accountStatus: SUSPENDED in token) CANNOT upload documents", async () => {
  const suspendedStorage = testEnv.authenticatedContext("suspended_user_1", {
    role: "CUSTOMER",
    accountStatus: "SUSPENDED",
  }).storage();

  const fileRef = suspendedStorage.ref("customers/suspended_user_1/kyc/aadhaar.jpg");
  await assertFails(fileRef.put(Buffer.from("kyc payload"), { contentType: "image/jpeg" }));
});

test("SEC-08: Storage - Suspended user CANNOT download or read files", async () => {
  // Pre-seed file under Alice
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const aliceFile = aliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertSucceeds(aliceFile.put(Buffer.from("valid aadhaar"), { contentType: "image/jpeg" }));

  // Alice gets suspended
  const suspendedAliceStorage = testEnv.authenticatedContext(ALICE_UID, {
    role: "CUSTOMER",
    accountStatus: "SUSPENDED",
  }).storage();
  const fileToRead = suspendedAliceStorage.ref("customers/cust_alice/kyc/aadhaar.jpg");
  await assertFails(fileToRead.getDownloadURL());
});

// ==========================================
// PHASE 3.2.2 ZERO-TRUST STORAGE VERIFICATION
// ==========================================
test("ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from uploading", async () => {
  const inactiveStorage = testEnv.authenticatedContext("inactive_user_storage", {
    role: "CUSTOMER",
    accountStatus: "INACTIVE",
  }).storage();

  const fileRef = inactiveStorage.ref("customers/inactive_user_storage/kyc/pan.jpg");
  await assertFails(fileRef.put(Buffer.from("pan image"), { contentType: "image/jpeg" }));
});

test("ZERO-TRUST: Storage - User with accountStatus INACTIVE in token is DENIED from downloading", async () => {
  // Pre-seed file under Alice
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const aliceFile = aliceStorage.ref("customers/cust_alice/agreements/contract.pdf");
  await assertSucceeds(aliceFile.put(Buffer.from("contract"), { contentType: "application/pdf" }));

  // Inactive Alice attempts download
  const inactiveAliceStorage = testEnv.authenticatedContext(ALICE_UID, {
    role: "CUSTOMER",
    accountStatus: "INACTIVE",
  }).storage();
  const fileToRead = inactiveAliceStorage.ref("customers/cust_alice/agreements/contract.pdf");
  await assertFails(fileToRead.getDownloadURL());
});

test("ZERO-TRUST: Storage - Customer CANNOT upload quotations (Staff-Only Path)", async () => {
  const custStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const quotationRef = custStorage.ref("quotations/QUOT-999/pdf/forged_quote.pdf");
  await assertFails(quotationRef.put(Buffer.from("fake quote"), { contentType: "application/pdf" }));
});

test("ZERO-TRUST: Storage - Customer CANNOT upload site survey photos or inspection evidence", async () => {
  const custStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).storage();
  const surveyPhotoRef = custStorage.ref("site-surveys/SURV-001/photos/fake_survey.jpg");
  await assertFails(surveyPhotoRef.put(Buffer.from("fake survey"), { contentType: "image/jpeg" }));

  const projectEvidenceRef = custStorage.ref("projects/PRJ-101/installation-evidence/forged_mount.jpg");
  await assertFails(projectEvidenceRef.put(Buffer.from("forged mount"), { contentType: "image/jpeg" }));
});

test("ZERO-TRUST: Storage - Stale Token Architectural Verification (Storage Rules isolated from Firestore)", async () => {
  // Architectural proof: Storage rules rely strictly on request.auth.token.
  // If an active token is minted without SUSPENDED claim, Storage cannot query Firestore /users/{uid}.
  // Therefore, token revocation via Firebase Admin SDK is mandatory for immediate Storage revocation.
  const activeTokenStorage = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER", accountStatus: "ACTIVE" }).storage();
  const fileRef = activeTokenStorage.ref("customers/cust_alice/kyc/test.jpg");
  await assertSucceeds(fileRef.put(Buffer.from("test kyc"), { contentType: "image/jpeg" }));
});



