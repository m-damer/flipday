package app.flipday.ui.events

import android.content.Context
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.core.PeriodPhase
import app.flipday.core.WidgetMath
import java.time.LocalDate

/** The caption, digits and extras for one event's share card, per type (design 06F.1 and 06G.3). */
internal data class ShareContent(val caption: String?, val digits: String, val extras: ShareExtras)

internal fun shareContent(context: Context, e: Event, today: LocalDate, numerals: NumeralMode, tag: String, value: Long): ShareContent {
    fun n(v: Any) = NumeralFormatter.format(v.toString(), numerals, tag)
    val digits = n(value.toString().padStart(2, '0'))
    return when (e.type) {
        EventType.COUNTDOWN -> ShareContent(context.getString(R.string.share_caption_due), digits, ShareExtras())
        EventType.AGE -> ShareContent(context.getString(R.string.detail_eyebrow_age), digits, ShareExtras())
        EventType.COUNTUP -> {
            val m = WidgetMath.nextMilestone(EventCalculator.daysSince(e.date, today).coerceAtLeast(0))
            val chip = context.getString(R.string.detail_milestone, n(m.target), context.resources.getQuantityString(R.plurals.wd_in_days, m.daysLeft.toInt(), n(m.daysLeft)))
            ShareContent(context.getString(R.string.detail_eyebrow_since), "+$digits", ShareExtras(chip = chip))
        }
        EventType.PERIOD -> {
            val s = EventCalculator.period(e.date, e.endDate!!, today)
            val caption = context.getString(when (s.phase) { PeriodPhase.UPCOMING -> R.string.detail_eyebrow_starts; PeriodPhase.ONGOING -> R.string.detail_eyebrow_ends; PeriodPhase.ENDED -> R.string.detail_eyebrow_lasted })
            val extras = when (s.phase) {
                PeriodPhase.ONGOING -> ShareExtras(progress = s.progress.toFloat(), progressLabel = context.getString(R.string.detail_percent, n((s.progress * 100).toInt())))
                PeriodPhase.ENDED -> ShareExtras(progress = 1f, progressLabel = context.getString(R.string.period_completed))
                PeriodPhase.UPCOMING -> ShareExtras()
            }
            ShareContent(caption, digits, extras)
        }
    }
}
