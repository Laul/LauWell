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
│   ├── auth/         Google sign-in (Credential Manager → Firebase Auth), sign-in screen
│   ├── catalog/      category → sub-category → metric registry and visibility (toggles)
│   ├── common/       shared types (e.g. AppResult) used across all features
│   ├── data/         shared Room database
│   ├── di/           AppContainer (manual dependency injection) + ViewModel factory helper
│   ├── navigation/   root NavHost + destinations
│   └── ui/theme/     custom theme tokens (colors, typography)
└── feature/
    ├── home/         Today: quick log, pinned, medications, appointments (one of four tabs)
    └── settings/     sign-out today; visibility toggles, thresholds, reminders later
```

Room schema exports are written to `app/schemas/`.

Navigation will be four bottom tabs: **Today** (quick log, pinned metrics, medications due,
symptom check, upcoming appointments),
**Tracker** (vitals, symptoms, activity, sleep, mood, cycle), **Care** (treatments, appliances, ostomy)
and **Records** (labs, documents, appointments, history). Data points are grouped by a
category → sub-category → metric hierarchy rather than by feature module. See `PLAN.md` → Decisions
for the rationale and current status.

## Design

The visual design is locked. The exports (screens in light and dark, token sheet, icon list) live in
`docs/design/`, which is kept out of git. In short: color stands
for the data category, near-black ink is the primary, Bricolage Grotesque headlines with IBM Plex Sans
body and IBM Plex Mono numbers, Material Symbols Outlined icons. Summary in `PLAN.md` → Decisions →
*Visual design*.
