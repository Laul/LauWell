# LauWell — Plan

## Decisions

Full rationale lives in Notion (LauWell App • Home → Table of Content). Summary:

- **Mobile:** Kotlin + Jetpack Compose, native Android only. No Material3 dependency (own theme tokens).
- **Code structure:** single `:app` Gradle module, one package per feature (`feature.vitals`, …) plus
  shared `core.*` packages. Features are turned on/off at runtime, not at build time.
  → Notion: *Modular Architecture*; `docs/architecture/feature-modules-and-toggles.md`.
- **Backend:** Firebase — Firebase Auth (Google sign-in), Cloud Firestore (data), Cloud Storage (photos).
  Custom, FHIR-inspired schema (LOINC codes where trivial). History: Medplum/FHIR rejected (provider-oriented,
  overkill for one user) → Supabase rejected (free tier pauses after 7 days) → Firebase.
  → Notion: *Backend Approach*. (The older *Tech Stack* page still leans Medplum: superseded.)
- **Auth:** native Google sign-in via Credential Manager (in-app sheet, no browser) → Google ID token →
  Firebase `signInWithCredential`. The SDK stores and refreshes the session. → Notion: *Authentication Approach*.
- **Data placement:**
  - Dense sensor data (steps, heart rate, sleep) comes from Health Connect. It is synced into a local
    **Room** database, which is the source of truth for charts (history, offline, speed). Only daily
    summaries go to Firestore.
  - User-logged data (manual vitals, medication, ostomy, glucose) goes to **Firestore**, relying on its
    built-in offline cache. No Room for this data unless a need shows up.
- **Charts:** Vico 3.x (`compose` module, no Material), behind our own `ChartSpec` layer; only one
  mapper file imports Vico. Koala Plot is the fallback; MPAndroidChart v4 to revisit once mature.
  → Notion: *Charting library*.
- **Dependency injection:** manual wiring — one `AppContainer` (plain Kotlin class) built in
  `LauWellApplication.onCreate()`, holding the app's singletons (auth, Firestore, Room, repositories);
  classes receive their dependencies through their constructor, ViewModels via small factories.
  Why not Hilt: one user, one module, a handful of singletons — Hilt's plugin, annotation processing,
  build time and concepts don't pay for themselves yet. Constructor injection is used either way, so
  migrating to Hilt later is mechanical (add annotations, delete the container). Revisit if the
  container grows past a few dozen entries or factories become a burden.
- **Lessons from TrackAid** (`docs/research/TrackAid review.md`): layered design (source →
  repository → ViewModel → UI), typed metric definitions, charts behind our own layer, `Instant` for time,
  `null` for missing data (never 0), unit-tested stats, no health data in logs.
- **Patient data categories:** five patient-facing categories, each with a test that decides where a
  new data point goes. **Vitals** — a measured value read against a clinical normal range.
  **Medical Records** — a fact with a start date rather than a timestamp, or the record of an
  encounter (absorbs appointments, visits and providers; renamed from "Medical history", which
  signalled *the past* and hid the active diagnosis list). **Lifestyle** — a daily log measured
  against a goal you set. **Symptoms** — self-reported, no instrument involved. **Treatments &
  appliances** — what you take or use, and the products behind it.
  Vitals vs Lifestyle is settled by one question: *clinical normal range, or personal goal?* That is
  why mood and the menstrual cycle sit in Lifestyle without an exception, and why stoma output volume
  sits in Vitals (sustained high output is a clinical dehydration threshold) while the change routine
  stays in Lifestyle. 31 sub-categories form the second level.
  → `docs/data-structure/Patient Data Categories.html` (+ `.csv`).
- **Storage shapes:** category and sub-category are *facets on the metric definition*, not tables.
  Seven shapes carry everything: `Measurement` (value·unit·instant), `Event` (instant + optional
  duration), `Interval` (start·end·status), `StandingFact` (no instant — blood type, family history),
  `CatalogueItem` (a thing with attributes), `Document` (a file with a date), `Derived` (computed —
  e.g. wear time from consecutive appliance changes). The map of 81 data points to shapes lives in the
  same doc, and its shape×category matrix is generated from the rows so the two cannot drift.
