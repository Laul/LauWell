# LauWell

Android app: a personal hub to monitor healthcare data — vitals (HR, BP, glucose…),
activity and sleep, ostomy appliances and changes, medications, and more.

## Layout

- `CLAUDE.md` — this file: project context and conventions. Read first.
- `PLAN.md` — milestones and the current task sequence. Update it as tasks complete.
- `app/` — the `:app` Gradle module (standard Android Studio layout: `app/src/main/...`).
  The Gradle root is the repo root (`settings.gradle.kts`, `build.gradle.kts`, `gradle/`).
- `firebase/` — Firestore security rules (`firestore.rules`) and their emulator tests
  (`npm test` from that folder). Pinned to the `lauwell-app` project in `.firebaserc`.
- `docs/` — documents written by Lauranne.
- `docs/data-structure/` — the data model reference: patient data categories, the data map
  (every data point → category / sub-category / storage shape), and the competitive review
  behind them. `Patient Data Categories` is the current reference; the `.csv` beside each
  `.html` holds the same rows and is the diffable copy.
- `docs/architecture/` — how the app is put together: module and feature-toggle design.
- `docs/research/` — prior-art reviews and competitive teardowns that fed the decisions.
- `docs/design/` — visual design work.
  Docs are grouped by subject, not by author. Anything drafted by Claude carries a provenance
  line under its title saying so and giving its status — keep that line accurate when editing.
- `tmp/` — scratch, gitignored. Never put deliverables here.

## Conventions

- Language: English for code, comments, and docs.
- Keep `PLAN.md` current: mark tasks done, add follow-ups discovered along the way.
- Prefer small, reviewable changes; explain trade-offs before large refactors.
- Every new data point gets a category, a sub-category and a storage shape before it gets a
  screen — see `docs/data-structure/` and PLAN.md → Decisions. Adding a tracker should be a
  registry entry, not a new navigation destination.

## Stack

Decisions and rationale: `PLAN.md` → Decisions, and the Notion pages it links to.

- Kotlin 2.4, Jetpack Compose (BOM), JDK 17. Native Android only.
- minSdk 26, compileSdk / targetSdk 36. Single `:app` module, one package per feature.
- UI: own theme tokens (`core.ui.theme`), no Material3 dependency.
- Backend: Firebase — Auth (Google sign-in via Credential Manager), Cloud Firestore, Cloud Storage.
- Data: Health Connect for sensor data, cached in Room; user-logged data in Firestore only.
  Patient-facing categories and the seven storage shapes: `docs/data-structure/`.
- Charts: Vico 3.x (`com.patrykandpatrick.vico:compose`), only behind the `ChartSpec` mapper.
  Never import Vico outside that file.
- Background work: WorkManager. Settings: DataStore.
- Dependency injection: manual wiring — `AppContainer` built in `LauWellApplication`, no Hilt.
- Privacy: no health data or tokens in logs.
