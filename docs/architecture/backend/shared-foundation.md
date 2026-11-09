# Shared foundation: things, acts, and links between records

*Drafted with Claude, 2026-11-07; revised the same day after review (two families of things, "Not used anymore").
Status: proposal under review, no code yet (Planning: "Shared foundation: things vs. acts, and links between records", M4). Builds on the Decisions Tracker entries *Storage shapes*, *Catalogue + occurrence*,
*Relationships between records* and *Persistence: Room on the phone, Postgres as the single source of truth*.*

## The question

Medication, ostomy, appointments and the healthcare resources all log the same two kinds of data:

- **Things** (catalogue), entered once and reused: Ramipril 5 mg, a SenSura Mio pouch, the pharmacy on the corner.
- **Acts** (occurrences), logged each time something happens, usually pointing at a thing: took Ramipril at 8:02,
  changed the pouch on 3 Nov, picked up at the pharmacy.

This note fixes, once for all of them: how a thing is stored, how an act is stored, how one record points at another,
and how a missing thing is added from inside a logging sheet. Each feature then only adds its own fields.

The tables are the same on the phone (Room) and in Postgres (Neon), so nothing is designed twice and the sync copies
rows one-to-one.

## 1. Things: two families, one table per kind

Things come in two families that mean different things and share different columns (agreed 2026-11-07):

| | **Products**: what I take or use | **Resources**: who and where care comes from |
|---|---|---|
| Kinds (one table each) | `medication`, `ostomy_product`, `device` (glucometer…) | `care_provider`, `care_facility` (clinic, hospital, **lab**), `supplier` (pharmacy, ostomy store) |
| Family columns | brand or maker, reference code (DIN, product code), form | type, address, phone, website, contact person |
| Kind's own columns | medication: strength, active ingredient; pouch: flange size (mm), convexity, barrier type | provider: specialty, works at; facility: department; supplier: what it supplies |
| Acts point to them as | *what* was taken or used | *who* or *where* |

Every table, in both families, also starts with the same few columns:

| Column | Meaning |
|---|---|
| `id` | made on the phone (UUID), so a thing can be created offline and synced later |
| `name` | what the list shows ("Ramipril 5 mg", "Pharmaprix Mont-Royal") |
| `note` | free comment, optional |
| `created_at`, `edited_at` | when it was entered / last changed |
| `not_used_since` | timestamp of the "Not used anymore" tag (section 4); empty while in use |

**Why not one shared table with a free-form "attributes" blob?** The decision *Catalogue + occurrence* exists so that
"did the 25 mm convex barrier give longer wear time than the flat one?" can be answered. That needs real columns
(a number for the flange size, a fixed list for convexity) that queries and statistics can rely on. A blob would
bring back the free text the decision rules out.

## 2. Acts: one common table, plus a detail table per kind

Every act, whatever its kind, gets one row in a common `record` table. That is the existing proposed schema
(`schema-er.mmd`) and the fields the app already has on every record (`HealthRecord`).

| Column | Meaning |
|---|---|
| `id` | made on the phone (UUID) |
| `metric_id` | which data point it is (`mood`, `medication_dose_taken`, `ostomy_appliance_change`) |
| `shape` | Measurement, Event, Interval… (from the data map) |
| `time_utc`, `end_time_utc` | when it happened; the end only for an Interval |
| `zone_offset_s` | to place it on the right local day |
| `source` | manual, device, lab, import |
| `recorded_at`, `edited_at`, `deleted_at` | as for things |
| `note` | the entry's own comment |
| `schema_version` | bumped on a breaking change |

Then each kind with its own fields has a **detail table**, one row per record, sharing the record's `id`:

```
record (common)            dose_taken (detail)
id ───────────────────────► record_id
metric_id                   medication_id ──► medication.id   (named link, a real foreign key)
time_utc …                  amount, unit, taken_or_skipped
```

- A simple measurement (mood, a 1-10 rating) has no detail table: its numbers go in the existing `record_value`
  table (`record_id`, `quantity_key`, `value`).
- The **History timeline, generic links and export** read only `record`: one query, every kind.
- A **feature screen** reads `record` joined with its detail table.

**Why not one table per act?** History, links and export would have to query a dozen tables and stay in step every
time a kind is added. **Why not everything in `record`?** The defining links (a dose → its medication) must be real
foreign keys, and those need their own columns.

