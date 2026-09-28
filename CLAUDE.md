# LauWell

Android app: a personal hub to monitor healthcare data — vitals (HR, BP, glucose…),
activity and sleep, ostomy appliances and changes, medications, and more.

## Layout

- `CLAUDE.md` — this file: project context and conventions. Read first.
- `PLAN.md` — milestones and the current task sequence. Update it as tasks complete.
- `app/` — the `:app` Gradle module (standard Android Studio layout: `app/src/main/...`).
  The Gradle root is the repo root (`settings.gradle.kts`, `build.gradle.kts`, `gradle/`).
- `docs/` — documents written by Lauranne.
- `docs/claude/` — everything Claude produces that is not app source: notes, research,
  design docs, generated assets. Write here, not elsewhere in `docs/`.
- `tmp/` — scratch, gitignored. Never put deliverables here.

## Conventions

- Language: English for code, comments, and docs.
- Keep `PLAN.md` current: mark tasks done, add follow-ups discovered along the way.
- Prefer small, reviewable changes; explain trade-offs before large refactors.

## Stack

_To be filled in once the Android project is created (Kotlin, Compose, min SDK, etc.)._