- **Catalogue + occurrence:** wherever a category holds both a *thing* and an *act*, they are two
  tables — medication vs dose, ostomy product vs appliance change, device vs reading, supplier vs
  order. Product attributes (brand, reference code, convexity, flange size, barrier type) belong to the
  catalogue entity and are **never free text on the event**. This is what makes "did the 25 mm convex
  barrier give longer wear time and better skin than the flat one" answerable; without it that
  question is permanently out of reach, and it is the one thing no existing ostomy app does.
- **Symptom terminology:** two vocabularies, not one. A coded reference terminology underneath
  (SNOMED CT / ICD-11 subset) for correctness and any export to a clinician, and a patient-facing
  layer on top — everyday names, synonyms, body-map entry, recents and a personal shortlist of the
  handful of symptoms actually tracked. Browsing the full tree is the rare path, never the default.
- **Lessons from the app teardown** (`docs/research/Review • Existing Health Apps.html`):
  Samsung's six topic silos make cross-category comparison impossible — the failure mode a topic-based
  menu always reaches. Guava separates current status from the archive (Profile vs Records). Bearable
  types data as *factors* vs *outcomes* and refuses to show a correlation until there are three
  entries with the factor and three without, with lag windows out to seven days. Human Health puts
  logging on a central FAB, scopes insights per condition rather than per tab, organises the treatment
  plan by time-of-day with a daily completion percentage, and picks the input control per tracker
  (slider / pips / chips). OstoBuddy already carries cross-brand product references with stock levels;
  what nobody does is join those attributes to outcomes.

## Milestones

### M0 — Lock the foundations
- [x] Fill in the Stack section of CLAUDE.md (DI still open, see next task)
- [x] Choose dependency injection → manual wiring via `AppContainer` (see Decisions)
- [ ] Adopt the feature descriptor + registry proposal and settle its open points (feature granularity,
      cross-feature data ownership, where the enabled set is stored, default state on first launch)

### M1 — Authentication and app shell ✅ (2026-10-03)
- [x] Provision the Firebase project and the Google OAuth "Web application" client;
      add `google-services.json` and `GOOGLE_WEB_CLIENT_ID` (not committed)
- [x] Gradle: google-services plugin, Firebase BOM (auth, firestore), Credential Manager, googleid;
      `GOOGLE_WEB_CLIENT_ID` → `BuildConfig`. Required a toolchain bump: Kotlin 2.1.0 → 2.4.20,
      KSP → 2.3.12 (KSP2), AGP 8.9.1 → 8.13.2, Gradle 8.11.1 → 8.14.4 (Firebase/googleid ship Kotlin 2.3/2.4 metadata)
- [x] `AppContainer` in `LauWellApplication` + a ViewModel factory helper (`appViewModel { … }`) (manual DI, see Decisions)
- [x] `core.auth`: `AuthRepository` returning `AppResult<Session>` (Credential Manager + Firebase Auth,
      nonce wired). `Session` holds only `uid` (no tokens, no name/email until a screen needs them);
      failures typed as `AuthException`. Unit tests: nonce + error mapping.
      Verified on device (2026-10-02): Firebase accepts the raw nonce (`setIdTokenWithRawNonce`)
- [x] `AuthGate` (no session → sign-in, session → nav host), sign-in screen, sign-out
      (Home → Settings → Sign out). Verified on device: sign-in, session survives app kill,
      sign-out returns to sign-in with no back stack, sheet shown again, dismiss shows no error
- [x] Firestore Security Rules scoped to `request.auth.uid`, tested with the emulator
      (`firebase/`: owner-only `users/{uid}/**`, deny everything else; 7 emulator tests via `npm test`;
      deployed to `lauwell-app` 2026-10-03)
- [x] No tokens or health data in logs (TrackAid logged the Firebase ID token). Audit 2026-10-03:
      no logging calls in `app/src`; device logcat over sign-out → sign-in → relaunch has no ID token,
      nonce or email. Also: app data excluded from cloud backup and device transfer
      (`allowBackup="false"` + `data_extraction_rules.xml`), since it holds the Firebase session

