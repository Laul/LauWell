# LauWell
Ultimate Goal: Building universal medical hub designed to centralize any type of health data, from real-time vitals and lab results to inventory tracking.

## Status

Architecture skeleton. Heart rate, glucose and steps flow from Health Connect into a local
database and onto an overview and a detail screen per metric. See
[docs/architecture.md](docs/architecture.md) for the layers and the reasons behind them.

## Build

Requires JDK 17 or newer and the Android SDK.

```
./gradlew :core:domain:test :core:charts:test   # pure-Kotlin logic
./gradlew :app:assembleDebug                     # the app
```
