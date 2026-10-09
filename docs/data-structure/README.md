# Data structure

The reference for how LauWell's health data is organised.

| File | What it is |
|---|---|
| `Patient Data Categories.html` | **Current reference.** Five patient-facing categories, their boundary tests, 35 sub-categories, the data map (81 points) and the shape-to-category bridge. |
| `Patient Data Categories.csv` | The same 81 rows, for import into Notion or a spreadsheet. |
| `Patient Data Codings.csv` | Standard terminology codes (LOINC, later SNOMED CT) for the same data points. First pass, in review — see *Codings* below. |
| `Health Data • Inventory-v01/v02` | Earlier passes, kept as snapshots of the thinking. v01 is the raw inventory by data shape and capture method; v02 maps data points onto a 37-category vocabulary that proved too fine-grained. Superseded. |

The decisions these documents encode are summarised in `PLAN.md` → Decisions.
The competitive review behind them is in `docs/research/`.

## Regenerating

`Patient Data Categories.html` / `.csv` and `Patient Data Codings.csv` are **generated**. Do not
edit them by hand — edit `_src/rows.js` (data points) or `_src/codings.js` (codes) and run:

```
node docs/data-structure/_src/build.js
```

One source, two outputs, so the page and the spreadsheet cannot disagree. The build
validates every row (known category, known shape, non-empty sub-category, no duplicate
names) and refuses to write if anything fails.

- `_src/rows.js` — the 81 data points: `[name, category, sub-category, shape, description]`
- `_src/template.html` — the page, with a `/*__DATA__*/` placeholder for the rows
- `_src/codings.js` — the codings, one line per code, plus default-status rules for uncoded points
- `_src/build.js` — the generator (plain Node, no dependencies); also validates the codings
  (known names, roles, systems, well-formed LOINC codes, exactly one primary per coded point)

## Codings (LOINC / SNOMED CT)

*Drafted by Claude (Claude Desktop session), Oct 2026. Status: first pass, in review.*

**Why.** Our category / sub-category / metric names are patient-facing and our own. Attaching a
standard code to each metric definition makes the data interoperable (FHIR export, sharing with a
clinician, importing lab results) and gives us structured data we can act on unambiguously:
"heart rate" from a watch, a cuff or a lab report is recognisably the same thing (LOINC `8867-4`),
with a standard UCUM unit. It is a layer on top, not a replacement: app logic keys on our own
metric IDs and never depends on a code being present.

`Patient Data Codings.csv` is generated from `_src/codings.js` by the same `build.js` run. It maps data points
to standard terminology codes (one row per coding) and gives every other data point a default status
(`snomed-later`, `din-atc-later`, `own-catalogue`, `per-analyte`, `none`). Codes attach to the metric
definition, never to individual records — except lab results (`per-analyte`), where each record carries the
code of its own analyte. The `Verified` column is `yes` only when the code and its display name were
confirmed in the LOINC search; treat `no` rows as drafts.

