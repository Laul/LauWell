# Feature modules and on/off toggles

_Status: proposal, not decided. Input for the PLAN.md task "Decide how data-type feature
modules are structured"._

## The question

LauWell will have feature modules (vitals, activity, sleep, medication, ostomy, glycemia, …)
that the user can turn on or off. Does that change how the app is built with Gradle?

## Short answer

No. There are two separate meanings of "optional":

| | Runtime toggle | Build-time exclusion |
|---|---|---|
| What it is | One APK containing every feature; a setting hides or shows each one | Gradle product flavors or dynamic feature modules produce different APKs |
| Changing it | Instant, from Settings | Rebuild, or a Play Store download |
| Good for | A personal app where the choice may change | Separate product editions, or cutting APK size at scale |

LauWell needs the **runtime toggle**. It is already implied by the planned Settings screen
("module enable/disable"). Gradle stays as it is: a single `:app` module.

## Consequences of a disabled feature

Example: vitals on, activity off.

- **Code:** the activity code still ships in the APK but never runs. The size cost is negligible.
- **Database:** the activity tables still exist in the shared Room database (`LauWellDatabase`)
  and stay empty. That is harmless, and it keeps a single schema and a single migration history.
- **Permissions:** this is the real constraint. `AndroidManifest.xml` must declare every
  permission any feature might need (e.g. each Health Connect data type). At runtime, the app
  *requests* only the permissions of enabled features.
- **Background work:** syncs, reminders and WorkManager jobs are scheduled only for enabled
  features. Enabling or disabling a feature must start or cancel its jobs.
- **UI:** navigation (`LauWellDestination`), the Home dashboard and notifications list only
  enabled features.
- **User data:** disabling should **hide** a feature's data, not delete it, so turning the
  feature back on loses nothing. Deleting should be a separate, explicit action in Settings.

## Proposal: a feature descriptor and a registry

Each feature exposes a single descriptor object that lists everything the app needs to know
about it. Nothing outside the feature reaches into its internals.

```kotlin
// core/feature/FeatureDescriptor.kt (sketch, names not final)
interface FeatureDescriptor {
    val id: String                          // stable key, stored in settings
    val label: String
    val destinations: List<LauWellRoute>    // screens this feature adds to navigation
    val dashboardCard: (@Composable () -> Unit)?
    val permissions: Set<String>            // requested only when enabled
    fun onEnabled(context: Context)         // e.g. schedule its workers
    fun onDisabled(context: Context)        // e.g. cancel its workers; keep data
}
```

A central registry lists every descriptor and filters them by the enabled set from settings
(e.g. DataStore):

```kotlin
object FeatureRegistry {
    val all: List<FeatureDescriptor> = listOf(/* VitalsFeature, ActivityFeature, ... */)
    fun enabled(enabledIds: Set<String>) = all.filter { it.id in enabledIds }
}
```

`LauWellNavHost`, `HomeScreen`, the permission flow and the reminder engine then read from
`FeatureRegistry.enabled(...)` instead of hard-coding features. The toggle logic lives in one
place.

### Why this shape

- **One place for the on/off decision:** no `if (activityEnabled)` checks scattered across
  screens.
- **Adding a feature** means writing one descriptor and one line in the registry.
- **Future-proof:** if the project later moves to one Gradle module per feature
  (`:feature:vitals`, …), each descriptor moves with its feature. Only the registry's list
  changes, because it would then gather descriptors from the modules (e.g. via Hilt multibinding).

## When separate Gradle modules would be worth it

Only for build reasons, not for toggling:

- Build times grow enough to hurt, and incremental builds per module would help.
- You want the compiler to *enforce* that features can't depend on each other's internals.

Neither applies today. Dynamic feature modules (download on demand) require Play Store
distribution and add complexity. They are not recommended here.

## Open points to decide

1. **Feature granularity:** is "health" one feature, or are vitals / activity / sleep separate
   toggles? (The example above assumes separate toggles.)
2. **Cross-feature data:** e.g. heart rate recorded during activity. Which feature owns it,
   and what does the other show when the owner is disabled?
3. **Where the enabled set is stored:** DataStore (simple) or a Room table.
4. **Default state on first launch:** all features off, with an onboarding picker, or a
   sensible default set.
