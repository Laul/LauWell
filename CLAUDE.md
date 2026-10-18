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
- `docs/data-structure/` — the data model reference. **Being reconciled** with the simplified model in
  Notion (*Data Model [Simplified]*: seven categories incl. Labs and Ostomy, a `Nav. Screens` column): until it is
  regenerated, PLAN.md → Decisions → *Simplified data model* wins where the two disagree.
  - `health-data-model/` — patient data categories, the data map (every data point → category /
    sub-category / storage shape), and the terminology codings (LOINC / SNOMED CT / UCUM).
    `Patient Data Categories` is the current reference; `Patient Data Model.csv` is the combined
    sheet (categories + codings). Generated from `_src/` by `node _src/build.js`; the `.csv` files
    are the diffable copies. Earlier inventories are kept beside them as snapshots.
  - `health_data_standards/` — source material for standardization, e.g. the MUHC laboratory
    reference ranges list.
  - `health_data_labs/` — `common_lab_tests.csv`, the seed list for the Labs category.
- `docs/architecture/` — how the app is put together: module and feature-toggle design.
- `docs/research/` — prior-art reviews and competitive teardowns that fed the decisions.
- `docs/design/` — visual design, made in Claude Desktop. **Gitignored, local only**: it may be absent;
  PLAN.md → Decisions → *Visual design* is the tracked record. **`Design • Global direction - v15.html`
  is the reference**: a zip, despite its name, holding the handoff of the locked direction 13b plus
  the Tracker views 15a/15b (bundled design HTML, `UI-spec.md`, icon list). `UI-spec.md` and
  `Icons/lauwell-icons.csv` are also copied loose beside it. Older exports (`v13b-02`, the loose
  `LauWell Final Design (for Figma import).html`) are superseded. `LauWell Design Directions.html`
  holds the exploration rounds; the PNGs are round-5 snapshots, not final. `Icons/` holds the uploads
  the custom tab icons were redrawn from (not shipped). Open the HTML files in a browser (bundled pages, ~15 MB each).
  Docs are grouped by subject, not by author. Anything drafted by Claude carries a provenance
  line under its title saying so and giving its status — keep that line accurate when editing.
- `tmp/` — scratch, gitignored. Never put deliverables here.

## Conventions

- Language: English for code, comments, and docs.
- Keep `PLAN.md` current: mark tasks done, add follow-ups discovered along the way.
- Prefer small, reviewable changes; explain trade-offs before large refactors.
- Every new data point gets a category, a sub-category, a storage shape and the screen(s) it shows on
  (`Nav. Screens`) before it gets a screen — see `docs/data-structure/` and PLAN.md → Decisions.
  Adding a tracker should be a registry entry, not a new navigation destination.
- Navigation: four bottom tabs — **Today** (quick-log row, pinned metrics, medications due,
  symptom check, appointments in the next 14 days), **Tracker** (vitals,
  symptoms, activity, sleep, mood, cycle, ostomy output), **Care** (treatments, appliances, the ostomy module,
  appointments) and **Records** (labs, documents, encounters, the History timeline); "Care" is a working name. Details and rationale:
  PLAN.md → Decisions → *App navigation*.
- Logging: quick entry lives on Today as a row of shortcuts opening a bottom sheet. No floating
  action button. Tracker shows a list with 7-day sparklines (default) or 2-up widgets, a stored
  display preference; each row reads with a one-line takeaway, and the mini-chart kind comes from
  the metric's registry entry.
- Data shown on two tabs (e.g. peristomal skin rating in Care and Tracker) is stored once; the second
  tab reads from the same registry entry, and keeps its category color.
- UI follows the locked design (PLAN.md → Decisions → *Visual design*): color = data category (an
  `fg` and a `tint` per category and mode), near-black ink as primary, metric values always in IBM
  Plex Mono, filled button only for a screen's main action. Take colors, sizes and radii from the
  theme tokens, never hard-coded hex or dp in screens. Screens not in the design (Care, Records,
  Settings…) reuse its components instead of inventing new ones.

## Stack

Decisions and rationale: `PLAN.md` → Decisions, and the Notion pages it links to.

- Kotlin 2.4, Jetpack Compose (BOM), JDK 17. Native Android only.
- minSdk 26, compileSdk / targetSdk 36. Single `:app` module, one package per feature.
- UI: own theme tokens (`core.ui.theme`), no Material3 dependency. Fonts: Bricolage Grotesque
  (display, headlines, titles), IBM Plex Sans (body), IBM Plex Mono (metric values). Icons: Material
  Symbols Outlined. Light and dark themes.
- Backend: Firebase — Auth (Google sign-in via Credential Manager), Cloud Firestore, Cloud Storage.
- Data: Health Connect for sensor data, cached in Room; user-logged data in Firestore only.
  Patient-facing categories and the seven storage shapes: `docs/data-structure/`.
- Charts: Vico 3.x (`com.patrykandpatrick.vico:compose`), only behind the `ChartSpec` mapper.
  Never import Vico outside that file. Tracker's 7-day mini charts are our own Canvas drawing
  (`core.ui.components.MiniChart`), not Vico.
- Background work: WorkManager. Settings: DataStore.
- Dependency injection: manual wiring — `AppContainer` built in `LauWellApplication`, no Hilt.
- Privacy: no health data or tokens in logs.
