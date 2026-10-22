# LauWell — Plan

## Decisions

Full rationale lives in Notion (LauWell App • Home → Table of Content). Summary:

- **Mobile:** Kotlin + Jetpack Compose, native Android only. No Material3 dependency (own theme tokens).
- **Code structure:** single `:app` Gradle module, one package per feature (`feature.vitals`, …) plus
  shared `core.*` packages. Data is shown or hidden at runtime, not at build time.
  → Notion: *Modular Architecture*; `docs/architecture/feature-modules-and-toggles.md`.
- **Toggles:** no separate "feature" concept. The data hierarchy itself is the toggle unit:
  category → sub-category → metric (7 / 35 / 114 since 2026-10-19, see *Simplified data model*; 5 / 35 / 81 before it), and any level can be switched off. The app stores
  the **switched-off** set (empty = everything visible, new metrics appear on their own), and one
  `isVisible` function is the only place that reads it. Hiding never deletes data. Permissions and
  syncs are declared per metric and run only for visible ones. MVP: everything on, no selection
  screen after sign-in; the Settings toggles are backlog. Ostomy is its own category since
  2026-10-17, so its category switch hides all of it; a switch per condition across categories stays
  deferred until another condition needs one.
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
- **Read first** (decided 2026-10-18). The current goal is reading Health Connect data and showing
  it properly; manual entry is not a goal for now. M3 starts with **heart rate**, not steps, and M4
  (manual entries) waits, together with the logging groundwork that only manual entries need.
  - **Why heart rate first:** one record type, no goal, no manual path to reconcile, and its weekly
    range bar is the hardest chart, so M3's chart validation test runs on real data from day one.
  - **Steps and duplicates:** the same walk reaches Health Connect two or three times, since Garmin
    (watch), Samsung Health and Google Health all write steps to it. Summing the raw `StepsRecord`s
    over-counts. Health Connect's aggregate calls (`aggregate`, `aggregateGroupByPeriod`)
    deduplicate using the user's app priority (Health Connect → Data and access → Steps → Data
    sources and priority); `readRecords` does not. Rule: **daily totals of cumulative data (steps,
    distance, energy) come from Health Connect's aggregation, never from adding records up**; raw
    records are read only to show which app or device a value came from. Check on device which
    source wins once all three apps are writing.
  - **Heart rate also arrives from several apps.** Min and max are not changed by a duplicate; the
    average leans towards whichever app samples more often. Keep each sample's data origin, take the
    daily min / average / max from Health Connect's aggregates, and check on device whether those
    apply the app priority too.
  - **Two sources for one metric** (the open rule of the provenance task), for now: between apps,
    Health Connect's priority list decides and we keep each record's data origin; manual vs device
    is decided with M4.
- **Charts:** Vico 3.x (`compose` module, no Material), behind our own `ChartSpec` layer; only one
  mapper file imports Vico (`core.ui.chart.VicoChart`). Pinned to **3.2.3**: 3.3.x needs compileSdk 37
  and AGP 9.1, and every Vico 3 needs Compose ≥ 1.10 (Compose BOM 2026.06.01, Compose 1.11, since
  2026-10-20). Koala Plot is the fallback; MPAndroidChart v4 to revisit once mature.
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
- **Patient data categories** (2026-10, revised 2026-10-11 — see *Simplified data model* below: Labs is now a sixth category and some placements changed): five patient-facing categories, each with a test that decides where a
  new data point goes. **Vitals** — a measured value read against a clinical normal range.
  **Medical Records** — a fact with a start date rather than a timestamp, or the record of an
  encounter (absorbs appointments, visits and providers; renamed from "Medical history", which
  signalled *the past* and hid the active diagnosis list). **Lifestyle** — a daily log measured
  against a goal you set. **Symptoms** — self-reported, no instrument involved. **Treatments &
  appliances** — what you take or use, and the products behind it.
  Vitals vs Lifestyle is settled by one question: *clinical normal range, or personal goal?* That is
  why mood and the menstrual cycle sit in Lifestyle without an exception, and why stoma output volume
  sits in Vitals (sustained high output is a clinical dehydration threshold) while the change routine
  stays in Lifestyle. 35 sub-categories form the second level, identified by category + name
  ("Respiratory" exists under both Vitals and Symptoms).
  → `docs/data-structure/health-data-model/Patient Data Categories.html` (+ `.csv`).
