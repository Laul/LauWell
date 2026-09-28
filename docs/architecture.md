# LauWell architecture

LauWell centralises health data from many sources. The design comes from a review of TrackAid,
an earlier vitals app: its product ideas were right, but one class fetched, aggregated and shaped
data for the chart library, which caused refresh bugs, wrong stats and breakage on every chart
library upgrade.

## Layers

```
Sources (Health Connect, later FHIR and manual entry)      core:data/source
   │  map records to Measurement, paged, with stable ids
   ▼
Room database: the source of truth                          core:data/db
   │  Flow<List<Measurement>>
   ▼
Aggregation and stats: pure Kotlin, unit-tested              core:domain
   │  DaySlot<RangeSummary>, DaySlot<Double>, PeriodStats, TimePoint
   ▼
ChartSpec: what to draw, no chart library                    core:charts
   │
   ▼
ViewModel: one immutable StateFlow per screen                app/ui/*
   │
   ▼
Compose UI + VicoChart.kt: the only file that imports Vico   app/ui/chart
```

| Module | Android? | Depends on |
| --- | --- | --- |
| `core:model` | no | nothing |
| `core:domain` | no | `core:model` |
| `core:charts` | no | `core:domain` |
| `core:data` | yes | `core:domain`, Room, Health Connect |
| `app` | yes | `core:charts`, `core:data`, Compose, Vico |

The three pure-Kotlin modules hold all the logic worth testing and run on a plain JVM.

## Decisions

Each rule fixes a problem found in TrackAid.

1. **One data shape.** Every source maps into `Measurement(metric, start, end, value, source)`.
   Metrics are the typed `Metric` enum; code never compares display names.
2. **Missing data is absent, not zero.** A day without readings is a `DaySlot` with a null value.
   A real 0-step day stays 0.
3. **Time stays `Instant`** until the chart mapper. Chart x values are small doubles (days or
   minutes from an origin), never epoch millis in a Float, which lost about two minutes of precision.
4. **Local days use the zone's calendar**, so DST days are 23 or 25 hours and still aggregate right.
5. **Room is the source of truth.** Screens observe Room; syncs write into it. This gives history
   beyond Health Connect's ~30-day window, offline use and a fast first screen, and removes the
   "need to pull to refresh" bug, since every write re-emits.
6. **Syncs are idempotent and paged.** Sources follow `pageToken` to the end and give every value a
   stable record id; the DAO replaces on the unique `(source, record, metric)` index. One sync runs
   at a time.
7. **Permissions per metric.** Refusing one metric only disables its card.
8. **The chart library sits behind `ChartSpec`.** `VicoChart.kt` is the only file that imports Vico.
   Range bars keep real low and high values; the mapper decides how to draw them.
9. **Tests first where bugs were.** Aggregation, stats and chart specs are covered in
   `core:domain` and `core:charts`.

## Privacy

- No health values, tokens or HTTP bodies in logs.
- `allowBackup="false"` and no cleartext traffic.
- Health Connect access is read-only.
- Secrets (`google-services.json`, keystores) stay out of git.

## Next steps

- [ ] Resolve Android dependency versions on first sync (the catalog marks which were not verified).
- [ ] Settings for personal targets instead of `Metric.defaultTarget`.
- [ ] FHIR source behind `HealthDataSource`, mapping Observations into `Measurement`.
- [ ] Background sync with WorkManager.
- [ ] Repository tests with an in-memory Room database.
- [ ] Screenshot tests for `MetricChart`.
