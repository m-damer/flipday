# End date: periods

Status: logic and data layer done (core + Room v2). Design board and UI come next. Do not build UI before the design is approved.

## Problem
Some things have a start and an end: a course, a trip, a contract, Ramadan, a diet that was stopped. The user wants to see how long it lasted, or how long is left.

## Decision
A new event type **Period** (EN "Period", AR "فترة"), plus a **"Mark as ended"** action on count-ups.

- Period = first day + last day, both required. The end is never before the start.
- Both days count. Mon to Fri is 5 days. On the last day "days left" is 0 and the label should say "Last day".
- Three phases, one primary number each, so the card is never ambiguous:

| Phase | Big number | Second line |
|---|---|---|
| Upcoming | days to start | "Lasts N days" |
| Ongoing | days left | "Day X of N", progress bar |
| Ended | N days long | "Ended M days ago" |

- Count-up with no end stays as is. "Mark as ended" turns it into a Period that ends today. The counter stops, and the screen shows how long it lasted. This covers "started and stopped" without asking for an end date up front.
- Reminders count back from the last day ("ends in 3 days"). A start reminder is a later option.
- Ended periods are "completed": hidden from Events unless "Show completed events" is on, archived by the same archive timing as countdowns.
- Nearest-first sorting uses the start while a period is ahead, then the end.

## Why not an optional end date on every type
- Countdown already has a target date and an optional start (for its progress bar). A second date on it would mean two meanings of "date".
- Count-up with a future end date reads as a contradiction in the type name.
- Similar apps use a start + end pair for progress, and one has a review where a future start date shows the start instead of days left. Explicit phases avoid that.

## Data
`events.endEpochDay INTEGER NULL`, migration 1 to 2 (`ALTER TABLE ... ADD COLUMN`). Only Period has a value.

## Open for design
- Add form: type chip "Period", two date fields (From, To), duration shown live ("5 days").
- Detail and list row for the three phases, widget layout, Arabic copy, empty and error states (end before start).
- Where "Mark as ended" lives (detail menu) and its confirmation.
- Archive filter chip for periods.

## Not in this change
Time of day for start/end, repeating periods, a "starts today" reminder.