- **Simplified data model** (decided 2026-10-11; the Notion database *Data Model [Simplified]* is the
  source of truth for rows, and `docs/data-structure/` is generated from a copy of it, regenerated
  2026-10-17). Prompted by the
  interface design: many data points were far from current priorities, so the map was cut down and
  each point now says where it is shown.
  - **Seven categories:** Vitals, **Labs** (new — promoted out of Vitals → Laboratory, so lab work gets
    its own home), Medical Records, Lifestyle, Symptoms, Treatments & Appliances, and **Ostomy** (added
    in Notion 2026-10-17, see *Ostomy category* below).
  - **`Nav. Screens` column:** each data point is tagged with the tab(s) that show it. A point is
    stored once and may be shown on several tabs (e.g. peristomal skin rating: Treatments & Appliances,
    tagged Care + Tracker). Today is not a tag: it reads from the same registry. The column offers
    Tracker, Records and **Care** (Care added and the Treatments & Appliances rows retagged in Notion,
    2026-10-15).
  - **Tab ↔ category:** Tracker = Vitals, Lifestyle (activity, sleep, mood, stress, cycle), Symptoms,
    and the Ostomy signals; Records = Labs, Medical Records; Care = Treatments & Appliances
    (medication, appointments) and the Ostomy interventions. The `Nav. Screens` tag of each row decides.
  - **Labs:** one row per analyte (sub-category = panel: hematology, lipid profile, iron studies …),
    seeded from `docs/data-structure/health_data_labs/common_lab_tests.csv`. Same `Measurement` shape
    with `source = lab`; the reference range printed on each report is kept.
  - **Ostomy** (superseded 2026-10-15 by *Ostomy placement*, then 2026-10-17 by *Ostomy category* below) now sits entirely under Treatments & Appliances → Ostomy (output volume and
    consistency, appliance change with its reason, pouch emptying, accessories used, peristomal skin).
    It used to be spread over Vitals, Lifestyle and Symptoms. A leak is a *reason* on an appliance
    change rather than its own event. This largely settles the "one switch per condition across
    categories" backlog item: switching off the Ostomy sub-category now does it.
  - **Cut or merged** (the old map keeps them if they come back; as of 2026-10-15 the Notion table
    has brought back ECG trace, sleep quality rating and headache / migraine, so they are no longer
    cut): from Vitals — heart rate
    variability, peak flow, ketones, weight, body composition, temperature, height; from
    Medical Records — pregnancy, family history, allergy, intolerance, provider, blood type, imaging
    study (pictures and documents remain); from Lifestyle — nutrition and intake, mindfulness, sleep
    quality rating, travel and weather context; from Symptoms — flare, suspected trigger,
    breathlessness, dizziness, skin condition.
  - **Moved:** menstrual cycle from Lifestyle to Vitals (sub-category Woman Health, shown in Tracker);
    visit / encounter is now *Note (Encounter)*, attached to an appointment (Appointment and Encounter
    are two records since 2026-10-17, see *Appointments and encounters*).
  - **Not finished, by choice:** Treatments & Appliances holds only the Appointment row since the
    ostomy rows became their own category. Medication
    (catalogue, scheduled / taken / skipped doses, as-needed, supplements, side effects), therapies,
    devices, supplies and trial periods stay as defined in the old map (`Patient Data Categories-v03`)
    and get re-added later; this does not block the rest.
  - The Notion table has no *Shape* column. The seven shapes and the catalogue + occurrence rule
    below still apply; shapes live in `_src/rows.js` (carried over from the old map where the row
    existed, assigned for the new ones: every lab analyte is a `Measurement`).
