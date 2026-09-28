# TrackAid Review for LauWell

Sep 28, 2026 · @Lauranne

TrackAid got the product ideas right (Health Connect as the source, range-bar vitals charts, target bands) but had no layers: one 420-line class fetched, aggregated and shaped data for the chart library, which caused its refresh bugs and broke on every chart-library upgrade. The review covers the `dev` branch (59 commits, last Oct 2024) and the `healthcareAI` branch; `main` holds only the first 2 commits.

## Architecture

&#91;embedded content: TrackAid today vs LauWell target · 5 layers each\]

In TrackAid, `ModuleData` (red) fetched, aggregated and shaped data for the chart, so every change rippled through it. In LauWell, a local database (blue) sits between the sources and everything the user sees.

## Findings

Six rows are worth carrying over; the problems are mostly structural and fixable by layering from day one.

| # | Area | Verdict | What TrackAid does | What LauWell should do |
| --- | --- | --- | --- | --- |
| 1 | Data source | Keep | Health Connect only; aggregate API for steps and heart rate | Keep. One adapter per outside source (Health Connect, FHIR, manual) behind one interface |
| 2 | Metric config | Keep, upgrade | One `DataProvider` entry per metric: icon, colour, targets, chart type | A typed `MetricDefinition`: unit, precision, target, aggregation, chart kind. No string comparisons |
| 3 | Information layout | Keep | Card with a small chart, then a detail screen (range chart, timeline, hourly steps) | Keep the pattern. Replace the tab-per-metric bottom bar with a metric grid, since LauWell goes beyond vitals |
| 4 | Chart encodings | Keep | Range bar + average dot, target band, tooltip with bold average | Keep, as reusable chart components of your own |
| 5 | Health Connect permissions | Keep, upgrade | Android 14 rationale done right; all-or-nothing grant blocks the app | Partial grants: each metric shows its own "grant access". Onboarding screen when Health Connect is missing |
| 6 | Architecture | Fix | No layers; `ModuleData` fetches, aggregates and shapes chart data | Source, repository with Room, use cases, ViewModel, UI |
| 7 | State | Fix | Global singleton of plain `ArrayList`s; the "need to refresh" bug | Immutable data in a `StateFlow`; one refresh at a time |
| 8 | Chart library | Fix | Data shaped for Vico; 4 library changes each broke data code | Domain emits `TimeSeries` and `DailyRange`; one mapper file builds Vico models |
| 9 | Time handling | Fix | Float epoch millis (about 2 min error); three date APIs mixed | `Instant` in the domain; chart x values computed only in the mapper |
| 10 | Missing data | Fix | 0 means "no data" | `null` or no entry; gaps drawn on purpose |
| 11 | Stats | Bug | Average written into min; today's max skipped | Unit-test aggregation and stats first |
| 12 | Fetching | Bug | `pageSize = 3000`, no pagination | Follow `pageToken`; sync only what changed |
| 13 | History and offline | Missing | No cache; limited to Health Connect's \~30 days before the grant | Room as the source of truth |
| 14 | Security and privacy | Risk | Token and FHIR bodies logged; cleartext allowed; backup on | No health data in logs, no cleartext, backups off or encrypted |
| 15 | FHIR | Keep, upgrade | Android FHIR SDK + Cloud Function proxy; branch marked non-working | Own model first; FHIR mapping only at the edges |
| 16 | Build and repo | Fix | Gradle wrapper not in git, `local.properties` and `.aar` committed, unused libraries | Version catalog, wrapper committed, CI running lint and tests |
| 17 | Tests | Missing | Only the template tests | Aggregation and stats tests; screenshot tests for charts |

## Bugs and risks

The stats bug means the glucose and heart-rate detail screens show Average = 0 and Min = the weekly average today. Links point at `dev` as of commit `ac8f97b`.

