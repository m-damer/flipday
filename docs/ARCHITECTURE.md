# Architecture

```
app (Android)                         core (pure Kotlin)
 ui/  Compose screens, ViewModels      Event, EventType, ReminderRule
 data/ Room entity+DAO, DataStore      EventCalculator  (days, age, progress)
 reminders/ AlarmManager scheduler     ReminderPlanner  (next triggers)
 widget/ Glance widget                 ArchivePolicy, NumeralFormatter
```
- **Single source of truth:** Room `events` table; the repository maps entities to the `core` `Event` model.
- **DI:** a small manual `AppContainer` (see `FlipdayApp.kt`). Add Hilt only if the graph outgrows it.
- **Reminders:** one inexact alarm per event for the next trigger; the receiver posts the notification and schedules the following one. Boot, time-zone and time changes reschedule everything (`ReschedulerReceiver`).
- **Time:** days are calendar days (`LocalDate`), not 24-hour blocks.
- **Backup:** Android Auto Backup for the database and DataStore. Widgets are not backed up.
- **Navigation:** three tabs plus child routes; see design board 08.
