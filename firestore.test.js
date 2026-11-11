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

// 5. Audit Log Immutability & Partner Isolation
test("Audit Logs: unauthenticated or regular users cannot delete or modify audit entries", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("document_audit_logs").doc("audit_log_001").set({
      auditId: "audit_log_001",
      documentId: "doc_001",
      action: "UPLOAD",
      actorUid: ALICE_UID,
      actorRole: "BDE",
      timestamp: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Modify audit log must be rejected
  await assertFails(aliceDb.collection("document_audit_logs").doc("audit_log_001").update({
    action: "ALTERED",
  }));
  // Delete audit log must be rejected
  await assertFails(aliceDb.collection("document_audit_logs").doc("audit_log_001").delete());
});

test("Partners: cannot access unauthorized customer documents metadata", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("documents").doc("doc_alice_private").set({
      documentId: "doc_alice_private",
      businessId: ALICE_UID,
      parentEntityType: "CUSTOMER",
      parentEntityId: ALICE_UID,
      storagePath: `customers/${ALICE_UID}/kyc/aadhaar.jpg`,
      uploadedByUid: ALICE_UID,
      uploadedAt: new Date(),
    });
  });

  const partnerDb = testEnv.authenticatedContext("partner_raj", { role: "PARTNER" }).firestore();
  await assertFails(partnerDb.collection("documents").doc("doc_alice_private").get());
});

test("Users: ordinary users cannot elevate their role to ADMIN or BDE", async () => {
  const ordinaryDb = testEnv.authenticatedContext("cust_user1", { role: "CUSTOMER" }).firestore();
  await assertFails(
    ordinaryDb.collection("users").doc("cust_user1").update({
      role: "ADMIN",
    })
  );
});

// ==========================================
// SEC-07: Corporate Email Domain Privilege Escalation Tests
// ==========================================
test("SEC-07: User with corporate email domain @hbxsolar.com WITHOUT role custom claim CANNOT access staff leads", async () => {
  // Pre-seed a lead belonging to Alice
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("leads").doc("lead_sec07").set({
      id: "lead_sec07",
      name: "Sunil Verma",
      phone: "+919876543210",
      village: "Gorakhpur",
      stage: "NEW",
      priority: "HIGH",
      capacity: "5kW",
      systemType: "ON_GRID",
      userId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  // User has @hbxsolar.com email address in Auth, but role claim is CUSTOMER or missing
  const corporateEmailNoClaimDb = testEnv.authenticatedContext("user_corp_fake", {
    email: "fake_staff@hbxsolar.com",
    role: "CUSTOMER",
  }).firestore();

  // Attempting to read another user's lead (staff privilege required) must FAIL
  await assertFails(corporateEmailNoClaimDb.collection("leads").doc("lead_sec07").get());
});

test("SEC-07: User with corporate email @purvanchalsolar.in WITHOUT custom claim CANNOT access document metadata", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("documents").doc("doc_sec07").set({
      documentId: "doc_sec07",
      businessId: ALICE_UID,
      parentEntityType: "CUSTOMER",
      parentEntityId: ALICE_UID,
      storagePath: `customers/${ALICE_UID}/kyc/pan.jpg`,
      uploadedByUid: ALICE_UID,
      uploadedAt: new Date(),
    });
  });

  const corpAttackerDb = testEnv.authenticatedContext("purvanchal_user", {
    email: "contractor@purvanchalsolar.in",
  }).firestore();

  await assertFails(corpAttackerDb.collection("documents").doc("doc_sec07").get());
});

