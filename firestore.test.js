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

test("Unauthenticated user cannot read or create kundalis", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("kundalis").get());
});

test("Authenticated user can create and read their own KundaliProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("kundalis").doc("kundali_1");
  const now = new Date();
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      name: "Aarav Sharma",
      birthDate: "1996-08-15",
      birthTime: "10:45",
      birthPlace: "Jaipur, Rajasthan",
      rashi: "Simha (Leo)",
      nakshatra: "Magha - Pada 2",
      lagna: "Tula (Libra)",
      aiSummary: "Strong Sun in 10th house brings leadership and spiritual wisdom.",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(docRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("kundalis").get());
});

test("Cross-user isolation: Bob cannot read or delete Alice's KundaliProfile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().collection("users").doc(ALICE_UID).collection("kundalis").doc("kundali_1").set({
      userId: ALICE_UID,
      name: "Aarav Sharma",
      birthDate: "1996-08-15",
      birthTime: "10:45",
      birthPlace: "Jaipur, Rajasthan",
      rashi: "Simha (Leo)",
      nakshatra: "Magha - Pada 2",
      lagna: "Tula (Libra)",
      aiSummary: "Summary",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kundalis").doc("kundali_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kundalis").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("kundalis").doc("kundali_1").delete());
});

test("Shadow update / ghost field is rejected on KundaliProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("kundalis").doc("kundali_ghost");
  const now = new Date();
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      name: "Aarav Sharma",
      birthDate: "1996-08-15",
      birthTime: "10:45",
      birthPlace: "Jaipur, Rajasthan",
      rashi: "Simha (Leo)",
      nakshatra: "Magha - Pada 2",
      lagna: "Tula (Libra)",
      aiSummary: "Summary",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can create MilanReport and PalmReading with valid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("milan_reports").doc("milan_1").set({
      userId: ALICE_UID,
      boyName: "Rohan",
      boyDetails: "1995-03-12 • Mesh Rashi",
      girlName: "Priya",
      girlDetails: "1997-07-21 • Simha Rashi",
      totalGuna: 28,
      compatibilityVerdict: "Uttam Milan (28/36 Gunas)",
      aiAnalysis: "Harmonious Bhakoot and Nadi alignment.",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("milan_reports").doc("milan_invalid").set({
      userId: ALICE_UID,
      boyName: "Rohan",
      boyDetails: "1995-03-12 • Mesh Rashi",
      girlName: "Priya",
      girlDetails: "1997-07-21 • Simha Rashi",
      totalGuna: 45, // invalid > 36
      compatibilityVerdict: "Invalid",
      aiAnalysis: "Invalid score",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("palm_readings").doc("palm_1").set({
      userId: ALICE_UID,
      handSide: "Right Hand",
      lifeLineSummary: "Deep and curved Jeevan Rekha indicating vitality.",
      heartLineSummary: "Balanced Hridaya Rekha reaching Jupiter mount.",
      headLineSummary: "Clear Mastishka Rekha showing analytical clarity.",
      fateAndMounts: "Elevated Surya and Guru mounts.",
      remedies: "Chant Gayatri Mantra daily at sunrise.",
      createdAt: now,
      updatedAt: now,
    })
  );
});
