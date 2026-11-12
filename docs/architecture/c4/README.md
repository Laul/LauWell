# LauWell C4 diagrams (levels 1 to 3)

*Drafted with Claude, 2026-10-30, from the code at commit `f374a25`, the Notion Home page and its subpages,
`PLAN.md` and the Notion Planning and Decisions databases. Updated with Claude, 2026-11-06, for the storage
decisions of 2026-11-04 (Postgres on Neon as the single source of truth, Firestore removed from the app on 2026-11-11; see the
Notion page *Long-term data store*). Status: draft for review against the vision.
Level 4 stays in the Notion sequence diagrams.*

Solid = built. Dashed = planned (a Notion Planning row exists, nothing built). Amber = built and being removed (M9).
Sources are Mermaid, so they diff in git. The two HTML pages are the same diagrams rendered, with the text around
them; they are rebuilt from the `.mmd` files, so change the `.mmd` first.

| Level | File | Shows |
|---|---|---|
| 1 Context | `c4-1-context.mmd` | The patient, LauWell and what it touches, incl. the planned Postgres store |
| 2 Containers | `c4-2-containers.mmd` | App, Room, DataStore, Health Connect, Firebase, Neon Postgres and the sync API |
| 3 Components, app | `c4-3-components-app-data.mmd` | How data reaches a screen (sync, read path, entries) and the planned sync to Postgres |
| 3 Components, app | table below | Which screen calls which service |
| 3 Components, Firebase | `c4-3-components-firebase.mmd` | Auth, the sync API (Cloud Function, Secret Manager), Storage (planned), the old Firestore data kept as a backup |
| 3 Components, Postgres | `c4-3-components-postgres.mmd` | Sync API (push, pull), user mapping, tables, row-level security; planned: readings, backup |

Rendered page: `lauwell-c4.html`.

## Which screen calls which service (level 3, app)

Catalog, Record model and the UI kit are used by every component and are left out.

| Screen | Summaries | HealthSync | BackgroundSync | HealthDataSource | SummaryStore | RecordRepository | AuthRepository | Preferences | ToggleRepository | Chart layer | EntrySync |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Sign-in | | | | | | | x | | | | |
| Today | x | x | | | | | | x | x | | |
| Tracker list | x | x | | | | | | x | x | | |
| Metric detail | x (DayHistory) | x | | x | x | x | | | | x | |
| Log entry sheet | | | | | | x | | | | | |
| Settings | | | | x | x | | x | x | x | | x |
| Health Connect grant | | | x | x | | | | x | x | | |
| Nav host | | | | | | | | x (onboarding) | | | |

`BackgroundSync` itself calls `HealthSync`, `HealthDataSource`, `ToggleRepository` and the auth session. Settings reads
`EntrySync`'s status (waiting, refused, last sync) and clears the local entries on sign-out.

In M9 `RecordRepository` gets a Room-backed implementation; the outbox and its sync job to Postgres (`EntrySync`,
`EntrySyncWorker`, requested by `LauWellApplication` at each foreground) are built. Its callers do not change, so this
table does not either.
