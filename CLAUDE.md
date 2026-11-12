# Working notes for Claude Code

- Product: Flipday, native Android, EN + AR (RTL). Design is the source of truth: Confirmed Design page on the design canvas.
- Keep date and reminder logic in `core` (pure Kotlin, JVM tests). Do not import Android there.
- UI is Compose + Material 3. Use theme tokens (`MaterialTheme.colorScheme`, `LocalFlap`), never hard-coded colours, except inside Glance widgets (phase 1).
- Every user-visible string goes in `values/strings.xml` AND `values-ar/strings.xml`. Arabic plurals need all six forms. Use the copy deck on design board 07.
- Numbers shown to users go through `NumeralFormatter`. Flap tiles are LTR even in Arabic and are one accessibility element with a spoken description.
- Reminders: inexact alarms only. Do not declare USE_EXACT_ALARM. SCHEDULE_EXACT_ALARM only with the opt-in "set time" feature.
- Tab roots (Events, Widgets, Settings) have no back arrow; FAB on Events only.
- Before finishing a change: `gradle :core:test :app:testDebugUnitTest :app:lintDebug`.
- The developer's contact details in `DeveloperInfo.kt` were given by the owner for the About screen. Do not copy them elsewhere.
