package app.flipday.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.core.ReminderPlanner
import app.flipday.ui.events.headline
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/** Everything a widget layout needs besides the event itself: today, formatting, strings, and the other upcoming events. */
internal class WCtx(
    val context: Context, val today: LocalDate, val now: LocalDateTime, val numerals: NumeralMode,
    val locale: Locale, val tag: String, val others: List<Event>, val timeMode: TimeMode = TimeMode.DAYS,
) {
    fun n(v: Any) = NumeralFormatter.format(v.toString(), numerals, tag)
    /** Digits for flap tiles, at least two. */
    fun tiles(v: Long, min: Int = 2) = n(v.toString().padStart(min, '0'))
    fun grouped(v: Long) = n(NumberFormat.getIntegerInstance(Locale.US).format(v))
    fun s(@StringRes r: Int, vararg a: Any): String = context.getString(r, *a)
    fun p(@PluralsRes r: Int, q: Long, vararg a: Any): String = context.resources.getQuantityString(r, q.toInt().coerceAtLeast(0), *a)
    private fun fmt(d: LocalDate, skeleton: String) = n(d.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)))
    /** "Sat, Dec 5, 2026" */ fun dateLong(d: LocalDate) = fmt(d, "EEEMMMdy")
    /** "Sat, Nov 21" */ fun dateShort(d: LocalDate) = fmt(d, "EEEMMMd")
    /** "12 Nov" */ fun dayMonth(d: LocalDate) = fmt(d, "dMMM")
    fun month(d: LocalDate) = d.format(DateTimeFormatter.ofPattern("MMM", locale)).uppercase(locale)
    fun monthYear(d: LocalDate) = n(d.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLLLy"), locale)))
    fun weekday(d: LocalDate) = d.format(DateTimeFormatter.ofPattern("EEEE", locale))
    val weekStart: DayOfWeek get() = WeekFields.of(locale).firstDayOfWeek

    /** The number and unit a countdown shows: days to go, years old, days since. */
    fun head(e: Event): Pair<Long, String> {
        val (v, u) = headline(e, today)
        // "Weeks" only applies to counts of days; a birthday stays in years, and under a week stays in days.
        return if (timeMode == TimeMode.WEEKS && e.type != EventType.AGE && v >= 7) (v / 7) to context.getString(R.string.wd_weeks_unit)
        else v to context.getString(u)
    }
    fun percent(p: Float) = n("${(p * 100).toInt()}%")
    fun other(i: Int): Event? = others.getOrNull(i)
}

@DrawableRes internal fun iconFor(e: Event): Int = when (e.type) {
    EventType.COUNTDOWN -> R.drawable.ic_type_countdown
    EventType.AGE -> R.drawable.ic_type_age
    EventType.COUNTUP -> R.drawable.ic_type_countup
    EventType.PERIOD -> R.drawable.ic_type_period
}

/** Events with a future or current date, nearest first. Count-ups have nothing to wait for, so they come last. */
internal fun upcomingSorted(events: List<Event>, today: LocalDate): List<Event> {
    val dated = events.mapNotNull { e ->
        if (e.type == EventType.COUNTUP) null else ReminderPlanner.anchorDate(e, today)?.takeIf { !it.isBefore(today) }?.let { e to it }
    }.sortedBy { it.second }.map { it.first }
    return dated + events.filter { it.type == EventType.COUNTUP }
}

internal fun daysTo(e: Event, today: LocalDate): Long? =
    ReminderPlanner.anchorDate(e, today)?.let { EventCalculator.daysUntil(it, today) }