| Defect | Where | Effect |
| --- | --- | --- |
| Average assigned to `min` | [ModuleData.kt:403](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L403) | Average shows 0, Min shows the average; the y-axis floor built from it is wrong too ([Views\_Charts.kt:260](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/views/Views_Charts.kt#L260)) |
| Loop stops at `duration`, series has `duration + 1` days | [ModuleData.kt:407](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L407) | Today's max is left out of the weekly max |
| Timestamps stored as `Float` epoch millis | [ModuleData.kt:147](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L147), [:315](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L315) | Points snap to a \~131 s grid; tooltip times off by up to about a minute |
| `pageSize = 3000`, `pageToken` never followed | [ModuleData.kt:132](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L132), [:310](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/data/ModuleData.kt#L310) | Watch heart rate past 3000 records is silently cut |
| Chart data in plain lists, not Compose state | [Views\_Main.kt:197](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/views/Views_Main.kt#L197) | Charts miss the first load (the open TODO in the last `dev` commit); first load and pull-to-refresh can clear a list mid-write |
| Tooltip assumes 3 entries = min, max, avg | [CustomMarkerLabelFormatter.kt:29](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/views/CustomMarkerLabelFormatter.kt#L29) | Breaks as soon as a layer is added or reordered |
| All permissions required at once | [Views\_Navigation.kt](https://github.com/Laul/TrackAid/blob/ac8f97b/app/src/main/java/com/laul/trackaid/views/Views_Navigation.kt) | One denied permission leaves a blank screen |
| Firebase ID token logged | [SignInActivity.kt:70](https://github.com/Laul/TrackAid/blob/2d0e53b/app/src/main/java/com/laul/trackaid/activities/SignInActivity.kt#L70) (`healthcareAI`) | Credential readable in logcat |
| HTTP logging at `Level.BODY` | [SignInActivity.kt:107](https://github.com/Laul/TrackAid/blob/2d0e53b/app/src/main/java/com/laul/trackaid/activities/SignInActivity.kt#L107) (`healthcareAI`) | FHIR health data written to logcat |
| Gradle wrapper ignored | `.gitignore` (`gradlew*`, `gradle/`) | A fresh clone cannot build without a local Gradle install |

## Top 5 for LauWell

Decide the data model and the chart boundary before building any screen.

1. **Domain model first.** `Measurement(metric, instant, value, source)`, with derived `DailyRange` and `TimeSeries`. This is what lets LauWell hold vitals, lab results and inventory alike.
2. **Charts behind your own layer.** A `ChartSpec` that one mapper turns into Vico models. Most of TrackAid's churn came from missing this boundary.
3. **A local database is the source of truth.** Health Connect and FHIR sync into Room. That fixes history, offline use, speed and the refresh bugs together.
4. **Observable, immutable state per screen.** ViewModel + `StateFlow`, no global mutable objects.
5. **Privacy and correctness from the start.** No health data in logs, unit-tested stats, explicit gaps for missing data.

## LauWell skeleton

The skeleton is on branch [`claude/eloquent-dijkstra-ae7o7r`](https://github.com/Laul/LauWell/tree/claude/eloquent-dijkstra-ae7o7r) of LauWell; `main` is untouched. The 16 unit tests in the pure-Kotlin modules pass. The Android modules have not been compiled yet: the build environment had no Android SDK and no access to Google Maven.

| Module | What it holds | Fixes from TrackAid | Verified |
| --- | --- | --- | --- |
| `core:model` | `Metric` enum, `Measurement`, `Source`, `TargetRange` | Typed metrics, no name comparisons | Compiles |
| `core:domain` | Daily ranges and totals, period stats, timeline | Stats bugs, gaps vs zeros, DST | 10 tests pass |
| `core:charts` | `ChartSpec`: what to draw, no chart library | Float timestamps, stacking hacks | 6 tests pass |
| `core:data` | Room database, paged Health Connect source, repository | No cache, truncation, duplicate re-syncs | Not compiled |
| `app` | ViewModels, overview and detail screens, `VicoChart.kt` | Refresh bug, all-or-nothing permissions, logging, backup | Not compiled |

Also added: the Gradle wrapper, a version catalog, a CI workflow (tests, build, lint) and `docs/architecture.md`. The Vico mapper was written against the real Vico 3.3.1 sources. The Android library versions are flagged in the catalog because they could not be checked; the first CI run will confirm them.

- [ ] Open a pull request and let CI build the Android modules
- [ ] Make targets user-configurable instead of hard-coded defaults
- [ ] Add a FHIR source behind the same `HealthDataSource` interface
