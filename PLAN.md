# LauWell — Plan

## Decisions

Full rationale lives in Notion (LauWell App • Home → Table of Content). Summary:

- **Mobile:** Kotlin + Jetpack Compose, native Android only. No Material3 dependency (own theme tokens).
- **Code structure:** single `:app` Gradle module, one package per feature (`feature.vitals`, …) plus
  shared `core.*` packages. Features are turned on/off at runtime, not at build time.
  → Notion: *Modular Architecture*; `docs/claude/feature-modules-and-toggles.md`.
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
- **Lessons from TrackAid** (`docs/claude/LauWell • Trackaid review.md`): layered design (source →
  repository → ViewModel → UI), typed metric definitions, charts behind our own layer, `Instant` for time,
  `null` for missing data (never 0), unit-tested stats, no health data in logs.

## Milestones

### M0 — Lock the foundations
- [x] Fill in the Stack section of CLAUDE.md (DI still open, see next task)
- [x] Choose dependency injection → manual wiring via `AppContainer` (see Decisions)
- [ ] Adopt the feature descriptor + registry proposal and settle its open points (feature granularity,
      cross-feature data ownership, where the enabled set is stored, default state on first launch)
- [ ] Decide how the TrackAid skeleton branch (`claude/eloquent-dijkstra-ae7o7r`, multi-module
      `core:model` / `core:domain` / `core:charts` / `core:data`) fits the single-`:app` decision:
      port its code into packages, or revisit the module decision
- [ ] Add a "Superseded" note to the Notion *Tech Stack* page

### M1 — Authentication and app shell
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
- [ ] Firestore Security Rules scoped to `request.auth.uid`, tested with the emulator
- [ ] No tokens or health data in logs (TrackAid logged the Firebase ID token)

### M2 — Feature framework
- [ ] `FeatureDescriptor` + `FeatureRegistry`; enabled set persisted; toggles in Settings
- [ ] Nav host and Home read from the registry instead of hard-coding features
- [ ] Domain model: `Measurement(metric, instant, value, source)`, typed `MetricDefinition`
      (unit, precision, target, aggregation, chart kind)
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
- M5 — Logged-data features: medication (+ shared reminder engine), ostomy (+ photos in Cloud Storage),
  glucose logs
- M6 — Dashboard and UX: cross-feature Home, real palette and type scale, accessibility pass
- Backlog: Life's Essential 8 survey, lab results, user-configurable targets, BigQuery export for ML,
  FHIR export, CI (lint, tests, build)

## Done

- [x] Create the Android project in `app/`
- [x] Fix Gradle build (missing wrapper, gradle.properties, JVM toolchain mismatch) — `./gradlew :app:assembleDebug` succeeds
