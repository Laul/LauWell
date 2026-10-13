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
    ├── home/         Today: quick-log row, upcoming events (becomes one of four tabs)
    └── settings/     module enable/disable, thresholds, reminders
```

Room schema exports are written to `app/schemas/`.

Navigation will be four bottom tabs: **Today**, **Tracker** (vitals, symptoms, activity, sleep, mood,
cycle), **Care** (treatments, appliances, ostomy) and **Records** (labs, documents, appointments,
history). Data points are grouped by a category → sub-category → metric hierarchy rather than by
feature module. See `PLAN.md` → Decisions for the rationale and current status.