- **Ostomy placement: signal vs. intervention** (*superseded 2026-10-17 by* Ostomy category *below;
  kept for the reasoning, which survives as its two sub-categories*) (decided 2026-10-15; revises the *Ostomy* bullet of
  *Simplified data model*, which put every ostomy row under Treatments & Appliances). Category follows
  what the data means; the screen follows when it is captured. The Notion table *Data Model [Simplified]*
  stays the source of truth for the rows.
  - **Sub-category stays `Ostomy`, under both categories** (identity = category + name, as Respiratory
    already exists under Vitals and Symptoms). No separate "Digestive output": it would collide with
    Symptoms → Digestive and would only do the grouping job, not the condition-switch job.
  - **Vitals → Ostomy**: Output Volume and Output Consistency (Tracker), Pouch Emptying (Tracker + Care). Signals read
    between changes against a clinical threshold (sustained high output = dehydration risk), like heart
    rate. Emptying is the logged event; frequency is derived from it. Stored once; where Care shows one,
    it reads the same registry entry and the metric keeps its Vitals color.
  - **Treatments & Appliances → Ostomy** (Care): Appliance Change (+ reason; a leak is a reason, not its
    own event), Accessories used,
    Wear time (new, derived from consecutive changes; the headline outcome for comparing products) and
    Peristomal Skin Rating (one scored rating, assessed once at each change; Tracker + Care read-through).
    Nav tags as in Notion: Accessories and Wear Time = Care; Appliance Change and Skin Rating = Tracker + Care.
  - **Rule of thumb:** read between changes = signal (Vitals); done or assessed at the change =
    intervention (Treatments & Appliances). Phases: daily signal -> intervention -> outcome (wear time, leaks).
  - **Settings toggle (proposed):** a Settings tree (category -> sub-category -> metric) hides things
    everywhere. "Ostomy" appears under two categories, so Settings shows one **Ostomy** switch that hides
    both groups, with the per-category rows underneath for fine control. This keeps the "one switch per
    condition" backlog item solved.
  - **Export:** a stoma-nurse export reads every row whose sub-category is Ostomy, across both categories.
  - **Why:** Vitals keeps one rule (*clinical normal range?*), thresholds and group cards work for output
    as for Cardiovascular, and the change routine stays one phase.
- **Ostomy category** (decided 2026-10-17, made in the Notion table). Ostomy is a seventh category, so
  everything about the stoma sits together, one category switch hides all of it, and a stoma-nurse
  export reads one category. The signal / intervention split survives as two sub-categories:
  - **Ostomy • Daily Routines:** Output Volume, Output Consistency, Pouch Emptying (Tracker);
    Peristomal Skin Rating (Tracker + Care); Accessories used (Care).
  - **Ostomy • Replacement:** Appliance Change (+ reason; a leak is a reason, not its own event) and
    Wear Time (computed from consecutive changes), both Care.
  - Nav tags changed with it: Appliance Change is now Care only (was Tracker + Care), Pouch Emptying
    Tracker only (was Tracker + Care).
  - **Color: amber, hue 80** (decided 2026-10-17). The widest gap in the six design hues is between
    Vitals (28) and Lifestyle (150); 80 sits 52° from the red and 70° from the green, and reads apart
    from both in fg and tint. Same OKLCH recipe as the others, values in *Visual design*. (The design
    drew ostomy in teal, Treatments & Appliances; that predates the category.) Amber is also the
    usual "warning" hue: if out-of-range states ever get a color, it must not be amber.
- **Storage shapes:** category and sub-category are *facets on the metric definition*, not tables.
  Seven shapes carry everything: `Measurement` (value·unit·instant), `Event` (instant + optional
  duration), `Interval` (start·end·status), `StandingFact` (no instant — blood type, family history),
  `CatalogueItem` (a thing with attributes), `Document` (a file with a date), `Derived` (computed —
  e.g. wear time from consecutive appliance changes). The map of 114 data points to shapes lives in the
  same doc, and its shape×category matrix is generated from the rows so the two cannot drift.