### M2 — Feature framework
- [ ] `FeatureDescriptor` + `FeatureRegistry`; enabled set persisted; toggles in Settings
- [ ] Nav host and Home read from the registry instead of hard-coding features
      (also replaces Home's temporary "Settings" link added in M1)
- [ ] Domain model: the seven storage shapes (`Measurement`, `Event`, `Interval`, `StandingFact`,
      `CatalogueItem`, `Document`, `Derived`). `MetricDefinition` carries unit, precision, target,
      aggregation and chart kind **plus `category` and `subCategory` as facets** — so adding a tracker
      is a registry entry, never a new screen or a new navigation destination
- [ ] Catalogue + occurrence split wherever both exist (see Decisions): catalogue tables for
      medication, ostomy product, accessory, device and supplier; event tables for their use
- [ ] Provenance on every record: `source` (device / lab / manual / import), recorded-at, edited-at,
      and a stated rule for when two sources report the same metric
- [ ] Attachments: a `Document` entity that can hang off any record, plus unattached files and one
      "all files" view
- [ ] Free-text note, attachable to any record and able to stand alone with a date (the escape hatch —
      without it people stop using the structured fields)
- [ ] Data conventions: Firestore paths `users/{uid}/{feature}/{docId}`, FHIR-inspired common fields,
      one typed model per feature, repositories returning `Flow<AppResult<…>>`
- [ ] Per-feature permission request (partial grants allowed, no all-or-nothing)

### M3 — Health Connect, starting with steps
Builds the reusable pieces with the simplest data type (read-only, one number per day).
- [ ] Health Connect data source behind a `HealthDataSource` interface (paginated with `pageToken`)
- [ ] Room database as the local store for Health Connect data; incremental sync
- [ ] Daily aggregation + stats, unit-tested (TrackAid's min/avg and off-by-one bugs)
- [ ] Chart validation test (~1 day): build the hardest chart first — heart-rate weekly range bar
      (Vico candlestick layer) + average dot + target band (`HorizontalBox`) + tooltip — fed by a
      `StateFlow` that changes while the screen is open. Pass → Vico for every chart; fail → try
      Koala Plot behind the same `ChartSpec`. (Notion: *Charting library*.)
- [ ] Reusable chart component: our own `ChartSpec`, one mapper file to Vico (the only file that
      imports Vico), range selector (week / month / 3 months), gaps drawn for missing days
- [ ] Chart updates: ViewModel `StateFlow` → mapper → Vico model producer; one refresh at a time
- [ ] Steps: Home card + detail screen with the chart
- [ ] Onboarding when Health Connect is missing; per-metric "grant access"
- [ ] Then: heart rate (range bar + average), sleep
- [ ] Daily summaries uploaded to Firestore (WorkManager, only for enabled features)

### M4 — Manual vitals, starting with weight
Adds the entry form and Firestore writes, reusing M3's chart and card.
- [ ] Weight: entry form, Firestore repository, list, chart, Home card
- [ ] Then: blood pressure, heart rate, temperature

### Later (review after M3/M4)
- M5 — Logged-data features: medication (+ shared reminder engine; dose taken **vs skipped**, since
  the skipped case carries as much information and most apps drop it), ostomy (appliance change events
  referencing the product catalogue, wear time derived from consecutive changes, leak events,
  peristomal skin, photos in Cloud Storage), glucose logs. **Trial periods** land here — a labelled
  interval on one configuration, so product comparisons run period-against-period instead of being
  averaged across every change ever made and confounded by season, diet and stoma maturation
- M6 — Dashboard and UX: cross-feature Home driven by the five categories, real palette and type
  scale, accessibility pass. Upcoming appointments surface on Home even though they are filed under
  Medical Records — nobody opens an archive to find out what they are doing on Thursday
- Backlog: emergency view assembled from Medical Records + Treatments (blood type, active conditions,
  current medications, allergies, contacts) and reachable without unlocking the app; symptom
  terminology import (coded subset + synonym layer); factor-to-outcome analysis with a minimum-evidence
  gate; Life's Essential 8 survey, lab results (same `Measurement` entity, `source = lab`),
  user-configurable targets, BigQuery export for ML, FHIR export, CI (lint, tests, build)
- Maintenance (from the M1 lint run): update the ~14 outdated dependencies and AGP, add an app icon
  (`MissingApplicationIcon`); CI could also run the Firestore rules tests (`firebase/`, `npm test`)

## Done

- [x] Create the Android project in `app/`
- [x] Fix Gradle build (missing wrapper, gradle.properties, JVM toolchain mismatch) — `./gradlew :app:assembleDebug` succeeds
