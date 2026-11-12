package app.flipday.core

import java.time.LocalDate

/** Finished countdowns move to the archive after [afterDays] days. Birthdays and count-ups never finish. */
object ArchivePolicy {
    const val DEFAULT_AFTER_DAYS = 7

    /** Setting value meaning "never archive automatically". */
    const val NEVER = -1

    fun shouldArchive(event: Event, today: LocalDate, afterDays: Int = DEFAULT_AFTER_DAYS): Boolean {
        if (afterDays < 0) return false
        if (event.type != EventType.COUNTDOWN || event.archivedOn != null || event.repeatYearly) return false
        return event.date.plusDays(afterDays.toLong()).isBefore(today)
    }

    /** True when a finished countdown should ask "Count again next year?" instead of archiving silently. */
    fun shouldOfferRepeat(event: Event, today: LocalDate): Boolean =
        event.type == EventType.COUNTDOWN && event.archivedOn == null && event.date.isBefore(today) && event.date.plusYears(1).isAfter(today)
}
