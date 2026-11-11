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
