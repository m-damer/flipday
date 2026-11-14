package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek

/** Numbers behind the ring and live-ticker widget styles. Pure Kotlin so they run as JVM tests. */
object WidgetMath {
    /** Share of the time already used, 0 to 1. Count-ups have no end, so their ring stays empty. */
    fun ringProgress(event: Event, today: LocalDate): Float = when (event.type) {
        EventType.COUNTDOWN -> EventCalculator.progress(event.startDate, event.date, today)?.toFloat() ?: 0f
        EventType.PERIOD -> EventCalculator.period(event.date, event.endDate!!, today).progress.toFloat()
        EventType.AGE -> {
            val next = EventCalculator.nextBirthday(event.date, today)
            EventCalculator.progress(next.minusYears(1), next, today)?.toFloat() ?: 0f
        }
        EventType.COUNTUP -> 0f
    }

    /** Days, hours, minutes left until the event, or null when the event has nothing to count down to. */
    fun tickerParts(event: Event, today: LocalDate, now: LocalDateTime): Triple<Long, Int, Int>? {
        val target = when (event.type) {
            EventType.COUNTDOWN -> event.date.atTime(event.time ?: LocalTime.MIDNIGHT)
            EventType.AGE -> EventCalculator.nextBirthday(event.date, today).atStartOfDay()
            EventType.PERIOD -> {
                val s = EventCalculator.period(event.date, event.endDate!!, today)
                if (s.phase == PeriodPhase.UPCOMING) event.date.atStartOfDay() else event.endDate!!.plusDays(1).atStartOfDay()
            }
            EventType.COUNTUP -> return null
        }
        if (!target.isAfter(now)) return null
        val r = EventCalculator.remaining(target, now)
        return Triple(r.days, r.hours, r.minutes)
    }

    /** Where the calendar year stands: week 1..52, days left after today, and the share of the year used (0..1). */
    data class YearStatus(val year: Int, val week: Int, val weeksTotal: Int, val daysLeft: Int, val progress: Float)

    fun yearProgress(today: LocalDate): YearStatus {
        val length = today.lengthOfYear()
        return YearStatus(today.year, ((today.dayOfYear - 1) / 7 + 1).coerceAtMost(52), 52, length - today.dayOfYear, today.dayOfYear.toFloat() / length)
    }

    /** Milestone steps for a count-up: 100, 365, 730, 1,000, then every 1,000 days. */
    private val FIXED_MILESTONES = listOf(100L, 365L, 730L, 1000L)

    data class Milestone(val target: Long, val daysLeft: Long, val shown: List<Long>)

    /** The next milestone after [daysSince], how far away it is, and the three steps to show as chips. */
    fun nextMilestone(daysSince: Long): Milestone {
        val next = FIXED_MILESTONES.firstOrNull { it > daysSince } ?: ((daysSince / 1000) + 1) * 1000
        val chips = if (next <= 1000) FIXED_MILESTONES.filter { it >= 365 }.take(3) else listOf(next - 1000, next, next + 1000)
        return Milestone(next, next - daysSince, chips)
    }

    /** Days and weeks lived since the birth date. */
    fun lived(birth: LocalDate, today: LocalDate): Pair<Long, Long> {
        val days = ChronoUnit.DAYS.between(birth, today)
        return days to days / 7
    }

    /** Rows of day numbers for the month of [date], padded with null. The week starts on [weekStart]. */
    fun monthGrid(date: LocalDate, weekStart: DayOfWeek): List<List<Int?>> {
        val first = date.withDayOfMonth(1)
        val lead = (first.dayOfWeek.value - weekStart.value + 7) % 7
        val cells = MutableList<Int?>(lead) { null } + (1..date.lengthOfMonth()).map<Int, Int?> { it }
        return cells.chunked(7).map { row -> row + List(7 - row.size) { null } }
    }
}