- **Terminology codes** (decided 2026-10-09; options and verdicts in the Notion *Data Model* page §2):
  standards give a metric its identity, not navigation. Each metric definition can carry codes (FHIR
  `Coding` shape) and a UCUM unit, for interoperability (FHIR export, clinician sharing, lab import) and
  unambiguous structured data. Codes live on the definition, not on records (lab results excepted: per
  analyte), and are never load-bearing in app logic — our own metric IDs are; a metric with no standard
  code gets a local namespace. Core set: LOINC + UCUM for measurements and labs now; a SNOMED CT subset
  for symptoms / conditions / procedures when symptoms are built; Health Canada DIN + WHO ATC for
  medication; GS1 GTIN to fill the ostomy product catalogue; Health Connect record types are the input
  mapping. Considered and skipped: openEHR, Open mHealth, IEEE 11073 (ICD-11 only if an administrative
  export is ever needed; ICPC-2 to evaluate for the symptom vocabulary). Lab reference ranges vary by
  lab, age and sex and LOINC does not carry them: store the range printed on each result, with the MUHC
  list (`docs/data-structure/health_data_standards/`) as a fallback seed. First pass: 16 data points
  coded, 11 codings still unverified. → `docs/data-structure/health-data-model/Patient Data Model.csv` (combined sheet),
  `Patient Data Codings.csv` (one row per coding).
- **Catalogue + occurrence:** wherever a category holds both a *thing* and an *act*, they are two
  tables — medication vs dose, ostomy product vs appliance change, device vs reading, supplier vs
  order. Product attributes (brand, reference code, convexity, flange size, barrier type) belong to the
  catalogue entity and are **never free text on the event**. This is what makes "did the 25 mm convex
  barrier give longer wear time and better skin than the flat one" answerable; without it that
  question is permanently out of reach, and it is the one thing no existing ostomy app does.
- **Relationships between records** (decided 2026-10-17). Records point at each other, two ways:
  - **Named fields for links that define a record**, which logic and statistics rely on: a dose → its
    medication, an appliance change → its ostomy product, a medication → its prescription, a
    prescription → the encounter that issued it, an encounter → the appointment it is based on.
  - **Generic links for context**, on every record: `links: [{to, relation}]`, e.g. a BP reading
    *recorded during* an encounter, a photo *about* a skin rating, a note *about* a symptom. Shown
    generically ("Linked to: Consultation, 12 Oct"); Firestore finds every record linked to X with one
    `array-contains` query. Links are stored on the record that points, never copied both ways.
- **Appointments and encounters** (decided 2026-10-17). Two records, as FHIR does:
  - **Appointment** = the plan (Notion: *Appointment*, Treatments & Appliances, Care): date and time,
    place, who with, reason, preparation, questions to ask. Status: upcoming → done / cancelled / missed.
  - **Encounter** = what happened (Notion: *Note (Encounter)*, Medical Records, Records): the problem
    discussed, decisions and care plan, and through links the documents, readings and prescriptions that
    came out of it. `basedOn` → its appointment, optional: an emergency visit, a walk-in or a phone call
    is an encounter with no appointment, and a cancelled appointment never becomes an encounter.
  - **Where they show:** upcoming appointments in the **next 14 days** on Today and in their own
    section in Care (like a Tracker category), with a "Later (n)" line under the Care section opening
    the full list. Encounters show in Records → History.
  - **To complete** (confirmed 2026-10-17): once an appointment's date has passed with no encounter, it shows in Care
    under "To complete" and as a light "How did it go?" prompt on Today for a few days; answering
    creates the encounter, prefilled from the appointment. Cancelled and missed appointments appear in
    History, greyed out.
- **Records → History** (decided 2026-10-17): one timeline of everything — entries, appointments,
  encounters, doses taken — newest first, filterable by category, each item in its category color,
  tapping one opens the record and its links. Sensor data appears as one daily summary per day
  ("Steps 8,412 · HR 58–142"), manual entries one by one. It needs the same few fields on every record:
  id, type, category, when, source, a one-line summary and its links.
  - **Firestore layout** (decided 2026-10-17): one `users/{uid}/records` collection with a `type` field
    and a typed body, so History, link lookups and export are each one query (Kotlin keeps one typed
    model per feature). Replaces one collection per feature, which would make History merge one query
    per feature. Catalogue entities (medications, ostomy products) are records too, with their own
    `type`. Health Connect data stays in Room; only its daily summaries are written here. The
    security rules already cover everything under `users/{uid}`.
