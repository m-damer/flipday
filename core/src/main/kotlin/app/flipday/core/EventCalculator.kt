package app.flipday.core

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit



/** Pure functions. No Android imports, so they run as plain JVM unit tests. */
object EventCalculator {

    fun phase(date: LocalDate, today: LocalDate): EventPhase = when {
        date.isAfter(today) -> EventPhase.UPCOMING
        date.isEqual(today) -> EventPhase.TODAY
        else -> EventPhase.DONE
    }

    /** Calendar days, not 24-hour blocks. */
    fun daysUntil(date: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    /** Day 0 is the start date. */
    fun daysSince(start: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(start, today)

    fun remaining(target: LocalDateTime, now: LocalDateTime): Remaining {
        val minutes = maxOf(0L, Duration.between(now, target).toMinutes())
        return Remaining(minutes / 1440, ((minutes % 1440) / 60).toInt(), (minutes % 60).toInt())
    }

    fun age(birth: LocalDate, today: LocalDate): Ymd {
        val p = Period.between(birth, today)
        return Ymd(p.years, p.months, p.days)
    }

    /** Next birthday on or after today. 29 Feb in a non-leap year falls on 28 Feb (java.time withYear). */
    fun nextBirthday(birth: LocalDate, today: LocalDate): LocalDate {
        val thisYear = birth.withYear(today.year)
        return if (thisYear.isBefore(today)) birth.withYear(today.year + 1) else thisYear
    }

    fun turnsAge(birth: LocalDate, today: LocalDate): Int = nextBirthday(birth, today).year - birth.year

    /** 0.0 to 1.0. Returns null when there is no usable start date. */
    fun progress(start: LocalDate?, target: LocalDate, today: LocalDate): Double? {
        if (start == null) return null
        val total = ChronoUnit.DAYS.between(start, target)
        if (total <= 0) return null
        return (ChronoUnit.DAYS.between(start, today).toDouble() / total).coerceIn(0.0, 1.0)
    }

    /** Days until the next multiple of [step] since the start (for "1,000 days" milestones). */
    fun daysToMilestone(daysSince: Long, step: Long = 1000): Long = step - (daysSince % step)
}
