# LauWell — Plan

## Milestones

1. Project setup — repo, Android Studio skeleton, CLAUDE.md filled in
2. Data model — vitals, activity/sleep, ostomy, medications
3. Local storage
4. First screens
5. Data sources (Health Connect, Garmin, …)

## Current tasks

- [ ] Fill in the Stack section of CLAUDE.md
- [ ] Decide how data-type feature modules (health, medication, ostomy, glycemia, ...) are structured for modularity/scalability, then define the data model (see `claude/` for research notes)

## Done

- [x] Create the Android project in `app/`
- [x] Fix Gradle build (missing wrapper, gradle.properties, JVM toolchain mismatch) — `./gradlew :app:assembleDebug` succeeds

