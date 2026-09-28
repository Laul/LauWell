# LauWell
Ultimate Goal: Building universal medical hub designed to centralize any type of health data, from real-time vitals and lab results to inventory tracking.

## Getting started

1. Open the repository root in Android Studio (Ladybug or newer — needs JDK 17+).
2. Sync Gradle and run the `app` configuration on a device/emulator running Android 8.0 (API 26) or higher.

## Project structure

Single `:app` module, with code separated by package rather than by Gradle module:

```
app/src/main/java/com/laul/lauwell/
├── LauWellApplication.kt
├── MainActivity.kt
├── core/
│   ├── common/       shared types (e.g. AppResult) used across all features
│   ├── data/         shared Room database
│   ├── navigation/   root NavHost + destinations
│   └── ui/theme/     custom theme tokens (colors, typography)
└── feature/
    ├── home/         dashboard across enabled modules
    └── settings/     module enable/disable, thresholds, reminders
```

Room schema exports are written to `app/schemas/`.

How data-type features (vitals, activity and sleep, medication, ostomy, glycemia, …) will be grouped is still being decided — see `PLAN.md` for current status and next steps.