- **Symptom terminology:** two vocabularies, not one. A coded reference terminology underneath
  (SNOMED CT subset; ICPC-2 to evaluate as a lighter base) for correctness and any export to a
  clinician, and a patient-facing
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
- **App navigation** (decided 2026-10-10, data model simplified 2026-10-11, Today revised by the design
  on 2026-10-12). Four bottom-nav tabs, replacing the category-per-destination idea; which tab shows a
  data point is the `Nav. Screens` column of the simplified model:
  - **Today** — home, top to bottom: date + greeting, the **Quick log** chip row (one tap opens a bottom
    sheet; **no FAB**, logging lives on the screen itself), **Pinned** — six metric tiles in a 3 × 2 grid
    (value, unit, progress bar when there is a goal, context line such as "of 8,000" or "3 days ago"),
    edited from a settings icon in the section header — and **Medications** due today ("2/3 taken"; tap
    the circle to mark a dose taken, no separate Take button), then, **below Medications**, the
    "symptom check?" prompt and **upcoming appointments**. Those last two are missing from the final
    design file by mistake, not by decision: they stay, and need drawing in the locked system. Keep it
    light: only widgets you would act on. The pinned grid replaces the earlier "current status" row
    (tiles wrap instead of scrolling sideways).
    Defaults: quick log = Fatigue, Blood pressure, Ostomy output, Mood, Symptom; pinned = Steps, Resting
    HR, Sleep, Fatigue, BP, Ostomy output. Both sets are default lists first; the entry point to edit
    the pinned set is designed (an icon), the editor screen is not (tracked in M6); adaptive ordering
    is a later option.
  - **Tracker** — Vitals, Symptoms, Activity, Sleep, Mood and stress, Menstrual cycle, each with its own flow (hub → metric
    detail → history and charts), Samsung Health style. The hub is grouped by category (Vitals,
    Lifestyle, Symptoms, Ostomy), with a list | widgets
    toggle and a search icon in the header. **Two views** (Tracker 15a/15b, adopted 2026-10-15,
    `docs/design/UI-spec.md`): a **list** (default) — one row per metric with a one-line takeaway, a
    7-day sparkline and the latest value — or **widgets**, 2-up tiles with value, a 34dp mini chart
    and the takeaway (an odd last tile spans both columns). The choice is one global display
    preference (`TrackerView` in DataStore), kept across sessions; switching keeps the same category
    in view. The takeaway replaced the earlier *synced* / *+ Add* badges: manual entry goes through
    Quick log or the detail screen. Each metric declares its mini-chart kind in the registry: bars
    (steps, sleep, ostomy output; dashed goal line), line + soft area (heart rate, mood, fatigue),
    range bars (blood pressure), severity dots (brain fog), 28-day strip (menstrual cycle); latest
    day in full color, earlier days lighter, a missing day a faint placeholder, never zero. Open in
    the design: grouping, sorting and filtering within Tracker beyond category. Symptoms and Periods are event logs and may need a different detail
    screen; revisit if they strain the shared one. Ostomy output volume, consistency, pouch emptying
    and peristomal skin rating are Ostomy rows tagged Tracker (see *Ostomy category*); the skin rating
    is also shown in Care, stored once.
  - **Care** (working name; "Treatments & Appliances" as the screen title) — medications and the whole
    ostomy module: product catalogue, comparison, changes, issues and entries; and **appointments**
    (decided 2026-10-16, following the later design export, which draws an Appointments screen under
    Care: a timeline of upcoming and past visits, each tagged in its category color — stoma nurse in
    T&A, blood test in Labs, consultation and notes in Medical Records, physio in Lifestyle).
    Confirmed 2026-10-17: Care is "the care I'm receiving", so appointments belong there. Care shows
    the next 14 days of appointments as their own section, with "Later (n)" for the rest (see
    *Appointments and encounters*). How Care holds medication, ostomy and appointments together is
    not designed yet.
  - **Records** — labs, clinical documents, pictures, prescriptions, encounters (what happened at past
    appointments), medical history (diagnosis, surgery, hospitalization, immunisation), insurance, and
    **History**, the timeline of every record (see *Records → History*). *Not designed yet.*
  - A new tracker is still a registry entry, never a new destination; Today's chips, the pinned tiles
    and the Tracker hub read from the same registry.
