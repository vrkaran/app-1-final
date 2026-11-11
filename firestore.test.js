const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const BDE_UID = "bde_rahul";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
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
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated Checks
test("Unauthenticated user: cannot read users collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot read leads collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leads").get());
});

// 2. User Profiles & Role Integrity
test("Authenticated user: can create own profile as BDE", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Alice Sharma",
      email: "alice@hbxsolar.com",
      phone: "+91 9876543210",
      role: "BDE",
      employeeId: "HBX-AZM-001",
      territoryId: "Azamgarh",
      accountStatus: "ACTIVE",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Self-registration: cannot grant SUPER_ADMIN role to self", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      fullName: "Bob Rogue",
      email: "bob@fake.com",
      phone: "+91 9876500000",
      role: "SUPER_ADMIN",
      employeeId: "FAKE-01",
      territoryId: "Azamgarh",
      accountStatus: "ACTIVE",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Privilege Escalation: user cannot update their own role from BDE to SUPER_ADMIN", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Alice Sharma",
      email: "alice@hbxsolar.com",
      phone: "+91 9876543210",
      role: "BDE",
      employeeId: "HBX-AZM-001",
      territoryId: "Azamgarh",
      accountStatus: "ACTIVE",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "SUPER_ADMIN",
    })
  );
});

// 3. Leads & Multi-Tenant Isolation
test("Leads: authenticated user can create lead with their own userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("leads").doc("lead_101").set({
      id: "lead_101",
      name: "Amit Singh",
      phone: "+91 98391 44210",
      village: "Civil Lines, Azamgarh",
      stage: "NEW_LEADS",
      priority: "HIGH",
      capacity: "3.3 kW",
      systemType: "On-Grid",
      userId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Leads: user cannot forge lead with another user's userId", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("leads").doc("lead_forged").set({
      id: "lead_forged",
      name: "Forged Lead",
      phone: "+91 99999 00000",
      village: "Mau",
      stage: "NEW_LEADS",
      priority: "LOW",
      capacity: "5 kW",
      systemType: "Hybrid",
      userId: ALICE_UID, // Bob attempting to impersonate Alice
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

// 4. Document Metadata Security & Anti-Hijacking
test("Documents: unauthenticated user cannot read documents collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("documents").get());
});

test("Documents: authenticated user can create document metadata for their own upload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("documents").doc("doc_001").set({
      documentId: "doc_001",
      businessId: "PRJ-102",
      parentEntityType: "PROJECT",
      parentEntityId: "PRJ-102",
      storagePath: "projects/PRJ-102/site-photos/photo.jpg",
      uploadedByUid: ALICE_UID,
      uploadedAt: new Date(),
    })
  );
});

test("Documents: user cannot forge document metadata with another user's uploadedByUid", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("documents").doc("doc_forged").set({
      documentId: "doc_forged",
      businessId: "PRJ-102",
      parentEntityType: "PROJECT",
      parentEntityId: "PRJ-102",
      storagePath: "projects/PRJ-102/site-photos/photo.jpg",
      uploadedByUid: ALICE_UID, // Bob attempting to impersonate Alice
      uploadedAt: new Date(),
    })
  );
});

test("Documents: Anti-Hijacking - Bob cannot steal ownership of Alice's document", async () => {
  // Seed Alice's document
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("documents").doc("doc_alice").set({
      documentId: "doc_alice",
      businessId: "PRJ-102",
      parentEntityType: "PROJECT",
      parentEntityId: "PRJ-102",
      storagePath: "projects/PRJ-102/site-photos/photo.jpg",
      uploadedByUid: ALICE_UID,
      createdAt: new Date(),
      uploadedAt: new Date(),
    });
  });

  // Bob attempts to change uploadedByUid to himself
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("documents").doc("doc_alice").update({
      uploadedByUid: BOB_UID,
    })
  );
});

test("Documents: Cross-Customer Isolation - Customer Bob cannot read Alice's document metadata", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("documents").doc("doc_alice_kyc").set({
      documentId: "doc_alice_kyc",
      businessId: ALICE_UID,
      parentEntityType: "CUSTOMER",
      parentEntityId: ALICE_UID,
      storagePath: `customers/${ALICE_UID}/kyc/aadhaar.jpg`,
      uploadedByUid: ALICE_UID,
      uploadedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { role: "CUSTOMER" }).firestore();
  await assertFails(bobDb.collection("documents").doc("doc_alice_kyc").get());

  // But Alice can read her own document
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { role: "CUSTOMER" }).firestore();
  await assertSucceeds(aliceDb.collection("documents").doc("doc_alice_kyc").get());
});

test("Documents: hard deletion is blocked to enforce enterprise audit retention", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("documents").doc("doc_permanent").set({
      documentId: "doc_permanent",
      businessId: "PRJ-102",
      uploadedByUid: ALICE_UID,
      uploadedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("documents").doc("doc_permanent").delete());
});
