# Flipday · فليب داي

A native Android app to count down to dates and count up from them, with home-screen widgets. English and Arabic (RTL).

> **Build status: not yet compiled.** This first commit was written without access to the Android SDK or Gradle repositories. The first CI run on GitHub is the first time any of it is built and tested. Expect a few compile or lint fixes before it is green.

## Design
The approved design lives on the Claude Design canvas (Confirmed Design page): screens, widgets, states, flows, logic, design system and tokens.
Tokens are in `design/tokens.json` and mirrored in `app/.../ui/theme`. See `docs/DESIGN.md`.

## Stack
Kotlin 2.0, Jetpack Compose + Material 3, Navigation Compose, Room, DataStore, Glance (widgets), AlarmManager (inexact reminders). Min SDK 26, target SDK 35. No network, no accounts, no analytics.

## Modules
- `core`: pure Kotlin (no Android). Date maths, reminder planning, archive policy, numeral formatting. Unit-tested.
- `app`: Android UI, storage, reminders, widget.

## Build
Needs JDK 17 and the Android SDK.
```
gradle wrapper --gradle-version 8.14.3   # once: creates gradlew and the wrapper jar
./gradlew :core:test
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
CI (`.github/workflows/ci.yml`) runs the same on every push and pull request.

## Status
See `docs/ROADMAP.md`. Phase 1 (this commit): foundation, Events list, Add event (basic), Settings, About, About the developer, first widget.

## Open items
- Fonts (IBM Plex Sans, IBM Plex Sans Arabic, Space Grotesk) are not bundled yet; the system font is used.
- Dependency versions in `gradle/libs.versions.toml` were not checked against the latest releases.
- No licence file yet. Choose one before making the repository public.
- The Arabic copy was written by a non-native editor. Have a native speaker review `values-ar/strings.xml`.
