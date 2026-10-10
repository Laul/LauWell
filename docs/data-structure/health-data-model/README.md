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

## Codings (LOINC / SNOMED CT) and the combined sheet

`_src/codings.js` holds the standard-terminology codes; `build.js` joins it to `rows.js` and writes two files:

- `Patient Data Model.csv` - **the combined sheet**: one row per data point (81), with category, sub-category,
  shape and description from `rows.js` plus the primary code, UCUM unit, any other codings, a `Verified` summary
  (`yes` / `partial` / `no`) and the coding note. This is the file to import into Notion.
- `Patient Data Codings.csv` - the detail view: one row per coding (a panel and its components, or a variant,
  each get a row). Use it when wiring `MetricDefinition.codings`.

Data points with no entry in `codings.js` get a default `Coding status`: `snomed-later`, `din-atc-later`,
`loinc-doc-later`, `via-catalogue` (inherits from the catalogue entry it points at), `own-catalogue`,
`per-analyte` or `none`. Codes attach to the metric definition, never to individual records. `Verified = yes`
only when the code and display name were confirmed in the LOINC search; treat `no` as a draft.
*Drafted by Claude, Oct 2026 - first pass, in review.*