## 3. Links between records: two kinds, as decided

Unchanged from *Relationships between records*; only the storage changes now that Firestore goes.

- **Named link, when it defines the record**, and logic or statistics depend on it: a column with a foreign key on
  the detail table (`dose_taken.medication_id`, `dispense.supplier_id`, `appliance_change.product_id`).
  Postgres refuses a dose pointing at a medication that does not exist.
- **Generic link, for context**: one `record_link` table (`from_id`, `to_id`, `relation`), e.g. a blood pressure
  reading *recorded during* an encounter, a photo *about* a skin rating. It is stored once, from the record that
  points, and an index on `to_id` answers "what is linked to this encounter?" The relation list (`recorded_during`,
  `about`) is the existing `LinkRelation`.

## 4. "Not used anymore": things are never deleted

A thing that any entry points to is **never deleted**: a medication taken then stopped still means something, for
example when symptoms line up with it. Instead it gets the tag **"Not used anymore"**, the same words everywhere
(medications included). A thing with the tag:

- stays in its list (Care, Records), shown with the tag and the date;
- is no longer offered in the pickers that log an act (a dose, a pouch change, a pickup);
- can be switched back to in use at any time.

**Medication** gets the tag from its courses as well as by hand:

- A course has a start date and an **optional** end date: a chronic treatment has none.
- The tag is set automatically when the last course ends, and cleared when a new course starts.
- The patient can also add, edit or **stop a medication early**. Stopping ends its open course on the chosen date,
  which sets the tag.
- The *start a course* picker still offers medications not used anymore, so restarting a drug keeps one history
  instead of a duplicate.

The one exception, a thing created by mistake that nothing points to yet, can be deleted. That is its own task on
the board (*Delete a thing created by mistake*, P3).

## 5. "Add it on the spot" in a logging sheet

Every field that picks a thing (pharmacy, medication, pouch) uses one shared picker:

1. It lists the things of that kind, most recently used first, with a search field. Things not used anymore are hidden.
2. The last row is **"+ New pharmacy…"**. It opens a short form inside the sheet, with only the name required.
3. Saving creates the thing and selects it, and the user is back in the logging sheet with everything typed so far
   kept. The rest of the thing's details can be filled in later from Care or Records.

The thing and the act are saved separately. If the user cancels the act, the new pharmacy stays (it is still real).

## 6. On the phone

- **Room stops being a throw-away cache.** Today a schema change drops the database, because everything in it can
  be rebuilt from Health Connect. Once it holds things you typed, every schema change needs a written migration and a
  migration test. The Health Connect cache tables keep their drop-and-rebuild rule; only they may be cleared.
- **No `user_id` in Room.** The phone holds one person's data; sign-out clears the user tables. The cloud function
  adds `user_id` (from the verified Firebase token) when it writes to Postgres, and row-level security does the rest.
- **Sync columns are there from day one** (`edited_at`, `deleted_at`, ids made on the phone), so the sync task
  (Room ↔ Postgres) adds no column later.

## 7. What this task builds, and what it leaves

**Builds:** the pattern, no concrete thing table.
- The Room tables for acts: `record`, `record_value`, `record_link`.
- The shared columns of the two thing families, and of every thing table, as code each kind's table reuses.
- The DAOs, Room migration tests, and the database moving from "drop on change" to real migrations.
- The shared picker with "add it on the spot" and the "Not used anymore" rule, in `core.ui.components`, with previews.
- `schema-er.mmd` updated to match (both families, detail tables, foreign keys), as the architecture check asks.

**Leaves to other tasks:** each concrete table arrives with the feature that first needs it.
- `medication` (Products): *Medication: catalogue + course*, which proves the Products pattern.
- `care_provider`, `care_facility`, `supplier` (Resources): *Care provider + care facility catalogues*.
- `ostomy_product`, `device`: their ostomy and glucose tasks.
- Postgres tables and security: *Real database (Neon): provision, schema and security* (written from this note).
- Moving Mood from Firestore to Room: *Move Mood off Firestore…* (needs the sync).

## Settled in review (2026-11-07)

1. One table per kind of thing, in two families (Products, Resources); labs are care facilities.
2. The foundation builds the pattern only; each concrete table comes with its feature.
3. Never delete a thing an entry points to; tag it "Not used anymore" (same words everywhere). A medication can be
   stopped by hand, and its course end date is optional.
