# Roadmap

**Phase 1 (this commit)** foundation, core logic + tests, Events list, basic Add event, Settings (grouped), About, About the developer, first widget, CI.

**Phase 2** Add/edit flow to the design (choose type, time picker, reminders sheet, edit, delete + undo), event detail screens, notification rationale in context, search + sort/filter, archive and repeat-next-year.

**Phase 3** (in progress) done: per-widget setup, studio (event, style, colours, text), gallery, midnight refresh, sizes. Left: ring, age, date and live-ticker styles built from approved board 06E (WidgetMath tests in core; ticker refreshes every ~15 min via inexact alarm; in-app preview shows the ring, the other new styles preview as flap tiles). Lock-screen widget needs no separate widget: from Android 16 QPR2 every home widget can be placed on the lock screen, so only the 2x2 size has to read at a glance.

**Phase 4** (in progress) done: backup and restore (JSON export/import of events, design 06C.4; auto backup note). Left: exact reminders opt-in, share card, fonts, accessibility pass (200% font scale, TalkBack), tablet layouts, Play listing.

## Periods (end date)
See docs/END_DATE.md. Done: logic, tests, Room v2, design board 06D (approved), UI (editor From/To with validation, list and detail states, Mark as ended, filter chips, EN+AR strings). Next: verify on device, period-specific widget layout, copy-deck rows.
