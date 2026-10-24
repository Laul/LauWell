# LauWell

Android app: a personal hub to monitor healthcare data — vitals (HR, BP, glucose…),
activity and sleep, ostomy appliances and changes, medications, and more.

## Layout

- `CLAUDE.md` — this file: project context and conventions. Read first.
- `PLAN.md` — decisions and their rationale (the *Decisions* section). Its *Milestones* section is a frozen
  archive (2026-10-23): tasks now live in Notion, see **Planning** below.
- `app/` — the `:app` Gradle module (standard Android Studio layout: `app/src/main/...`).
  The Gradle root is the repo root (`settings.gradle.kts`, `build.gradle.kts`, `gradle/`).
- `firebase/` — Firestore security rules (`firestore.rules`) and their emulator tests
  (`npm test` from that folder). Pinned to the `lauwell-app` project in `.firebaserc`.
- `docs/` — documents written by Lauranne.
- `docs/data-structure/` — the data model reference, generated from the Notion table *Data Model
  [Simplified]* (seven categories incl. Labs and Ostomy, a `Nav. Screens` column), which stays the source
  of truth for rows: change Notion first, then `_src/rows.js`, then rebuild.
  - `health-data-model/` — patient data categories, the data map (every data point → category /
    sub-category / storage shape), and the terminology codings (LOINC / SNOMED CT / UCUM).
    `Patient Data Categories` is the current reference; `Patient Data Model.csv` is the combined
    sheet (categories + codings); the `-v03` files are the earlier five-category map. Generated from `_src/` by `node _src/build.js`; the `.csv` files
    are the diffable copies. Earlier inventories are kept beside them as snapshots.
  - `health_data_standards/` — source material for standardization, e.g. the MUHC laboratory
    reference ranges list.
  - `health_data_labs/` — `common_lab_tests.csv`, the seed list for the Labs category.
- `docs/architecture/` — how the app is put together: module and feature-toggle design.
- `docs/research/` — prior-art reviews and competitive teardowns that fed the decisions.
- `docs/design/` — visual design, made in Claude Desktop. **Gitignored, local only**: it may be absent;
  PLAN.md → Decisions → *Visual design* is the tracked record. **`Design • Full Experience - v02.html` is
  the reference** (2026-10-20): the Galaxy Z Fold closed (cover screen = the phone design, for any
  phone, nine screens) and open (inner screen: Today dashboard, Tracker week board, list + detail),
  each in light and dark; the app switches layout when the phone is unfolded. Its written spec is
  `UI-spec.md` from the v18 handoff (`Design • Global direction - v18.html`, a zip despite its name,
  also holding the icon list); `UI-spec.md` and `Icons/lauwell-icons.csv` are copied loose beside
  them. Older exports (`v15`, `v13b-02`, the loose `LauWell Final Design (for Figma import).html`)
  are superseded. `LauWell Design Directions.html`
  holds the exploration rounds; the PNGs are round-5 snapshots, not final. `Icons/` holds the uploads
  the custom tab icons were redrawn from (not shipped). Open the HTML files in a browser (bundled pages, 15–23 MB each).
  Docs are grouped by subject, not by author. Anything drafted by Claude carries a provenance
  line under its title saying so and giving its status — keep that line accurate when editing.
- `tmp/` — scratch, gitignored. Never put deliverables here.

## Planning

Tasks, bugs and milestones live in the Notion database **LauWell Planning** (source of truth since 2026-10-23).
`PLAN.md` keeps only decisions; its old *Milestones* section is read-only history.

- Database: https://app.notion.com/p/4019d84e94a18017b8a2f52244cc9ca9 — data source `collection://4019d84e-94a1-8099-883e-000beeeb2768`.
- Use the Notion MCP tools (`notion-query-data-sources`, `notion-fetch`, `notion-update-page`, `notion-create-pages`).
  If they are not available in the session, say so and stop: do not fall back to editing tasks in `PLAN.md`.
- Two levels only. **Parent** = a milestone (`Type = Milestone`), closed when all its children are Done.
  **Child** = one capability, bug or feature (`Parent item` set). Not a checklist item: group small steps into one row.
- Columns (exact names): `Task Name`, `Type` (Milestone, Feature, Bug, Design, Tech, Verify on device),
  `Status` (Not started, Next up, In progress, Done), `Priority` (`P0 • Critical`, `P1 • High`, `P2 • Medium`,
  `P3. Low` — note the dot on P3), `Effort (days count)` (number of days, estimate), `Screens relation`
  (multi-select: `01. Sign in`, `02. Today`, `03. Tracker`, `04. Care`, `05. Records`, `06. Settings`),
  `Parent item`, `Blocked by` / `Blocking`, `Start Date` (a date or a start–end range; property key `date:Start Date:start` / `:end`),
  `Description`, `PLAN.md ref` (where the item came from in the old plan, may be empty).

**"What's next?"** — query the database for rows with `Type != Milestone` and `Status != Done`, then answer with:
1. anything `In progress`; then
2. tasks marked `Next up`; then
3. the rest by `Priority` (P0 first), then smaller `Effort` first.
Skip a task whose `Blocked by` contains a task that is not Done (fetch the blockers to check) and say which blocker it waits on.
Give the top 3 with parent milestone, effort and one line of why, and ask before starting if several are close.

**While working**
- Starting a task: set `Status = In progress` (and `Start Date` to today if empty).
- Finishing a task: set `Status = Done` and put today as the end of its `Start Date` range. When the last child of a
  milestone is Done, set the milestone to Done too.
- Follow-ups found along the way: create a new child row (set `Parent item`, `Type`, `Priority`, `Effort`, `Screens relation`,
  and `Blocked by` where it applies) instead of adding a TODO to code or docs. Ask before creating a new milestone.
- Put the detail a future reader needs (what was decided, how it was verified, what is left) in the row's `Description`
  or page body, as the old PLAN.md checklist notes did. Decisions with rationale still go to `PLAN.md` → Decisions.

## Conventions

- Language: English for code, comments, and docs.
- Keep the Notion planning database current (see **Planning**): status when work starts and ends, follow-ups as new rows.
  `PLAN.md` is edited only to record a decision (or revise one) in *Decisions*, never to track tasks.
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
