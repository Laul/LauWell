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
- **Lessons from TrackAid** (`docs/claude/LauWell • Trackaid review.md`): layered design (source →
  repository → ViewModel → UI), typed metric definitions, charts behind our own layer, `Instant` for time,
  `null` for missing data (never 0), unit-tested stats, no health data in logs.

## Milestones

### M0 — Lock the foundations
- [ ] Fill in the Stack section of CLAUDE.md
- [ ] Choose dependency injection: Hilt or manual wiring in `LauWellApplication`
- [ ] Adopt the feature descriptor + registry proposal and settle its open points (feature granularity,
      cross-feature data ownership, where the enabled set is stored, default state on first launch)
- [ ] Decide how the TrackAid skeleton branch (`claude/eloquent-dijkstra-ae7o7r`, multi-module
      `core:model` / `core:domain` / `core:charts` / `core:data`) fits the single-`:app` decision:
      port its code into packages, or revisit the module decision
- [ ] Add a "Superseded" note to the Notion *Tech Stack* page

### M1 — Authentication and app shell
- [ ] Provision the Firebase project and the Google OAuth "Web application" client;
      add `google-services.json` and `GOOGLE_WEB_CLIENT_ID` (not committed)
- [ ] `core.auth`: `AuthRepository` returning `AppResult<Session>` (Credential Manager + Firebase Auth,
      nonce wired)
- [ ] `AuthGate` (no session → sign-in, session → nav host), sign-in screen, sign-out
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
- [ ] Reusable chart component: our own `ChartSpec`, one mapper to the chart library (Vico),
      range selector (week / month / 3 months), gaps drawn for missing days
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
