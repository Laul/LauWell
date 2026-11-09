# LauWell database tables

*Drafted with Claude, 2026-11-07, from `schema-er.mmd` (2026-11-01) and the decisions taken since: Postgres on Neon as the
single source of truth (2026-11-04), our own user key (2026-11-06), and the Shared foundation review (2026-11-07, see
`../shared-foundation.md`). Status: proposal, nothing built (Planning: M9 and "Shared foundation"). This file is the
editable reference: change a table here first, then `schema-er.mmd` and the rendered page.*

How to read the tables:

- **Key**: `PK` primary key, `FK → table` foreign key (Postgres refuses a row pointing at nothing), `UQ` unique.
- **Where**: `PG` Postgres only, `Room` phone only, `Both` the same table in both.
- In Postgres, every table also has `user_id FK → users`, filled by the sync API from the verified sign-in, and
  row-level security limits each person to their own rows. Room has no `user_id`: the phone holds one person's data,
  and signing out clears it. The tables below leave `user_id` out, except where it is part of the key.
- Ids are UUIDs made on the phone, so anything can be created offline and synced later.
- Metric definitions (unit, aggregation, chart kind, category) stay in code. The database stores only the metric's id
  as text (`heart_rate`, `mood`). There is no metrics table.

## 1. Who you are

### `users` (PG)

| Column | Type | Key | Meaning |
|---|---|---|---|
| id | uuid | PK | our own id for a person, referenced everywhere |
| created_at | timestamptz | | |

### `auth_identity` (PG)

| Column | Type | Key | Meaning |
|---|---|---|---|
| provider | text | PK | `firebase` today |
| subject | text | PK | the Firebase uid |
| user_id | uuid | FK → users | |

Changing the sign-in provider later only touches this table.

## 2. Sensor data (from Health Connect)

### `reading` (Both: all history in PG, the last month in Room)

One row per quantity per timestamp: every heart rate sample, each half of a blood pressure.

| Column | Type | Key | Meaning |
|---|---|---|---|
| user_id | uuid | PK, FK → users | (PG only) |
| metric_id | text | PK | `heart_rate`, `blood_pressure` |
| quantity_key | text | PK | `value`, `systolic`, `diastolic` |
| time_utc | timestamptz | PK | |
| origin_id | int | PK, FK → origin | which app and device wrote it |
| zone_offset_s | int | | to place it on the right local day |
| value | float | | |
| recording_method | text | | automatic, active, manual, unknown |
| source_record_id | text | | Health Connect's id; finds a deletion made in the source app |
| deleted_at | timestamptz | | deleted in the source app; what to do with it is still to decide |

Syncing twice is safe: the key makes a second copy impossible. Two apps reporting the same thing are both kept;
which one a chart shows is a display rule.

### `daily_summary` (Both)

Health Connect's own figure per day, kept as received (not rebuilt from readings, which would count two apps'
steps twice).

| Column | Type | Key | Meaning |
|---|---|---|---|
| metric_id | text | PK | |
| day | date | PK | local day |
| quantity_key | text | PK | |
| aggregation | text | | sum, average, min-average-max, latest |
| value | float | | |
| min, max | float | | for a range |
| count | int | | |
| last_at | timestamptz | | for a latest value |
| origins | text | | the writing apps |

### `origin` (Both)

| Column | Type | Key | Meaning |
|---|---|---|---|
| id | int | PK | |
| app_package | text | | `com.garmin…`, `com.sec.android.app.shealth` |
| manufacturer, model | text | | when the app says |
| device_type | text | | watch, phone, ring… |

## 3. What you log: acts

Every act, of every kind, is one row in `record`. History, generic links and export read only this table. A kind
with fields of its own adds a **detail table** (section 5), one row per record, sharing the record's id.

### `record` (Both: all history in PG and in Room)

| Column | Type | Key | Meaning |
|---|---|---|---|
| id | uuid | PK | |
| metric_id | text | | `mood`, `medication_dose_taken`, `ostomy_appliance_change` |
| shape | text | | Measurement, Event, Interval, Standing fact, Document, Derived |
| time_utc | timestamptz | | when it happened, or started |
| end_time_utc | timestamptz | | Interval only, and **optional**: an ongoing course has none |
| zone_offset_s | int | | |
| source | text | | manual, device, lab, import |
| recorded_at | timestamptz | | when it was entered |
| edited_at | timestamptz | | last change |
| deleted_at | timestamptz | | an act can be deleted; the row stays as a tombstone so the deletion syncs |
| note | text | | the entry's own comment |
| schema_version | int | | bumped on a breaking change |

### `record_value` (Both)

The numbers of a simple measurement (mood 7, fatigue 4), which needs no detail table.

| Column | Type | Key | Meaning |
|---|---|---|---|
| record_id | uuid | PK, FK → record | |
| quantity_key | text | PK | `value`, or the part's name |
| value | float | | |

### `record_link` (Both): generic links, for context

| Column | Type | Key | Meaning |
|---|---|---|---|
| from_id | uuid | PK, FK → record | the record that points |
| to_id | uuid | PK, FK → record | indexed: "what is linked to this encounter?" |
| relation | text | PK | `recorded_during`, `about` |

Stored once, on the record that points. Links that *define* a record (a dose → its medication) are not here: they are
named columns on the detail tables.

### `file` (PG)

