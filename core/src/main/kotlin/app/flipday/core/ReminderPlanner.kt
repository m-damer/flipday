package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime

data class ReminderTrigger(val at: LocalDateTime, val daysBefore: Int)

object ReminderPlanner {
    /** The date reminders count back from. Birthdays use the next occurrence. */
    fun anchorDate(event: Event, today: LocalDate): LocalDate? = when (event.type) {
        EventType.COUNTDOWN -> event.date
        EventType.AGE -> EventCalculator.nextBirthday(event.date, today)
        EventType.COUNTUP -> null
        // Reminders for a period count back from its last day ("ends in 3 days").
        EventType.PERIOD -> event.endDate
    }

    /** Future triggers only, soonest first. Count-ups use [milestoneTrigger] instead. */
    fun upcoming(event: Event, now: LocalDateTime): List<ReminderTrigger> {
        val anchor = anchorDate(event, now.toLocalDate()) ?: return emptyList()
        return event.reminders
            .map { ReminderTrigger(anchor.minusDays(it.daysBefore.toLong()).atTime(it.at), it.daysBefore) }
            .filter { it.at.isAfter(now) }
            .sortedBy { it.at }
    }

    /** Next "every N days" milestone for a count-up, at [rule] time. Null when milestones are off. */
    fun milestoneTrigger(event: Event, today: LocalDate, at: java.time.LocalTime = java.time.LocalTime.of(9, 0)): ReminderTrigger? {
        val step = event.milestoneEveryDays?.toLong() ?: return null
        if (event.type != EventType.COUNTUP || step <= 0) return null
        val since = EventCalculator.daysSince(event.date, today)
        val next = event.date.plusDays((since / step + 1) * step)
        return ReminderTrigger(next.atTime(at), 0)
    }
}