- **Visual design** (locked 2026-10-12 as direction **13b**, after five rounds of exploration in Claude
  Desktop; Tracker views 15a/15b added 2026-10-15; week board 16c and the Fold inner screen, round 18,
  2026-10-17/18). Reference since 2026-10-20 (local only, `docs/design/` is gitignored):
  **`Design • Full Experience - v02.html`** (v01 is the same file), a bundled page: the Galaxy Z Fold **closed** — the cover
  screen, i.e. the phone design used on any phone, nine screens (sign-in, Health Connect grant,
  Today, quick-log sheet, Tracker list / widgets / week board, Steps detail, Care · Appointments) —
  and **open** — the inner screen's chosen layouts — **each in light and dark**. Its phone screens are
  byte-identical to the v18 handoff's; the dark inner screen is new. Written spec: `UI-spec.md` from
  the v18 handoff (`Design • Global direction - v18.html`, a zip despite its name, with the icon
  list; spec and icon list copied loose into `docs/design/`). Superseded: the v15 handoff
  (`Design • Global direction - v15.html`, eight phone screens, no week board, no inner screen) and
  `Design • Global direction - v13b-02.html` (zip, 2026-10-13: Vivid palette, custom tab icons,
  Appointments, PNG screens, `tokens.png`) and the loose `LauWell Final Design (for Figma import).html`
  (before all of those). The exploration rounds are in `LauWell Design Directions.html`; the
  PNGs beside them are snapshots of round 5 (not final).
  - **Look:** page `#F4F6F7`; content sits in light panels (`#FAFBFB`, 1px `#E4E8EB` hairline, 12dp
    corners); section labels (uppercase, 11.5 sp, tracked) sit **outside** the panels on a 2px ink
    rule. Primary is near-black ink (`#16191C`; `#ECEFF1` in dark), not a hue. Light and dark
    themes, following the system.
  - **Type:** Bricolage Grotesque for display, headlines and titles; IBM Plex Sans for body and
    labels; **IBM Plex Mono for every metric value** (numbers, times, "2/3"). Scale: display 56 sp/700,
    headline 28/700, title 18/600, body 16/400, label 11.5/600, metric 20/500, metric-xl 60/500.
  - **Color = data category.** Six hues (Vitals, Labs, Medical Records, Lifestyle, Symptoms,
    Treatments & Appliances), each with an `fg` (text, icons, bars) and a `tint`, per mode. A metric
    keeps its category color wherever it is shown (peristomal skin keeps its color in Tracker and
    Care). Ostomy, the seventh category since 2026-10-17, got the seventh hue (amber, 80; see *Ostomy
    category*), not from the design. Neutrals:
    `bg, surface, surface2, ink, ink2, line, accent, onAccent, panel, panelLine`. Values below, copied
    from the token sheet. (The icon table in the same file uses slightly different hexes; the token sheet wins.)

    | Neutral   | Light     | Dark      |
    |-----------|-----------|-----------|
    | bg        | `#F4F6F7` | `#090B0D` |
    | surface   | `#FFFFFF` | `#12161A` |
    | surface2  | `#E9EDF0` | `#1B2127` |
    | ink       | `#12171B` | `#E4EAEE` |
    | ink2      | `#56616B` | `#93A0AB` |
    | line      | `#16191C` | `#ECEFF1` |
    | accent    | `#16191C` | `#ECEFF1` |
    | onAccent  | `#FFFFFF` | `#0B0D0F` |
    | panel     | `#FAFBFB` | `#14191D` |
    | panelLine | `#E4E8EB` | `#262E35` |

    | Category                | Light fg  | Light tint | Dark fg   | Dark tint |
    |-------------------------|-----------|------------|-----------|-----------|
    | Vitals                  | `#D4312A` | `#FFD9D2`  | `#FF8474` | `#3F1915` |
    | Labs                    | `#8C51D9` | `#E9DEFF`  | `#CA99FF` | `#2B1F3F` |
    | Medical Records         | `#0073E9` | `#CFE7FF`  | `#60B9FF` | `#102743` |
    | Lifestyle               | `#00952A` | `#CEEFD3`  | `#4CD676` | `#0C2E16` |
    | Symptoms                | `#C83184` | `#FFD8E8`  | `#FF81C3` | `#3C1929` |
    | Treatments & Appliances | `#0095A5` | `#BFEFF2`  | `#00D7E4` | `#002F32` |

    This is the **Vivid** palette of the later export (switched 2026-10-16; the earlier, softer one had
    Vitals `#984742`, Symptoms in amber `#895700`). Quick-log chip strokes are a mid-tone of the
    category hue, not on the token sheet: light Vitals `#FF998A`, Labs `#CCA8FF`, Medical Records
    `#7DC1FF`, Lifestyle `#75D78D`, Symptoms `#FF96C7`, T&A `#00D8E1`; dark `#932C25`, `#623E96`,
    `#0C54A0`, `#006925`, `#8B2B5D`, `#006A74`. The design derives every category color in OKLCH:
    fg L 0.57 / chroma 0.20 (dark L 0.78 / 0.18), tint L 0.92 / 0.05 (dark L 0.27 / 0.06), stroke
    L 0.80 / 0.14 (dark L 0.45 / 0.14). Hues: Vitals 28, Labs 300, Medical Records 255, Lifestyle 150,
    Symptoms 352, Treatments & Appliances 200; Ostomy 80 (ours, same recipe): light fg `#B26200`,
    tint `#F6E2C0`, stroke `#EDB345`; dark fg `#F2A700`, tint `#362200`, stroke `#7C4800`. The design's
    values come out exactly when each sRGB channel is clipped, so Ostomy was computed that way.
  - **Shape and spacing:** panels 12dp; chips, controls and icon tiles 8dp; spacing scale
    4 / 8 / 12 / 16 / 20 / 24 / 32 dp, plus 14dp inside panels.
  - **Tokens the screens use beyond the token sheet** (read from the design's render code,
    2026-10-15, now in the theme): `lineSoft` `#D9DFE4` / `#252D35` (dividers outside panels, nav top
    edge, missing chart day) and `trackBg` `#E6EAED` / `#262E35` (empty goal bar); shapes
    `toggleTrack` 10dp (the list | widgets switch; `badge` 6dp went with the + Add badge),
    `sheet` 14dp top corners, `bar` 2dp; text styles `item` (Plex Sans 15/500), `caption` (12/400)
    and `metricSmall` (Mono 15/500) for rows, tiles and sheets.
  - **Controls:** a filled ink button only for the screen's main action (Save, Allow, Continue with
    Google); 1.5px stroke otherwise. Quick-log chips are outlined in a mid-tone of the category hue,
    with the icon in the category `fg` and the label in ink (the locked render; an earlier note said
    both in `fg`). Metric icon tiles are outlined in the category `fg`, with no fill.
    Rating input = ten outlined pips, the selected one filled. Bottom nav on the page background;
    the active tab is a filled icon + bold label, with no pill.
  - **Icons:** Material Symbols Outlined (weight 400, FILL 0; FILL 1 only for the active tab), listed
    with their color in `lauwell-icons.csv` (v15: `view_list` / `grid_view` added for the Tracker
    toggle, `sync` dropped with the Synced badge; `add` stays for the Appointments header). Three custom glyphs: a hexagon gear for "edit pinned"
    (24 × 24, 1.5 stroke: a rounded hexagon with a radius-3 circle in the middle), and the **Tracker
    (target) and Records (folder) tab icons** (24 × 24, 1.6 stroke, 2.1 when active, round caps),
    which replace Material `monitoring` / `folder_open` in the later export
    (`docs/design/Design • Global direction - v13b-02.html`, a zip: `export/icons/`,
    `lauwell-icons.csv`). Those two were redrawn in the design tool from the uploads in
    `docs/design/Icons/` and share no path data with them, so the SVG Repo originals don't ship.
  - **Chart conventions** (Steps detail): bars in the category `fg`; days below goal dimmed; today
    outlined; a missing day drawn as a dashed empty slot (`null`, never 0); dashed goal line; Week /
    Month / 3 months selector; stats row (daily average, best day, days at goal); per-day list with
    notes ("goal met", "watch not worn"); a source line ("Health Connect · Pixel Watch · synced 09:12").
  - **Fold, open (inner screen ≈ 884 × 780 dp, expanded width), round 18:** switch on the window
    size class and keep state (selected metric, day, Tracker view) across fold / unfold; the cover
    screen keeps the phone layout unchanged. An **80dp navigation rail** (same four tabs and icons)
    replaces the bottom bar. Tokens unchanged. Chosen layouts:
    - **Today = widget dashboard (17a/18a):** 4-column grid, 10dp gap, 118dp rows — Steps 2×2,
      Medications 2×2, Fatigue 2×1, Sleep · HR · BP · Output · Mood · Brain fog 1×1, Quick log
      4×1; header date + greeting · Edit. **Edit:** dashed outline per widget, × to remove, resize
      handle, "Add widget"; metric taps disabled while editing; stored like the pinned set
      (ordered IDs + size) in DataStore.
    - **Tracker = week board + day pane (16c/18b):** 152dp name column, 7 × 44dp day columns,
      46dp rows; day pane 290dp: every metric's value that day, medications, "Log for this day".
      A day column selects the day.
    - **Tapping a metric** (a Today widget, a board row name, a row in the day pane) opens **list +
      detail with compare (16b):** Tracker list 370dp (selected row on `surface2`) · detail: value,
      Week / Month / 3 months, chart, *Compare with* chips (a second metric as a dashed overlay,
      scaled to fit; not for the cycle), stats, entries, source. The back arrow returns to the
      dashboard or the board.
    - Not drawn: Care and Records on the inner screen. Alternatives kept in the file: 16a Today
      always on the left, 17b journal + always-open log, 17c visit prep.
  - **Tracker week board (16c, also on the phone):** a third Tracker view (`view_list` | `grid_view` |
    `calendar_view_week`, stored as `TrackerView = LIST | WIDGETS | WEEK`): one shared Mon–Sun axis
    so days line up; row 36dp: icon + short name (106dp column) · 7-day lane (7 × 30dp); category
    labels as thin sub-headers; header with the week range and ‹ to go back; the selected day a
    full-height `surface2` band; below the board a day summary row ("Sunday 11 Oct · Today — 7 of
    9 metrics logged") that opens that day's entries, where you can also back-log. The cycle lane
    shows the 7 cycle days as pills.
  - **Screens designed:** sign-in, Health Connect grant, Today (without its symptom-check prompt and
    upcoming appointments, see App navigation), quick-log sheet (fatigue), Tracker list, widgets and
    week board, Steps detail, Appointments (Care tab) — each in light and dark — and the Fold inner
    screen above (light and dark).
    **Not yet:** Today's symptom-check prompt and upcoming-appointments section, the rest of Care, Records, Labs
    (by report or by analyte), heart-rate range chart, Settings, the pinned editor, the symptom
    picker, empty and error states, appointment detail, Care's upcoming-appointments section with
    "Later (n)" and "To complete", encounter detail with its linked records, Records → History, Care
    and Records on the inner screen.

## Milestones

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
  - [ ] Bars seen on device (no bar metric until steps) and the dashed empty slot of a range bar
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
- [ ] Heart rate: the detail screen as designed — per-day list, range selector, source line naming
      the apps and devices (row and validation chart done above)
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
- [ ] Then steps, with the duplicates rule (Decisions → *Read first*: Garmin, Samsung Health and
      Google Health all write steps; daily totals from Health Connect's aggregation): pinned tile,
      Tracker hub row, and the detail screen as designed (big Mono value + goal progress, chart,
      stats row, per-day list with notes, source line). The design assumes a default goal of 8,000
      steps (sleep: 8 h) until user-configurable targets exist
- [ ] Then sleep
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
