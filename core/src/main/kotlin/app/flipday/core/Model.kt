package app.flipday.core

import java.time.LocalDate
import java.time.LocalTime

enum class EventType { COUNTDOWN, AGE, COUNTUP }
enum class EventPhase { UPCOMING, TODAY, DONE }

data class Remaining(val days: Long, val hours: Int, val minutes: Int)
data class Ymd(val years: Int, val months: Int, val days: Int)

/** Notify [daysBefore] days before the event date at [at]. 0 means on the day. */
data class ReminderRule(val daysBefore: Int, val at: LocalTime = LocalTime.of(9, 0)) {
    init { require(daysBefore >= 0) { "daysBefore must be >= 0" } }
}

data class Event(
    val id: Long = 0,
    val title: String,
    val type: EventType,
    /** Countdown: due date. Age: date of birth. Count-up: start date. */
    val date: LocalDate,
    /** Optional time of day (countdown only). */
    val time: LocalTime? = null,
    /** Optional start of a countdown, used only for the progress bar. */
    val startDate: LocalDate? = null,
    val reminders: List<ReminderRule> = emptyList(),
    val repeatYearly: Boolean = false,
    val archivedOn: LocalDate? = null,
    val milestoneEveryDays: Int? = null,
) {
    init { require(title.isNotBlank()) { "title must not be blank" } }
}