| Column | Type | Key | Meaning |
|---|---|---|---|
| id | uuid | PK | |
| record_id | uuid | FK → record | optional: the entry the file belongs to |
| storage_path | text | | the object in Cloud Storage, which holds the contents |
| mime_type | text | | |
| size_bytes | int | | |
| uploaded_at | timestamptz | | |

## 4. Things: the catalogues

Entered once, reused by many acts. Two families, one table per kind. Every thing table, in both families, starts with:

| Column | Type | Key | Meaning |
|---|---|---|---|
| id | uuid | PK | |
| name | text | | what lists and pickers show |
| note | text | | |
| created_at, edited_at | timestamptz | | |
| not_used_since | timestamptz | | the **"Not used anymore"** tag, as the moment it stopped; screens show "since Mon 3 Nov". Still listed, no longer offered when logging; empty while in use |
| deleted_at | timestamptz | | only for a thing created by mistake that nothing points to (P3 task); otherwise never deleted |

### Products: what I take or use

Family columns, on each Products table:

| Column | Type | Meaning |
|---|---|---|
| brand | text | brand or maker |
| reference_code | text | DIN, product code |
| form | text | tablet, drops, two-piece pouch… |

| Table | Own columns (first ideas, settled in each feature's task) | Comes with task |
|---|---|---|
| `medication` | strength, strength_unit, active_ingredient | Medication: catalogue + course |
| `ostomy_product` | product_type (pouch, barrier, accessory), flange_size_mm, convexity, barrier_type | Ostomy: product catalogue… |
| `device` | device_type (glucometer…), model | the glucose task |

`medication.not_used_since` is set automatically when its last course ends and cleared when a new one starts; the
patient can also stop a medication by hand, which ends its open course on the chosen date.

### Resources: who and where care comes from

Family columns, on each Resources table:

| Column | Type | Meaning |
|---|---|---|
| type | text | GP, nurse… / clinic, hospital, lab… / pharmacy, ostomy store… |
| address | text | |
| phone | text | |
| website | text | |
| contact_person | text | |

| Table | Own columns | Comes with task |
|---|---|---|
| `care_provider` | specialty, works_at_id FK → care_facility | Care providers, facilities and suppliers catalogues |
| `care_facility` (labs included) | department | same |
| `supplier` | supplies (medication, ostomy, devices) | same |

## 5. Detail tables: an act's own fields and its named links

One row per record, `record_id` is both its primary key and `FK → record`. The medication set below shows the
pattern; each feature's task settles its own.

| Table | Act (data map row) | Named links (FK) | Own columns |
|---|---|---|---|
| `medication_course` | Medication Course (Interval) | medication_id → medication, prescription_id → record (optional) | dose_amount, dose_unit, times_per_day, schedule (local times), end_reason |
| `dose_taken` | Medication Dose Taken or Skipped (Event) | course_id → record, medication_id → medication | taken_or_skipped, amount, unit, end_reason (why a dose was skipped) |
| `dispense` | Medication Dispense (Event) | supplier_id → supplier | total_cost |
| `dispense_item` | one drug in a dispense | dispense_id → record, medication_id → medication | quantity, unit |
| `stock_count` | Medication Stock Count (Measurement) | medication_id → medication | count |
| `appliance_change` | Ostomy Appliance Change (Event) | product_id → ostomy_product | reason |
| `appointment` | Appointment (Event) | provider_id → care_provider, facility_id → care_facility | questions to ask, prep |

Not stored, computed when shown: *Medication Dose Scheduled* (from the course's schedule), *Medication Stock* (last
count + picked up − taken), *Ostomy Wear Time* (between two appliance changes).

## 6. Phone only

| Table | Meaning |
|---|---|
| `sync_state` | Health Connect's changes token and the upload position per metric |
| `outbox` | entries written on the phone and not yet sent to Postgres |

## Settled in review (2026-11-07)

- `not_used_since` is a timestamp (the moment it stopped); "since Mon 3 Nov" is computed when shown.
- A pharmacy pickup usually holds several drugs, so `dispense_item` stays: one row per drug, with real foreign keys.
- What the phone keeps (Decisions Tracker: *What the phone keeps*): every entry and every thing, all history; sensor
  readings for the last month, older ones fetched from Postgres when online.

## Left to later tasks

- **Prescription.** It is a Document today (a photo or PDF); whether it also gets typed fields (prescriber, list of
  drugs) is the task *Medication: prescription with items*.

## Agreed 2026-11-08 (FHIR review), not all in the tables above yet

- **One word for "why it ended or did not happen": `end_reason`.** Used on `medication_course` (why it stopped) and
  `dose_taken` (why a dose was skipped); any later table with the same need reuses the name. `appliance_change.reason`
  is a different thing (the cause of the change) and keeps its name.
- **`schedule` becomes a structured, Timing-style block** (frequency, period, days of week, times of day, as-needed)
  when the medication task settles it, instead of a text column. The same block is meant for a prescription item and
  a course.
- **Catalogue tables get `code` and `code_system`** next to `reference_code` (DIN, ATC, GTIN…), since a thing's code
  belongs to the row, unlike a metric's, which belongs to its definition.
- **Deferred to M6, on purpose:** a metric reference table and a unit stored with each value, and the lab tables
  (requisition → report → panel → results). Planning: *Metric reference table + unit stored with every value*.
