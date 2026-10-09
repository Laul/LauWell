# Feature modules and on/off toggles

*Drafted with Claude, 2026-09-30; rewritten 2026-10-08 after the data-categories work. Status:
adopted (PLAN.md → M0). Replaces the earlier "feature descriptor + registry" proposal.*

## The question

LauWell holds many kinds of health data (vitals, activity, sleep, medication, ostomy, glucose, …)
and the user should be able to hide what they don't track. Does that change how the app is built
with Gradle, and what is the unit that gets switched on and off?

## Runtime toggle, not build-time exclusion

There are two separate meanings of "optional":

| | Runtime toggle | Build-time exclusion |
|---|---|---|
| What it is | One APK containing everything; a setting hides or shows each part | Gradle product flavors or dynamic feature modules produce different APKs |
| Changing it | Instant, from Settings | Rebuild, or a Play Store download |
| Good for | A personal app where the choice may change | Separate product editions, or cutting APK size at scale |

LauWell needs the **runtime toggle**. Gradle stays as it is: a single `:app` module. Separate
Gradle modules would only be worth it for build reasons (slow incremental builds, compiler-enforced
boundaries); neither applies today. Dynamic feature modules need Play Store distribution and are
not recommended.

## The toggle unit is the data hierarchy itself

The first version of this document proposed a `FeatureDescriptor` per feature, each bringing its
own screens. Two later decisions made that the wrong shape (PLAN.md → Decisions):

- Adding a tracker is a **registry entry, never a new screen or navigation destination**.
- Every data point already has a home in a three-level hierarchy
  (`docs/data-structure/Patient Data Categories`):

| Level | Count | Example | In code |
|---|---|---|---|
| Category | 5 | Vitals | `Category` enum |
| Sub-category | 35 | Vitals → Cardiovascular | `SubCategory` enum, keyed by category **and** name ("Respiratory" exists under both Vitals and Symptoms) |
| Data point | 81 | Heart rate | `MetricDefinition` |

So there is no separate "feature" concept to invent. **Any level of that hierarchy can be switched
off**: a whole category, a sub-category, or a single metric. The hierarchy also answers the old
"who owns heart rate?" question for free: each metric has exactly one sub-category.

### What gets stored: the switched-off set

The app stores the set of things the user has turned **off**, not what is on. Each entry is a
stable key at one of the three levels:

```kotlin
// core/catalog (sketch, names not final)
sealed interface ToggleKey {
    data class OfCategory(val category: Category) : ToggleKey
    data class OfSubCategory(val subCategory: SubCategory) : ToggleKey
    data class OfMetric(val metricId: String) : ToggleKey
}

fun isVisible(metric: MetricDefinition, off: Set<ToggleKey>): Boolean =
    ToggleKey.OfMetric(metric.id) !in off &&
    ToggleKey.OfSubCategory(metric.subCategory) !in off &&
    ToggleKey.OfCategory(metric.subCategory.category) !in off
```

Why "off" rather than "on":

- An empty set means **everything visible**, which is the MVP behaviour, with nothing to seed on
  first launch.
- A metric added in a later release appears without a migration of the stored set.
- Whatever the Settings screen ends up looking like (per category, per sub-category, per metric),
  it only adds and removes keys. Nothing else changes.

`isVisible` is **the only place** that decides visibility. Home, the category screens, the
permission flow and background sync all ask it (through a repository exposing
`Flow<Set<ToggleKey>>`); none of them checks a toggle directly.

### MVP scope

- No selection screen after sign-in. Everything is visible.
- The switched-off set sits behind an interface; the first implementation returns an empty set.
  The DataStore-backed implementation lands together with the Settings toggles, which are backlog.
- When the Settings toggles do land, Settings is their primary home.

## Consequences of a hidden part

Example: Lifestyle → Activity switched off.

- **Code:** still ships in the APK, never runs. Negligible size cost.
- **Data:** hidden, **never deleted**. Turning it back on loses nothing. Deleting data is a
  separate, explicit action in Settings. Storage (Firestore `users/{uid}/…`, Room tables) keeps
  one schema and one migration history either way.
- **Permissions:** `AndroidManifest.xml` declares every permission any metric might need. At
  runtime a permission is requested only for a visible metric, and only when it is first needed
  (e.g. the first heart-rate read), never all at once. Partial grants are fine. Permissions and
  data sources are declared on the **metric definition**, so they follow any toggle level.
- **Background work:** syncs and reminders are scheduled only for visible metrics. A change in the
  switched-off set starts or cancels the matching jobs.
- **UI:** navigation is generic — Home, `category/{id}`, `metric/{id}`, Settings — and lists only
  visible metrics. Adding a metric never touches the nav host.

## Deferred: switching off everything related to one condition

Some data is related across categories. Ostomy is the clearest case: output volume is in Vitals,
the change routine in Lifestyle, peristomal skin in Symptoms, products in Treatments & appliances.
The ideal behaviour is one switch that turns off *everything related to ostomy*, wherever it is
filed, and the same question applies to other conditions.

Not needed for the MVP. The likely shape is a tag (or "topic") on the metric definition plus a
fourth `ToggleKey` variant, which fits the switched-off set without reworking it. To be decided
when it is needed.