// ==========================================
// SEC-08: Suspended and Inactive Account Access Control Tests
// ==========================================
test("SEC-08: Suspended user (accountStatus: SUSPENDED in token) CANNOT create leads", async () => {
  const suspendedDb = testEnv.authenticatedContext("suspended_bde_1", {
    role: "BDE",
    accountStatus: "SUSPENDED",
  }).firestore();

  await assertFails(
    suspendedDb.collection("leads").doc("lead_suspended_attempt").set({
      id: "lead_suspended_attempt",
      name: "Ramesh Kumar",
      phone: "+919876543210",
      village: "Varanasi",
      stage: "NEW",
      priority: "HIGH",
      capacity: "3kW",
      systemType: "HYBRID",
      userId: "suspended_bde_1",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("SEC-08: Suspended user CANNOT read payments collection", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("payments").doc("pay_sec08").set({
      receiptId: "REC-888",
      customerName: "Vikas Singh",
      consumerId: "CONS-888",
      amount: 45000,
      method: "NEFT",
      referenceNo: "UTR987654321",
      status: "Verified",
      userId: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const suspendedDb = testEnv.authenticatedContext("suspended_bde_2", {
    role: "BDE",
    accountStatus: "SUSPENDED",
  }).firestore();

  await assertFails(suspendedDb.collection("payments").doc("pay_sec08").get());
});

test("SEC-08: User CANNOT update their own accountStatus from ACTIVE to SUSPENDED or vice versa", async () => {
  const createdDate = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("user_test_sec08").set({
      uid: "user_test_sec08",
      fullName: "Test User",
      email: "test@hbxsolar.com",
      phone: "+919999999999",
      role: "BDE",
      employeeId: "EMP-08",
      territoryId: "UP-EAST",
      accountStatus: "ACTIVE",
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  const userDb = testEnv.authenticatedContext("user_test_sec08", { role: "BDE" }).firestore();

  // Attempting to change own accountStatus must FAIL
  await assertFails(
    userDb.collection("users").doc("user_test_sec08").update({
      accountStatus: "SUSPENDED",
      updatedAt: new Date(),
    })
  );
});

test("SEC-08: Admin CAN update another user accountStatus to SUSPENDED", async () => {
  const createdDate = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("user_to_suspend").set({
      uid: "user_to_suspend",
      fullName: "Rogue Agent",
      email: "rogue@hbxsolar.com",
      phone: "+919888888888",
      role: "BDE",
      employeeId: "EMP-99",
      territoryId: "UP-EAST",
      accountStatus: "ACTIVE",
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_user_1", { role: "ADMIN" }).firestore();

  // Admin suspends user
  await assertSucceeds(
    adminDb.collection("users").doc("user_to_suspend").set({
      uid: "user_to_suspend",
      fullName: "Rogue Agent",
      email: "rogue@hbxsolar.com",
      phone: "+919888888888",
      role: "BDE",
      employeeId: "EMP-99",
      territoryId: "UP-EAST",
      accountStatus: "SUSPENDED",
      createdAt: createdDate,
      updatedAt: new Date(),
    })
  );
});

// ==========================================
// PHASE 3.2.2 ZERO-TRUST SECURITY VERIFICATION
// ==========================================
test("ZERO-TRUST: User with accountStatus INACTIVE in token is DENIED from creating leads", async () => {
  const inactiveDb = testEnv.authenticatedContext("inactive_user_1", {
    role: "BDE",
    accountStatus: "INACTIVE",
  }).firestore();

  await assertFails(
    inactiveDb.collection("leads").doc("lead_inactive_attempt").set({
      id: "lead_inactive_attempt",
      name: "Inactive User Lead",
      phone: "+919876543210",
      village: "Varanasi",
      stage: "NEW",
      priority: "HIGH",
      capacity: "3kW",
      systemType: "HYBRID",
      userId: "inactive_user_1",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("ZERO-TRUST: User whose Firestore profile lacks accountStatus field is DENIED from leads", async () => {
  const createdDate = new Date();
  // Pre-seed user document without accountStatus property
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("user_missing_status").set({
      uid: "user_missing_status",
      fullName: "No Status User",
      email: "nostatus@example.com",
      phone: "+919876543210",
      role: "CUSTOMER",
      employeeId: "CUST-NOSTATUS",
      territoryId: "Azamgarh",
      // Notice: accountStatus field is completely omitted
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  const missingStatusDb = testEnv.authenticatedContext("user_missing_status").firestore();

  // Accessing leads must be rejected because accountStatus is missing from their profile
  await assertFails(
    missingStatusDb.collection("leads").doc("lead_no_status").set({
      id: "lead_no_status",
      name: "Should Fail",
      phone: "+919876543210",
      village: "Azamgarh",
      stage: "NEW",
      priority: "LOW",
      capacity: "2kW",
      systemType: "ON_GRID",
      userId: "user_missing_status",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("ZERO-TRUST: Immediate Revocation - Previously authenticated user with active token is DENIED after Admin suspends profile in Firestore", async () => {
  const createdDate = new Date();
  const targetUid = "user_revoked_target";

  // 1. Initial State: User is ACTIVE in Firestore
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(targetUid).set({
      uid: targetUid,
      fullName: "Target User",
      email: "target@example.com",
      phone: "+919876543210",
      role: "BDE",
      employeeId: "EMP-TARGET",
      territoryId: "UP-EAST",
      accountStatus: "ACTIVE",
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  // User holds an ID token issued when ACTIVE (token claims contain no SUSPENDED flag)
  const userClientDb = testEnv.authenticatedContext(targetUid, { role: "BDE" }).firestore();

  // 2. Admin immediately changes Firestore document to SUSPENDED
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(targetUid).update({
      accountStatus: "SUSPENDED",
      updatedAt: new Date(),
    });
  });

  // 3. User attempts to create a lead using their previously issued, unexpired token
  // Under zero-trust, Firestore security rules query the live user document and MUST DENY access
  await assertFails(
    userClientDb.collection("leads").doc("lead_after_suspension").set({
      id: "lead_after_suspension",
      name: "Post Suspension Lead",
      phone: "+919876543210",
      village: "Mau",
      stage: "NEW",
      priority: "HIGH",
      capacity: "5kW",
      systemType: "ON_GRID",
      userId: targetUid,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("ZERO-TRUST: Customer cannot self-escalate role to BDE, INSTALLATION_ENGINEER, ADMIN or SUPER_ADMIN", async () => {
  const custUid = "customer_attempting_privilege";
  const createdDate = new Date();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(custUid).set({
      uid: custUid,
      fullName: "Ordinary Customer",
      email: "cust@gmail.com",
      phone: "+919876543210",
      role: "CUSTOMER",
      employeeId: "CUST-001",
      territoryId: "Azamgarh",
      accountStatus: "ACTIVE",
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  const custDb = testEnv.authenticatedContext(custUid, { role: "CUSTOMER" }).firestore();

  // Attempt to elevate to BDE -> FAIL
  await assertFails(custDb.collection("users").doc(custUid).update({ role: "BDE" }));
  // Attempt to elevate to INSTALLATION_ENGINEER -> FAIL
  await assertFails(custDb.collection("users").doc(custUid).update({ role: "INSTALLATION_ENGINEER" }));
  // Attempt to elevate to ADMIN -> FAIL
  await assertFails(custDb.collection("users").doc(custUid).update({ role: "ADMIN" }));
  // Attempt to elevate to SUPER_ADMIN -> FAIL
  await assertFails(custDb.collection("users").doc(custUid).update({ role: "SUPER_ADMIN" }));
});

test("ZERO-TRUST: BDE cannot self-escalate role to ADMIN or SUPER_ADMIN", async () => {
  const bdeUid = "bde_privilege_escalator";
  const createdDate = new Date();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(bdeUid).set({
      uid: bdeUid,
      fullName: "BDE Worker",
      email: "bde@hbxsolar.com",
      phone: "+919876543211",
      role: "BDE",
      employeeId: "BDE-999",
      territoryId: "UP-EAST",
      accountStatus: "ACTIVE",
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  const bdeDb = testEnv.authenticatedContext(bdeUid, { role: "BDE" }).firestore();

  // BDE attempt to elevate to ADMIN -> FAIL
  await assertFails(bdeDb.collection("users").doc(bdeUid).update({ role: "ADMIN" }));
  // BDE attempt to elevate to SUPER_ADMIN -> FAIL
  await assertFails(bdeDb.collection("users").doc(bdeUid).update({ role: "SUPER_ADMIN" }));
});


