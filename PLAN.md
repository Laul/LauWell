# LauWell — Plan

> **Tasks and decisions moved to Notion on 2026-10-23.** The database *LauWell Planning* (see `CLAUDE.md` → Planning)
> is the source of truth for milestones, tasks and their status, and *LauWell • Decisions Tracker* for decisions. This
> file keeps a one-line index of the **Decisions**, linking to their entries, and the *Milestones* section, now a frozen
> archive of what was done and why (kept for its implementation notes). Do not tick or add tasks here.

## Decisions

The decisions and their rationale live in the Notion database **LauWell • Decisions Tracker**
(https://app.notion.com/p/4039d84e94a180178fe4fc6a285c7b82), the source of truth since 2026-10-23: each
entry has its date, status (Active / Superseded) and the Planning task(s) it relates to. This list is a
one-line index into it, kept so code and docs can keep pointing at *PLAN.md → Decisions → <name>*. Change
the Notion entry first, then its line here. (The full text as it stood here before the move is in git at
commit `b4ea515`.)

- **Mobile:** Kotlin + Jetpack Compose, native Android only; no Material3 (own theme tokens). → [Notion](https://app.notion.com/p/4039d84e94a181bda300e75ccf791d8d)
- **Code structure:** single `:app` module, one package per feature plus `core.*`; data shown or hidden at runtime. → [Notion](https://app.notion.com/p/4039d84e94a181679337cb8043ff8cfc)
- **Toggles:** category → sub-category → metric is the toggle unit; the app stores the switched-off set, read by one `isVisible`. → [Notion](https://app.notion.com/p/4039d84e94a181d1b4c3ff007ff27665)
- **Backend:** Firebase (Auth, Firestore, Storage), custom FHIR-inspired schema; Medplum and Supabase rejected. → [Notion](https://app.notion.com/p/4039d84e94a181f197c5e0114c45d5c1)
- **Auth:** Google sign-in via Credential Manager → ID token → Firebase `signInWithCredential`. → [Notion](https://app.notion.com/p/4039d84e94a181acbc1ae7e819359aa7)
- **Data placement:** Health Connect data cached in Room (source of truth for charts), daily summaries to Firestore; user-logged data in Firestore only. → [Notion](https://app.notion.com/p/4039d84e94a1810a9f28c2aa1e182d1a)
- **Read first:** reading Health Connect before manual entry, heart rate first; daily totals come from Health Connect's aggregation, never summed records. → [Notion](https://app.notion.com/p/4039d84e94a181c1a233ed965681b8a5)
- **Health Connect sync triggers:** sync on every return to the foreground and every 20 min in the background (WorkManager), only while signed in. → [Notion](https://app.notion.com/p/4039d84e94a181c8aad0c7c4ad77e9e4)
- **Background read permission:** asked on the grant screen with the metrics and past data; without it only foreground syncs run. → [Notion](https://app.notion.com/p/4039d84e94a181e7adbce071bf69f978)
- **Blood pressure from readings:** a blood pressure day is the mean of its readings (with their count), since Android 14+'s Health Connect can't aggregate it; readings bypass the app priority. → [Notion](https://app.notion.com/p/4039d84e94a1810882f3eafa53a7675c)
- **Charts:** Vico 3.2.3 behind our own `ChartSpec` layer, imported only in `core.ui.chart.VicoChart`. → [Notion](https://app.notion.com/p/4039d84e94a181aa8589c0172399ba9c)
- **Dependency injection:** manual `AppContainer` built in `LauWellApplication`, constructor injection, no Hilt. → [Notion](https://app.notion.com/p/4039d84e94a18108afccec13c3f5be86)
- **Lessons from TrackAid:** layered design, typed metric definitions, `Instant` for time, `null` for missing data, no health data in logs. → [Notion](https://app.notion.com/p/4039d84e94a181288fa6d40ecd9ad4d8)
- **Patient data categories:** one test per category; Vitals vs Lifestyle = clinical normal range or personal goal. → [Notion](https://app.notion.com/p/4039d84e94a181cd90f3e14f421cdfdc)
- **Simplified data model:** seven categories, a `Nav. Screens` column per data point; the Notion table *Data Model [Simplified]* is the source of rows. → [Notion](https://app.notion.com/p/4039d84e94a181838a93c124b0a96d20)
- **Ostomy placement: signal vs. intervention:** *superseded* by *Ostomy category*; kept for its reasoning. → [Notion](https://app.notion.com/p/4039d84e94a181cc8de6d83afe293c8e)
- **Ostomy category:** a seventh category (Daily Routines + Replacement), amber hue 80. → [Notion](https://app.notion.com/p/4039d84e94a18137a32ce07901e41bb1)
- **Storage shapes:** seven shapes (`Measurement`, `Event`, `Interval`, `StandingFact`, `CatalogueItem`, `Document`, `Derived`); category and sub-category are facets. → [Notion](https://app.notion.com/p/4039d84e94a1816e9374ead68ad83d85)
- **Terminology codes:** codes live on the metric definition and are never load-bearing; LOINC + UCUM first. → [Notion](https://app.notion.com/p/4039d84e94a18144a025c800c94dae6a)
- **Catalogue + occurrence:** a thing and an act are two tables; product attributes never free text on the event. → [Notion](https://app.notion.com/p/4039d84e94a1810d92a1e362230794f7)
- **Relationships between records:** named fields for defining links, generic `links: [{to, relation}]` for context. → [Notion](https://app.notion.com/p/4039d84e94a181058574c5bccc9364fe)
- **Appointments and encounters:** two records (plan vs what happened); next 14 days on Today and Care; "To complete" for past ones. → [Notion](https://app.notion.com/p/4039d84e94a18111ac69c4ec6d029254)
- **Records → History:** one timeline of every record, newest first, filterable by category. → [Notion](https://app.notion.com/p/4039d84e94a181aa85d3e94a9d100f98)
- **Firestore layout:** one `users/{uid}/records` collection with a `type` field and a typed body. → [Notion](https://app.notion.com/p/4039d84e94a1817a852feb5c0dace879)
- **Symptom terminology:** a coded reference terminology underneath, a patient-facing vocabulary on top. → [Notion](https://app.notion.com/p/4039d84e94a181458920cd797f1697ec)
- **Lessons from the app teardown:** what Samsung, Guava, Bearable, Human Health and OstoBuddy teach. → [Notion](https://app.notion.com/p/4039d84e94a181bf8e1ecd1d02381daf)
- **App navigation:** four bottom tabs — Today, Tracker, Care, Records — quick log on Today, no FAB; a tracker is a registry entry, never a destination. → [Notion](https://app.notion.com/p/4039d84e94a181c3b3a6dd818d8aafed)
- **Visual design:** direction 13b locked: color = data category, near-black ink primary, IBM Plex Mono for values; reference file, tokens, Fold layouts. → [Notion](https://app.notion.com/p/4039d84e94a1813eb85ee3f0d395599e)

## Milestones — archive (frozen 2026-10-23, tracked in Notion)

Status below is as of the freeze; later progress is in the Notion database only.

### M0 — Lock the foundations
- [x] Fill in the Stack section of CLAUDE.md (DI still open, see next task)
- [x] Choose dependency injection → manual wiring via `AppContainer` (see Decisions)
- [x] Settle feature toggles (2026-10-08): the category → sub-category → metric hierarchy is the toggle
      unit, a switched-off set is stored, everything on for the MVP (see Decisions → Toggles).
      The earlier `FeatureDescriptor` proposal is replaced
- [x] Lock the visual design (2026-10-12, direction 13b): tokens, type, icons and six screens
      (see Decisions → Visual design)
- [x] Fonts and icons ship inside the app, nothing downloaded at runtime (decided 2026-10-13): the
      three font families bundled in `res/font`, the Material Symbols in use imported as vector
      drawables
- [x] `docs/design/` is gitignored (decided 2026-10-13): the exports stay local. PLAN.md → Decisions →
      *Visual design* is the in-repo record of the design, so keep it complete enough to build from

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
- [x] Catalogue of the hierarchy (`core.catalog`, 2026-10-08): `Category` (5, now 7), `SubCategory` (35, keyed
      by category + name), `StorageShape` (7), `MetricDefinition` (quantities with unit, precision and
      normal range; aggregation; chart kind; goal — no terminology codes yet) and a `MetricRegistry`, starting with
      the 4 Vitals → Cardiovascular metrics (3 since heart rate variability was cut by the simplified
      model: heart rate, blood pressure, ECG trace). `CatalogMatchesDocsTest` checks the code against the CSVs
      in `docs/data-structure/health-data-model/` (categories, sub-categories, each metric's filing and shape)
- [x] Visibility: `ToggleKey` (category / sub-category / metric, with stable storage keys), `isVisible`,
      `MetricRegistry.visible`, unit-tested; `ToggleRepository` in `AppContainer`, first implementation
      `EverythingOnToggleRepository` (everything on)
- [ ] Review the generic adult normal ranges in `CardiovascularMetrics` (HR 60–100, BP 90–120 / 60–80)
- [x] ~~Split the multi-metric rows in `rows.js` and `codings.js`~~ — dropped 2026-10-15: peak flow,
      body composition and blood type were all cut by the simplified model. Revisit if one comes back
- [x] Theme from the locked design (`core.ui.theme`, 2026-10-13): `LauWellColors` with the ten
      neutrals + a `CategoryColor(fg, tint, stroke)` per category, light and dark (`stroke` is the
      quick-log chip mid-tone); `LauWellColors.forCategory(Category)` is the one lookup;
      `LauWellTypography` with the seven styles; `LauWellShapes`, `LauWellStrokes` (1 / 1.5 / 2 dp)
      and `LauWellSpacing`. Fonts bundled in `res/font` (Bricolage Grotesque and IBM Plex Sans as
      variable fonts, Bricolage's optical size set per style; Plex Mono Regular + Medium), OFL texts
      in `assets/licenses/`. Screens moved to the new color names
  - [x] Material Symbols as vector drawables (2026-10-15): the 25 glyphs in use + filled variants of
        the four tab icons, `res/drawable/ic_*.xml`, converted from Google's Material Symbols SVGs;
        the hexagon gear as `ic_edit_pinned`. Apache 2.0 text in `assets/licenses/`
  - [x] Map `Category.Labs` and `Category.Ostomy` in `forCategory` (2026-10-17)
  - [ ] Show the font licences somewhere in Settings (open-source notices)
  - [ ] Drop the `ExperimentalTextApi` opt-in in `Type.kt` once variable-font resources are stable in Compose
- [x] Shared components drawn in the design (`core.ui.components`, 2026-10-15): `SectionHeader` /
      `Section` (label on the 2dp ink rule, optional category dot and trailing slot), `Panel` +
      `PanelDivider`, `MetricIconTile`, `MetricRow` (Synced / + Add badges then; replaced by the
      takeaway and sparkline, see Tracker views below), `PinnedTile`
      (goal bar), `QuickLogChip`, `FilledButton` / `StrokeButton` / `QuietButton`,
      `SegmentedSelector`, `PipRating`, `LauWellBottomSheet`, `CategoryLine`, `LauWellIcon`.
      Light/dark previews in `ComponentsPreview.kt`; compiled, not yet checked on a device
  - [ ] Check the previews against the design side by side (light and dark), on a device too
  - [ ] Bottom sheet: drag to dismiss, and IME padding once the note field exists (today: scrim tap
        and Back only; drawn in the caller's layout, not its own window)
  - [ ] Toggle switch (Health Connect grant rows) and the medication check row: build with the
        screens that first use them (M3 grant screen, M6 Today)
- [x] Generic navigation (2026-10-15): four tabs (`TopLevelTab`), each a nested graph so a pushed
      screen keeps the bar with its tab selected and each tab keeps its own back stack;
      `BottomNavBar` as designed. Tracker hub grouped by category (Vitals, Lifestyle, Symptoms) from
      `MetricRegistry.visible` → shared `metric/{id}` detail (placeholder: filing + empty value).
      Today = date, greeting and the avatar that opens Settings (Home and its link removed). Care
      and Records are placeholders. `MetricDefinition` gained `icon` and `sources` (Notion *Data
      Sources*: Manual / Wearable). New shared pieces:
      `ScreenHeader`, `TopBar`, `IconAction`, `ScreenColumn`. Not yet run on a device
  - [ ] Ostomy rows on the hub (output, emptying, skin rating): the category and the `Nav. Screens`
        tag exist (2026-10-17); add their `MetricDefinition`s when the ostomy features are built
  - [ ] Tracker search icon (in the design): add once there is a search to open
  - [ ] Today's greeting has no name (`Session` holds only the uid by design); decide whether to
        show the Google first name
- [x] Tracker views (2026-10-16, design 15a/15b): list with 7-day sparklines (default) and 2-up
      widgets, switched by an `IconToggle` in the header and stored as `TrackerView` in the app's
      Preferences DataStore (`core.settings.DisplayPreferences`, first use of DataStore; backup already
      excluded). `MetricDefinition.miniChart` (`MiniChartKind`: Bars, Line, Range, SeverityDots,
      CycleStrip); `MiniChart` draws them with Compose Canvas, ported from the design's own drawing
      rules — not Vico, which stays behind `ChartSpec` for detail charts. `MetricRow` rewritten
      (takeaway + sparkline + value), new `MetricWidget`; Synced / + Add badges, `MetricSource`,
      `ic_sync` and the `badge` shape removed. Until M3/M4 every metric shows "—", "No data yet" and
      a placeholder week; previews use the design's sample week. Checked on device (2026-10-17): both
      views in light and dark, the toggle, the choice kept across launches
  - [ ] Takeaway lines ("60% of goal", "Up from 4 on Mon", "3 readings · in range"): a rule per
        metric or chart kind, written with the first real data (M3)
  - [ ] View toggle segments are 34 × 28dp as designed, under the 48dp touch target: revisit in the
        accessibility pass (M6)
  - [ ] Large screens: designed since 2026-10-17 (Fold inner screen, round 18, in *Visual design*);
        to build — see M6 (Later)
- [x] Restyle sign-in to the design (2026-10-16): six category bars + wordmark, filled
      "Continue with Google", privacy line; errors shown above the button. The "G" is Google's
      current gradient mark, rendered from the official Sign in with Google assets
      (`signin-assets.zip`, Android dark square) to `res/drawable-*dpi/ic_google_g.png`: a vector
      drawable can't hold its conic gradient. Its use is governed by Google's branding guidelines,
      not a licence file. Checked on device in light and dark, sheet opens, sign-in completes
- [x] Reconcile the data map with the simplified Notion data model (see Decisions → Simplified data
      model), then regenerate `docs/data-structure/`. In order:
  - [x] Clean the Notion rows (checked 2026-10-15): Blood Pressure under Cardiovascular, Steps under
        Activity, typos fixed, output-volume description matches its Vitals row
  - [x] Appointments in the Care tab (checked 2026-10-17): the *Appointment* row is tagged Care, filed
        under Treatments & Appliances → Appointment; *Note (Encounter)* stays Medical Records → Appointment,
        tagged Records. Today is not a tag: it reads upcoming appointments from the registry
  - [ ] Still open in Notion: the sub-category list is one flat select shared by all categories, so
        lab panel names leak into other categories — *Blood glucose* is filed as Vitals →
        "Biochemistry - Metabolic" (should be Vitals → Metabolic or similar); "Cardiovascular" now
        exists under Vitals, Labs and Symptoms (fine, identity is category + name). *Ostomy Output
        Volume*'s description still says it "sits in Vitals". (*Ostomy Wear Time* now has Data Sources =
        Computed: fixed)
  - [x] Add a **Care** option to `Nav. Screens` and retag the Treatments & Appliances rows (done in
        Notion 2026-10-15; Tracker shows output volume / consistency / emptying, appliance change and
        skin rating)
  - [x] Apply *Ostomy category* in `_src/rows.js`, the CSVs and `SubCategory` (2026-10-17)
  - [x] ~~Settings: one **Ostomy** switch that hides both Ostomy groups~~ — dropped 2026-10-17: Ostomy is
        one category, so its ordinary category switch does it
  - [x] Pick a hue for Ostomy (amber, 80) and add it to `LauWellColors` (2026-10-17); `forCategory`
        maps it when Ostomy joins `Category`
  - [x] Regenerated from Notion (2026-10-17): 113 rows, 7 categories, 35 sub-categories, with a shape
        (ours), `Nav. Screens` and `Data Sources` on every row; the page rewritten for seven categories
        in the app's category colors. The five-category map is frozen as `-v03` (Categories html/csv
        and the Model csv with its codings). Codings renamed to the Notion names; codings of dropped
        rows survive only in the v03 Model csv. Code moved with it: `Category` (7, T&A label now
        "Treatments & Appliances"), `SubCategory` (35, new set), `forCategory` maps Labs and Ostomy,
        `MetricDefinition.screens` (`NavScreen`) and `DataSource.Computed`; the Tracker hub shows
        metrics tagged Tracker, Ostomy block after Symptoms. `CatalogMatchesDocsTest` now also checks
        screens and sources, and matches names ignoring case (Notion mixes "Heart Rate" and "Blood
        glucose"; the app keeps sentence case)
  - [x] Descriptions for NT-proBNP, Reticulocytes and hs-CRP written in Notion and copied (2026-10-17).
        *Ostomy Output Volume*'s "sits in Vitals" wording is intentional and stays as Notion has it
- Re-scoped 2026-10-18 (Decisions → *Read first*): the rest of M2 is what reading Health Connect
  needs. The logging groundwork (catalogue + occurrence, appointments and encounters, attachments,
  notes, the quick-log set) moved to M4, and the Firestore record conventions to M3, next to their
  first writer, the daily-summary upload.
- [x] Domain model (`core.record`, 2026-10-18): `Measurement` (values keyed by `Quantity.key`, units
      from the definition, `requireMatches(definition)`) and `DailySummary` with one `DayValue`
      subtype per `Aggregation` (`Total`, `Mean`, `Range`, `Last`); a day without data has no
      summary, never a zero. `MetricDefinition` already carries unit, precision, aggregation and
      chart kind plus `category` and `subCategory` as facets. The other six shapes (`Event`,
      `Interval`, `StandingFact`, `CatalogueItem`, `Document`, `Derived`) get their record type with
      their first metric, not before
- [x] Provenance on every record (2026-10-18): `Provenance(source, origin, recordingMethod,
      recordedAt, editedAt)`; `Source` = device / lab / manual / import, `DataOrigin` = the writing
      app's package + `SourceDevice` (manufacturer, model, type), `RecordingMethod` as Health
      Connect reports it (a value typed into Samsung Health is device-sourced, manual-recorded). The
      two-source rule is stated in Decisions → *Read first*
  - [x] Per-metric last-sync timestamp for the source line (2026-10-20): `sync_state` in Room,
        read with `SummaryStore.lastSyncedAt`
- [x] Common record fields (2026-10-18): `HealthRecord` (id, metric, time, zone offset, provenance,
      `links: [RecordLink(to, relation)]`); type = metric + record class, category read from the
      definition; `summaryLine` gives the one-line summary ("Blood pressure 118/76 mmHg"). The named
      links of *Relationships between records* come with the models that need them (M4, M5)
- [x] Today's pinned set (2026-10-18): `PinnedMetrics` in the settings DataStore (`today_pinned`,
      ordered IDs; nothing stored = `DefaultPinned`), `pinnedTiles` keeps the stored order, skips
      hidden metrics, metrics not in the registry yet and those that can't be pinned
      (`MetricDefinition.canPin`: a quantity and an aggregation), at most six. Today shows the grid
      (heart rate and blood pressure for now, "—" and "No data yet"). A metric ID is the snake_case of
      its Notion name, so the defaults already name `steps`, `sleep_duration`, `fatigue`,
      `ostomy_output_volume`. Compiled, not yet seen on a device
  - [ ] The edit icon in the Pinned header waits for the pinned editor (M6); a tile doesn't open the
        metric yet (the detail route lives in the Tracker graph)
- [x] Permissions declared per metric (2026-10-18): `MetricDefinition.healthConnectTypes` (heart
      rate: `HeartRateRecord`; resting heart rate: `RestingHeartRateRecord`, its own metric since
      2026-10-19; blood pressure: `BloodPressureRecord`; ECG
      has no Health Connect type), `readPermissions(metrics)` for the visible ones, and the manifest
      declaring every one of them (`HealthConnectPermissionsTest` keeps manifest and registry equal).
      Requesting them, partial grants allowed, is M3's grant screen, which also needs the
      permissions-rationale activity Health Connect requires
- [ ] *Deferred — Lauranne handles standardization separately; the code carries no codes or UCUM units
      until then.* Terminology: `MetricDefinition.codings` (FHIR Coding shape + UCUM unit) filled from `docs/data-structure/health-data-model/Patient Data Codings.csv`; confirm the `Verified = no` rows in the LOINC search first (glucose, ketones, peak flow and blood type are still candidates;
      HRV is RMSSD in the app, while the drafted code 80404-7 is SDNN). Codes are never load-bearing in app logic

### M3 — Health Connect, starting with heart rate
Builds the reusable reading and display pieces with heart rate (read-only; started with steps
until 2026-10-18, see Decisions → *Read first*).
- [x] Health Connect data source behind a `HealthDataSource` interface (2026-10-19): availability,
      granted permissions, `readMeasurements` (every page read; a heart-rate series = one
      `Measurement` per sample, id `<series id>#<index>`) and `dailySummaries` from
      `aggregateGroupByPeriod` by local day (app priority applied, empty days left out). The mapping
      is pure functions in `HealthConnectMapping.kt`, unit-tested; `HealthConnectDataSource` only
      makes the calls. Not yet run against a real Health Connect
  - [x] History beyond 30 days: done with the grant screen (below)
  - [ ] Reading counts: the aggregation gives no count for blood pressure or resting heart rate
        (`DayValue.Mean.count` is null), so "3 readings" on the Tracker row needs the day's records
- [x] Room database as the local store for Health Connect data; incremental sync (2026-10-20).
      Room caches daily summaries (`daily_summary_value`, one row per metric · day · quantity) and
      `sync_state` (changes token, last sync); raw samples stay in Health Connect. `HealthSync`:
      first sync or expired token → token first, then the whole 90-day window; later → only the
      days the changed records fall on, the whole window after a deletion (no day given), edits
      older than the window ignored; ungranted metrics skipped, one failing metric doesn't stop
      the others, one sync at a time. A schema change drops the cache (rebuilt from Health
      Connect). Sync logic unit-tested with fakes; the DAO is compiled, not yet run on a device
  - [x] Sync when Tracker or Today resumes (their shown metrics) and while a detail screen is open
        (every minute) (2026-10-20)
  - [ ] Still to trigger: on app start and periodically with WorkManager
        (with the daily-summary upload)
  - [ ] DAO instrumented test (in-memory Room) once there is an emulator in the loop
- [x] Daily aggregation + stats, unit-tested (2026-10-20). Daily aggregation is Health Connect's
      (`dailySummaries`, above); `core.record.PeriodStats`: `daySeries` (every day of a range, both
      ends, missing days `null`) and `periodStats` per quantity: days with data, average of the
      daily values (missing days don't pull it down), lowest / highest with their day (a range's
      own min and max; the latest day on a tie), days at goal, days in the normal range. Tests pin
      TrackAid's two bugs (average written into min; the range's last day left out)
  - [ ] Heart rate's period average is the mean of daily averages, not weighted by sample count;
        revisit if the detail screen shows it next to Health Connect's own weekly figure
- [x] Chart validation test (2026-10-20, Galaxy Z Fold API 37, real Garmin data): **pass, Vico
      stays.** Heart-rate 7-day range bars (candlestick layer, a candle body from low to high, no
      wicks) + average dot (line layer, points only) + 60–100 normal band (own `Decoration` drawn
      under the bars; Vico's `HorizontalBox` draws over them) + tooltip built from `ChartSpec` by
      slot, fed by the Room flow while the sync runs every minute (sync time seen moving on screen).
      `ChartSpec` / `rangeBarsSpec` / `yBounds` / `tooltip` unit-tested
  - [ ] See the chart redraw on new data while open (sync the watch with the screen open)
  - [ ] Check dark mode and the unfolded inner screen
  - [ ] A day without data inside the week (all 7 had data): the gap is an x without an entry;
        the design's dashed empty slot comes with the reusable component
- [x] Source line names the devices (2026-10-21, `feature.tracker.SourceLine`): the aggregation
      returns no data origins on this device, so the detail screen reads the headline day's records
      (`Metadata.dataOrigin` + `device`), again only when that day's values change. Each source is
      its device ("Garmin Forerunner 265", manufacturer not doubled), else its app; the watch icon
      when a watch is among them, as designed ("Health Connect · Pixel Watch · synced 09:12"). On
      device: Garmin writes the watch type but no model, so it reads "Health Connect · Garmin
      Connect · synced 21:27" with the watch icon
- [ ] Interim: the detail screen asks for its own metric's permission ("Allow access") and syncs
      on open; replaced by the grant screen and the sync triggers. `PermissionsRationaleActivity`
      (+ activity-alias for Android 14+) is in place: Health Connect showed its prompt with it
- [x] Reusable chart component (2026-10-20): one `ChartSpec` for bars, lines and range bars
      (`chartSpec(metric, summaries, period, today)`), `VicoChart.kt` the only file importing Vico.
      Periods end today: Week (7 days), Month (30 days), 3 months (13 weekly slots: mean of the
      days, extremes of their ranges; 91 daily bars would be unreadable); `SegmentedSelector` on
      the detail screen. Conventions from the Steps detail (v18 source): bars in the category fg,
      the current slot outlined, below-goal bars at 55%, dashed goal line (1.5dp ink2 55%), no y
      axis for bars, a missing day as a dashed empty slot (bars and range bars, week only) and as
      a break for a line; header "14 – 20 Oct" · "Goal 8,000" / "Normal 60–100"; today's label
      bold; labels every 7th day (month) / 4th week (3 months) counted back from today; tooltip
      from the spec by slot. Stats row: "Daily average · Best day · At goal" with a goal, else
      "Lowest · Average · Highest". Headline: the latest day with data ("76 bpm · Yesterday").
      Seen on device: heart rate range bars (all periods), resting heart rate line with its gaps
  - [x] Bars seen on device with steps (2026-10-22): goal line, days under goal dimmed, today
        outlined (transparent, 2dp border: as in the design source)
  - [ ] The dashed empty slot of a range bar, seen on device
  - Note: Vico refuses blank axis labels (crash): labeled slots are chosen with its `ItemPlacer`,
        and `slotLabel` gives a label for any index, also outside the slots — keep it that way
  - [ ] "Compare with" (design v18, Fold inner screen 16b): a second metric as a dashed overlay
- [ ] Chart updates: ViewModel `StateFlow` → mapper → Vico model producer; one refresh at a time
- [x] Tracker rows / widgets and Today's pinned tiles read the synced data (2026-10-20):
      `core.summary.metricSummary` (pure, unit-tested) turns a metric's last 7 daily summaries into
      the Mono value (the latest day's; "118/76" for shared units), the takeaway ("83–130 · today",
      "Yesterday · in range"), the mini chart by `MiniChartKind` (gaps kept, scale padded), and the
      tile's context ("of 8,000" with a goal, else when) and goal progress. `metricSummaries` feeds
      both ViewModels from Room. Seen on device with heart rate (list and widgets)
  - Decision: no "in / above / below range" for a min–average–max day (heart rate): its average
    includes activity, while the 60–100 range is at rest. The detail chart keeps the band
  - [x] Today's tile seen with data (2026-10-20): resting heart rate "76 bpm · Yesterday"
- [ ] Landscape / inner screen: content runs under the side navigation bar (horizontal insets not
      applied; seen on the Fold in landscape, "Good evening" cut at the start)
- [x] Heart rate: the detail screen as designed (row, chart, range selector and source line done
      above)
  - [x] Per-day list (2026-10-21, `feature.tracker.DayList`): the selected period's days, newest
        first, from the oldest day with data; day ("Today", "Tue 20", "Thu 1 Oct" past a week),
        note, value in Mono ("60–118", "118/76" for blood pressure, "—" without data, category
        colour at goal). Notes: "in progress" today for a total or a range, "goal met", "above /
        below range" for a single daily reading, "no data". Normal / abnormal visuals will be
        revisited (Lauranne, UX/UI decisions pending)
- [ ] Resting heart rate (own Notion row and metric `resting_heart_rate` since 2026-10-19, in the
      default pinned set): Tracker row (line mini chart), detail screen (line + normal band), the
      pinned tile on Today (done, see above). Answered 2026-10-20: a `RestingHeartRateRecord` arrives
      in Health Connect once a day (last 30 days present, one gap on 2026-10-14, none yet for today
      at 21:30), so nothing to derive; which app writes it is unknown until the source line reads
      the records' origins
  - [x] Data map (2026-10-19): row added to `_src/rows.js` and rebuilt (114 data points); counts
        updated in the README, the architecture doc and here
- [x] Health Connect grant screen as designed (2026-10-20, `feature.onboarding`): opens once after
      sign-in (DataStore flag, answered either way) and from Settings; one row per visible metric
      read from Health Connect (icon box, "Vitals · read only", `Switch`, the whole row toggles), on by
      default, an already-allowed metric says "allowed"; "Allow N permissions" / "Not now"; the
      privacy line. Health Connect missing or outdated: says so, "Get Health Connect" opens the Play
      Store. Seen on device (unfolded: still the phone layout, see M6)
  - [x] Reading history: `READ_HEALTH_DATA_HISTORY` declared and asked with the metrics when
        Health Connect supports it ("Allow past data" when it is the only thing left). `sync_state`
        records whether the window was filled with history readable; when that changes, the next
        sync refreshes the whole window, since no change event brings the older days (a first
        version ran it from the grant screen and was cancelled when the screen closed). Verified
        on device 2026-10-21: heart rate's 3 months now go back to early August
- [x] Steps (2026-10-22, `ActivityMetrics.Steps`, `READ_STEPS`): pinned tile ("2,669 · of 8,000"
      with progress), Tracker row (bars + dashed goal), detail screen (bars, "Goal 8,000", "Daily
      average · Best day · At goal", per-day list with "in progress" / "goal met", source line).
      Default goal 8,000 until user-configurable targets exist. Daily totals come only from Health
      Connect's aggregation (`StepsRecord.COUNT_TOTAL`, the user's app priority); records are read
      for their origins only, never summed. On-screen numbers group thousands (`displayValue`:
      "8,260", same on every phone). Seen on device: four apps write steps on Lauranne's phone
      (Fitbit, Garmin Connect, Samsung Health, and Samsung Health again with the phone itself)
  - [ ] Check the total against the app ranked first in Health Connect's priority for steps (a
        mismatch would mean a duplicate counted)
  - [ ] Phone models show as codes ("Samsung SM-F971W"): a short model → name table if it matters
- [ ] Sleep, on a **provisional design by Claude** (2026-10-22; Lauranne has no design yet and will
      review market sleep trackers, then revise it with the chart visuals). Seen on device 2026-10-22
      (Garmin is the only app writing sleep on Lauranne's phone; a week of nights with stages):
  - One metric `sleep_duration` (time asleep, minutes, read "7h 12m": `ValueFormat.Duration`) whose
    `breakdown` holds the night's stages (deep, REM, light, and awake beside them, `inTotal = false`).
    This also covers the data map's *Sleep stages* row. Decided (Lauranne, 2026-10-22): two rows in
    the data model don't mean two screens; one sleep screen with its stages is right
  - Nights (`MetricDefinition.nightly`): a night counts on the day it ends, days run 18:00 → 18:00,
    so no night is cut at midnight; an evening nap after 18:00 counts on the next night
  - Duplicates: the night's total is Health Connect's aggregation (`SLEEP_DURATION_TOTAL`, the apps'
    priority) over that window; the app whose sessions last closest to it is taken as the one
    counted, and time asleep and stages come from its sessions only (`sleepNight`), never mixed
    with another app's. Without stages, the aggregated total as is. The source line keeps to that app
  - Screens: pinned tile ("7h 12m · of 8h"), Tracker row (bars + 8 h goal), detail: "Last night"
    breakdown panel (a bar split by stage, then stage · share · time; it is also the chart's legend),
    stacked bars (deep darkest → light palest, the category colour at decreasing opacity), stats
    "Average · Longest · At goal", per-night list ("Last night", "goal met"), source line
  - [ ] Check whether `SLEEP_DURATION_TOTAL` includes time awake (only the choice of app depends
        on it, and only once a second app writes sleep)
  - [ ] Later, if the review asks for it: last night's hypnogram (stages over time), bed and wake
        times, sleep schedule consistency; storing stage intervals, not only their totals
- [ ] Idea, not decided (Lauranne, 2026-10-22, to iterate on): **grouped detail screens**, more than
      one metric or chart on one screen. Today each metric has its own detail, so following the
      cardiovascular metrics (heart rate, resting heart rate, blood pressure…) means opening each one.
      A grouping (by sub-category, or a defined set) could show their charts and stats together.
      Sleep's breakdown is a first step in that direction (several values on one screen)
- [ ] Data conventions (moved from M2): one Firestore collection `users/{uid}/records/{docId}` with a
      `type` field (Decisions → *Records → History*), FHIR-inspired common fields, one typed model
      per feature, repositories returning `Flow<AppResult<…>>`
- [ ] Daily summaries uploaded to Firestore (WorkManager, only for enabled features)

### M4 — Manual entries, starting with fatigue
*On hold since 2026-10-18: reading comes first (Decisions → Read first).*
Adds the entry form and Firestore writes, reusing M3's chart and card.
- [ ] Logging groundwork (moved from M2, 2026-10-18):
  - [ ] Catalogue + occurrence split wherever both exist (see Decisions): catalogue tables for
        medication, ostomy product, accessory, device and supplier; event tables for their use
  - [ ] The named links of *Relationships between records* on the models that need them
  - [ ] Appointment and Encounter as two records (Decisions → *Appointments and encounters*):
        Encounter `basedOn` its appointment; Prescription (Medical Records → Documents, in Notion)
        linked to its encounter and to the medications it prescribes. Past appointment with no
        encounter → "To complete" in Care and a "How did it go?" prompt on Today (confirmed)
  - [ ] Today's quick-log set, stored as ordered metric IDs in DataStore like the pinned set
  - [ ] Attachments: a `Document` entity that can hang off any record, plus unattached files and one
        "all files" view
  - [ ] Free-text note, attachable to any record and able to stand alone with a date (the escape
        hatch — without it people stop using the structured fields)
  - [ ] The manual vs device rule for a metric with both sources (heart rate, blood pressure)
Weight, temperature and height were cut from the data model on purpose (2026-10-11). Fatigue goes first
instead: a single 1–10 rating, so the entry form, the Firestore write and the chart are proven on the
simplest possible input before blood pressure's two values.
- [ ] Fatigue (Symptoms → Systemic, `Measurement`, one 1–10 rating): entry form, Firestore repository,
      list, chart, quick-log chip on Today, Tracker hub row, pinned tile. The entry form is the
      designed quick-log sheet: category line, question ("How tired do you feel right now?"), ten
      pips with labelled ends (1 · Rested, 10 · Exhausted), a time row (now, changeable), an optional
      note, and "Save · 6/10"
- [ ] Then: blood pressure (two values), heart rate (manual entry alongside the synced one)

### Later (review after M3/M4)
- M5 — Logged-data features: medication (+ DIN / ATC lookup from Health Canada's drug database, shared reminder engine; dose taken **vs skipped**, since
  the skipped case carries as much information and most apps drop it), ostomy (GTIN barcode scan to fill the product catalogue, appliance change events
  referencing it, wear time derived from consecutive changes, leaks as a change reason (not their own event),
  peristomal skin, photos in Cloud Storage), glucose logs. **Trial periods** land here — a labelled
  interval on one configuration, so product comparisons run period-against-period instead of being
  averaged across every change ever made and confounded by season, diet and stoma maturation
- M6 — Dashboard and UX: the Today screen (quick log, pinned grid, medications, symptom-check prompt,
  upcoming appointments; see Decisions → App navigation), design Care and Records (incl. Labs) in
  the locked system, accessibility pass (palette and type scale are done: Decisions → Visual design).
  Upcoming appointments (next 14 days) surface on Today as well as in Care; past visits become
  encounters in Records → History — nobody opens an archive to find out what they are doing on Thursday
  - [ ] Design, then build, Today's symptom-check prompt and upcoming-appointments section (below
        Medications), missing from the final design file
  - [ ] Design, then build, the pinned editor (opened from the icon in the Pinned header): pick up to
        six visible metrics that have a latest-value summary, reorder them, reset to defaults
  - [ ] Fold inner screen (designed, Visual design → *Fold, open*): switch layout on the window
        size class, keep state across fold / unfold; navigation rail; Today widget dashboard (with
        its Edit mode, stored like the pinned set); Tracker week board + day pane; list + detail
        with *Compare with*. Also fixes the side-inset bug seen in landscape (M3)
  - [ ] Tracker week board on the phone (third view, `TrackerView.WEEK`) and the day summary that
        opens a day's entries (back-logging there needs M4)
  - [ ] Ink tokens: `Color.kt` has `#12171B` light / `#E4EAEE` dark, the design (and *Look* below)
        `#16191C` / `#ECEFF1`; confirm which is right before the dark-mode pass
- Medications on Today ("2/3 taken", tap the circle when a dose is taken) need the medication
  schedule and dose log from M5; until then the section is hidden or shows placeholder data
- Backlog: Settings toggles over the switched-off set (DataStore-backed; Settings is their home, no
  picker after sign-in); one switch per condition across categories (ostomy no longer needs it, being
  one category; likely a tag on `MetricDefinition` + a fourth `ToggleKey` if another condition does); emergency view assembled from Medical Records + Treatments (blood type, active conditions,
  current medications, allergies, contacts) and reachable without unlocking the app; symptom
  terminology import (SNOMED CT subset or ICPC-2, plus synonym layer — evaluate ICPC-2 first); factor-to-outcome analysis with a minimum-evidence
  gate; Life's Essential 8 survey, lab results (same `Measurement` entity, `source = lab`, with the
  reference range as printed on each report),
  user-configurable targets, BigQuery export for ML, FHIR export, CI (lint, tests, build)
- Maintenance (from the M1 lint run): update the ~14 outdated dependencies and AGP, add an app icon
  (`MissingApplicationIcon`; the six category bars from the sign-in screen are a ready candidate); CI could also run the Firestore rules tests (`firebase/`, `npm test`)

## Done

- [x] Create the Android project in `app/`
- [x] Fix Gradle build (missing wrapper, gradle.properties, JVM toolchain mismatch) — `./gradlew :app:assembleDebug` succeeds
