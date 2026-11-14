# Design audit: code vs Confirmed Design (Teal)

Checked on 2026-11-14 by reading the code against boards CD-04, 06 coverage, 08 IA and the Teal widget and screen boards. Nothing was run on a device, so visual findings come from the code, not from screenshots of the app.

## Verified OK
- Colour tokens in `Color.kt` match CD-04 (light and dark, flap tile and digit).
- Fonts, shapes (12/10/14/28dp), three tabs, no back arrow on tab roots, FAB on Events only.

## Gaps: widgets (largest)
Design: 8 widget families, each at Strip 4x1, Small 2x2, Medium 4x2, Large 4x4, EN and AR.
Code: one provider, three responsive sizes, six styles, only two of which exist in the design.

| Design family | Code today |
|---|---|
| Big number (strip, small, medium, large with progress bar and event list) | Flap tiles only: no progress bar, no strip, no list in large |
| Age counter (years/months/days tiles, % to next birthday, days lived, weeks) | Years only plus "turns" line |
| Count-up (milestone card: 365 / 730 / 1,000, next milestone) | Plain days, no milestones |
| Agenda (upcoming list, "+4 more") | Missing |
| Progress ring | Small and medium only, no strip or large |
| Date card (month header, weekday, large = month calendar) | Small and medium only, no calendar |
| Live ticker (dark strip, d/h/m) | Medium style, no dark strip |
| Year progress (week grid, 87%) | Missing |
| PAPER style | In code, not in design (remove or design it) |

Also missing: Strip 4x1 size (no `targetCellHeight`/min height for 1 row), the launcher picker listing 8 named widgets (needs 8 providers or one per family), in-app gallery with size filter chips and real previews at each size, widget background options (solid, gradient, photo, clear), dynamic colour toggle, corner radius, font, number size, numerals in the studio.

## Gaps: colours
- Only about 15 Material roles are set. Roles the design never defines fall back to Material's default purple/grey: `primaryContainer` (FAB container, selected row on tablets), `surfaceContainer*` (navigation bar, dialogs, sheets, text fields, chips), `secondary`, `tertiary`, `surfaceVariant`. The FAB and bar will not match the teal design.
- Widget colours are hard-coded in Glance (allowed), but the accent set (indigo, rose, amber) is not in the design.

## Gaps: screens
- No onboarding or language picker on first run (design 01).
- Event row: no icon tile, no progress bar, no 3dp plate edge (CD-04).
- Detail: days only. Design shows days, hours, minutes, seconds tiles, progress %, Edit / Widget / Share row, age tiles (years, months, days, days lived), count-up tiles (days, weeks, years, milestone).
- Event day and completed screens (08, 09) are not distinct screens.
- Empty state: no suggestion chips, no illustration.
- Notifications: check the rich "Remind me tomorrow" action exists.
- Widget setup on drop: needs template chips for all families, progress and dynamic colour switches, corner radius.
- Studio (4 tabs: Template, Colours, Text, Content) is not built.
- Settings: design still has "Week starts on" (removed in code) and a Theme row to check.

## Screens that need designing (not in the canvas)
1. Widget picker mapping: how 8 families appear in the Android launcher (names, sizes). Designed (board 14), needs the real preview set.
2. Year progress and Agenda widget states: empty, loading, needs setup, event deleted for the new families.
3. Widget studio on tablet and landscape.
4. Period event widget layout (noted open in END_DATE.md).
5. Milestone settings for count-up (which milestones: 100, 365, 730, 1,000).
6. Share card for count-up and period events.
7. Revised Settings after removing Week start (06C).

## Status after the 2026-11-14 pass (nothing verified on a device yet)
Done in code, CI green: all Material colour roles, FAB and bar; 8 widget families with 4 sizes, studio (Template, Colours, Text, Content), in-app gallery with live previews; first-run onboarding with language switch; event row icon tile, progress and plate edge; detail hero per type and phase (days/hours/minutes, age with turns-in and days lived, count-up with milestone, period ongoing and ended, event day, completed with Keep counting up); empty-state suggestion chips; "Remind me tomorrow" notification action; Exact reminders row in Settings.

Known deviations from the design:
- Detail HRS and MIN tiles show the exact time left, not days x 24. DAYS stays the calendar count so it matches the list and widgets.
- Ticker widget refreshes about every 15 minutes (inexact alarm), the design board 06E says the same; the older 13-direction board says once a minute.
- Week start row removed from Settings; board 06C still shows it and needs updating.
- Launcher picker has no preview images (previewLayout not set).
- Progress bars use Material's linear indicator, not the segmented bar.
- Event-day screen reuses the detail layout (EVENT DAY eyebrow plus message) rather than a separate full-screen design.

Designed on board 06G and built: widget loading, deleted, no-events and agenda-empty states; period widget on the big layout; count-up and period share cards; revised Settings (Week start gone, Exact reminders and Updates rows). In-app updates added (GitHub Releases, WorkManager every 3.5 days, Android confirms the install; needs the Internet and install-packages permissions, privacy text updated).
Not built, by decision: studio on tablet and landscape (the owner does not use a tablet).
Needs a device: RTL tile order, corner clipping, TalkBack and 200% font, phone and tablet layouts.
