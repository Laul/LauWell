# Data structure

The reference for how LauWell's health data is organised.

| File | What it is |
|---|---|
| `Patient Data Categories.html` | **Current reference.** Five patient-facing categories, their boundary tests, 31 sub-categories, the data map (81 points) and the shape-to-category bridge. |
| `Patient Data Categories.csv` | The same 81 rows, for import into Notion or a spreadsheet. |
| `Health Data • Inventory-v01/v02` | Earlier passes, kept as snapshots of the thinking. v01 is the raw inventory by data shape and capture method; v02 maps data points onto a 37-category vocabulary that proved too fine-grained. Superseded. |

The decisions these documents encode are summarised in `PLAN.md` → Decisions.
The competitive review behind them is in `docs/research/`.

## Regenerating

`Patient Data Categories.html` and `.csv` are **generated**. Do not edit them by hand —
edit `_src/rows.js` and run:

```
node docs/data-structure/_src/build.js
```

One source, two outputs, so the page and the spreadsheet cannot disagree. The build
validates every row (known category, known shape, non-empty sub-category, no duplicate
names) and refuses to write if anything fails.

- `_src/rows.js` — the 81 data points: `[name, category, sub-category, shape, description]`
- `_src/template.html` — the page, with a `/*__DATA__*/` placeholder for the rows
- `_src/build.js` — the generator (plain Node, no dependencies)
