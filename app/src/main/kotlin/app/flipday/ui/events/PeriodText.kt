package app.flipday.ui.events

import android.content.Context
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.core.PeriodPhase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** One-line state of a period for list rows and the detail screen. Pure text, no layout. */
internal fun periodSubtitle(context: Context, event: Event, today: LocalDate, numerals: NumeralMode, locale: Locale): String {
    val end = event.endDate ?: return ""
    val tag = locale.toLanguageTag()
    fun n(v: Any) = NumeralFormatter.format(v.toString(), numerals, tag)
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    fun d(date: LocalDate) = n(date.format(fmt))
    val s = EventCalculator.period(event.date, end, today)
    val res = context.resources
    return when (s.phase) {
        PeriodPhase.UPCOMING -> res.getQuantityString(R.plurals.period_starts_lasts, s.totalDays.toInt(), d(event.date), n(s.totalDays))
        PeriodPhase.ONGOING ->
            if (s.daysLeft == 0L) context.getString(R.string.period_last_day)
            else context.getString(R.string.period_progress_until, n(s.dayNumber), n(s.totalDays), d(end))
        PeriodPhase.ENDED ->
            if (s.daysSinceEnd == 0L) context.getString(R.string.period_ended_today)
            else res.getQuantityString(R.plurals.period_ended_ago, s.daysSinceEnd.toInt(), n(s.daysSinceEnd))
    }
}
