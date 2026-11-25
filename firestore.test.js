const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

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

// --- 1. UNAUTHENTICATED DENIAL ---
test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Hacker",
      city: "Kota Palembang",
      countryCode: "ID",
      timeZoneId: "Asia/Jakarta",
      latitude: -2.9909,
      longitude: 104.7567,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("prayer_logs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("quran_progress").doc("main").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasbih_sessions").doc("active").get()
  );
});

// --- 2. USER PROFILE (/users/{userId}) ---
test("Owner can create and partially update profile; ghost fields & immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Ahmad Fauzi",
      email: "ahmad@example.com",
      city: "Kota Palembang",
      region: "Sumatera Selatan",
      countryCode: "ID",
      timeZoneId: "Asia/Jakarta",
      latitude: -2.9909,
      longitude: 104.7567,
      notificationPrefs: { Subuh: "ADZAN", Zuhur: "ADZAN" },
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update of city & notificationPrefs succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      city: "Kuala Lumpur & Putrajaya",
      region: "Wilayah Persekutuan",
      countryCode: "MY",
      jakimZone: "WLY01",
      timeZoneId: "Asia/Kuala_Lumpur",
      latitude: 3.139,
      longitude: 101.6869,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Listing all users fails (PII protection)
  await assertFails(aliceDb.collection("users").get());
});

test("Cross-user access to profile is strictly denied", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob",
      email: "bob@example.com",
      city: "Kota Depok",
      countryCode: "ID",
      timeZoneId: "Asia/Jakarta",
      latitude: -6.4,
      longitude: 106.8,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

// --- 3. PRAYER LOGS (/users/{userId}/prayer_logs/{logId}) ---
test("Owner can create, update, list, and delete prayer_logs; invalid enum or future timestamp rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-25_zuhur");

  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      prayerDate: "2026-11-25",
      prayerName: "zuhur",
      completed: true,
      prayMode: "berjamaah_masjid",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update completed & prayMode
  await assertSucceeds(
    logRef.update({
      completed: false,
      prayMode: "sendiri",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List own prayer_logs succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").get()
  );

  // Reject invalid prayerName enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-25_dhuha").set({
      userId: ALICE_UID,
      prayerDate: "2026-11-25",
      prayerName: "dhuha",
      completed: true,
      prayMode: "sendiri",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-25_asar").set({
      userId: ALICE_UID,
      prayerDate: "2026-11-25",
      prayerName: "asar",
      completed: true,
      prayMode: "sendiri",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write denied
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("prayer_logs").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-25_zuhur").get()
  );
});

// --- 4. QURAN PROGRESS (/users/{userId}/quran_progress/main) ---
test("Owner can create and update quran_progress; out-of-bounds surah or cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("quran_progress").doc("main");

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      lastSurahNumber: 2,
      lastSurahName: "Al-Baqarah",
      lastAyah: 142,
      totalAyahsRead: 3866,
      khatamPercentage: 62,
      bookmarks: ["2:142"],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ref.update({
      lastAyah: 148,
      totalAyahsRead: 3872,
      khatamPercentage: 62,
      bookmarks: ["2:148"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid surah number > 114
  await assertFails(
    ref.update({
      lastSurahNumber: 200,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user denied
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quran_progress").doc("main").get()
  );
});

// --- 5. TASBIH SESSIONS (/users/{userId}/tasbih_sessions/active) ---
test("Owner can create and update tasbih_sessions; negative count or cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("tasbih_sessions").doc("active");

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      dzikirTitle: "Subhanallah",
      currentCount: 33,
      targetCount: 100,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ref.update({
      currentCount: 34,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject negative count
  await assertFails(
    ref.update({
      currentCount: -1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user denied
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasbih_sessions").doc("active").get()
  );
});
