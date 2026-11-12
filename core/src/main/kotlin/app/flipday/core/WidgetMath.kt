package app.flipday.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
}
