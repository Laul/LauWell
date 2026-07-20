# LauWell
Ultimate Goal: Building universal medical hub designed to centralize any type of health data, from real-time vitals and lab results to inventory tracking.

## Getting started

1. Open this folder (`lauwell/`) in Android Studio (Ladybug or newer — needs JDK 17+).
2. Android Studio will offer to regenerate `gradle/wrapper/gradle-wrapper.jar` automatically on first sync (only the `.properties` file is checked in). Accept it, or run `gradle wrapper` manually if you have Gradle installed locally.
3. Sync Gradle and run the `app` configuration on a device/emulator running Android 8.0 (API 26) or higher.

## Project structure

Single `:app` module, features separated by package (not by Gradle module — see `Claude-LauWell/PLAN.md` decisions log for why):

```
app/src/main/java/com/laul/lauwell/
├── core/
│   ├── common/      shared types (e.g. AppResult) used across all features
│   ├── data/         shared Room database
│   ├── navigation/   root NavHost + destinations
│   └── ui/theme/     custom theme tokens (no Material3 — see PLAN.md)
└── feature/
    ├── home/         dashboard across enabled modules
    ├── health/       vitals, activity, and sleep
    ├── medication/
    ├── ostomy/
    ├── glycemia/
    └── settings/      module enable/disable, thresholds, reminders
```

Each `feature/*` package is meant to stay self-contained. As a feature grows past a single placeholder screen, split it internally into `data` (Room entities/DAO), `domain` (repository), and `ui` (screen + ViewModel) sub-packages.

See `../Claude-LauWell/PLAN.md` for current status and next steps.
