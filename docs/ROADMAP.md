# Roadmap

**Phase 1 (this commit)** foundation, core logic + tests, Events list, basic Add event, Settings (grouped), About, About the developer, first widget, CI.

**Phase 2** Add/edit flow to the design (choose type, time picker, reminders sheet, edit, delete + undo), event detail screens, notification rationale in context, search + sort/filter, archive and repeat-next-year.

**Phase 3** done: per-widget setup, studio, gallery, midnight refresh, sizes, ring/age/date/live-ticker styles (board 06E). Lock screen needs no separate widget (Android 16 QPR2 shows any widget; 2x2 must read at a glance). The dead "Week starts on" row was removed because nothing in the app uses it; "Widget refresh" now explains when widgets update.

**Phase 4** done in code: backup and restore (JSON events, 06C.4), opt-in exact reminder time (06C.3, `SCHEDULE_EXACT_ALARM` only), share card square/story/wide (06F.1), bundled fonts (IBM Plex Sans, Plex Sans Arabic, Space Grotesk; OFL), accessibility pass (flap digits independent of font scale, heading semantics, one-element tiles), navigation rail from 600dp and list-detail from 840dp (06F.2), privacy policy, licences and rate screens, Play listing draft (`docs/PLAY_LISTING.md`), copy deck (`docs/COPY.md`).

**Not done, needs the owner or a device:** run everything on real phones and a tablet (nothing has been run), TalkBack and 200% font checks, a hosted privacy-policy URL, screenshots and feature graphic, release signing and Play App Signing, name clearance, native Arabic review.

## Periods (end date)
See docs/END_DATE.md. Done: logic, tests, Room v2, design board 06D (approved), UI (editor From/To with validation, list and detail states, Mark as ended, filter chips, EN+AR strings). Next: verify on device; a period-specific widget layout is not designed.
