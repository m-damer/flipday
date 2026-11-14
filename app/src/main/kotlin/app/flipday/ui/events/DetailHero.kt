package app.flipday.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventPhase
import app.flipday.core.EventType
import app.flipday.core.PeriodPhase
import app.flipday.core.WidgetMath
import app.flipday.ui.components.FlipNumber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One labelled group of flap tiles in the detail hero. */
private data class TileGroup(val label: Int, val text: String)

/**
 * Hero block of the detail screen (design 05.4, 06D.5 to 06D.8): an eyebrow, one large group of tiles, smaller groups below,
 * then progress or a milestone. Every type and phase has its own eyebrow, including event day and completed.
 * [description] is the single spoken description of the large tiles; the small groups read as "value unit".
 */
@Composable
internal fun DetailHero(
    event: Event, today: LocalDate, now: LocalDateTime, description: String,
    n: (Any) -> String, date: (LocalDate) -> String,
) {
    val colors = MaterialTheme.colorScheme
    val (value, _) = headline(event, today)
    val big = n(value.toString().padStart(2, '0')).let { if (event.type == EventType.COUNTUP) "+$it" else it }
    val groups = ArrayList<TileGroup>()
    var eyebrow = R.string.detail_eyebrow_down
    var progress: Float? = null
    var line: String? = null

    when (event.type) {
        EventType.COUNTDOWN -> {
            when (EventCalculator.phase(event.date, today)) {
                EventPhase.TODAY -> { eyebrow = R.string.detail_eyebrow_today; line = stringResource(R.string.detail_today_msg) }
                EventPhase.DONE -> { eyebrow = R.string.detail_eyebrow_done; line = stringResource(R.string.detail_done_msg) }
                EventPhase.UPCOMING -> {
                    val minutes = minutesLeft(event.date.atTime(event.time ?: LocalTime.MIDNIGHT), now)
                    groups += TileGroup(R.string.unit_hrs, n((minutes / 60).toString().padStart(2, '0')))
                    groups += TileGroup(R.string.unit_min, n(minutes.toString().padStart(2, '0')))
                    progress = EventCalculator.progress(event.startDate, event.date, today)?.toFloat()
                }
            }
        }
        EventType.AGE -> {
            val age = EventCalculator.age(event.date, today)
            eyebrow = R.string.detail_eyebrow_age
            groups += TileGroup(R.string.unit_mth, n(age.months.toString().padStart(2, '0')))
            groups += TileGroup(R.string.unit_days, n(age.days.toString().padStart(2, '0')))
            groups += TileGroup(R.string.unit_lived, n(WidgetMath.lived(event.date, today).first.toString()))
            progress = WidgetMath.ringProgress(event, today)
        }
        EventType.COUNTUP -> {
            val since = EventCalculator.daysSince(event.date, today).coerceAtLeast(0)
            eyebrow = R.string.detail_eyebrow_since
            groups += TileGroup(R.string.unit_wks, n((since / 7).toString().padStart(2, '0')))
            groups += TileGroup(R.string.unit_yrs, n(EventCalculator.age(event.date, today).years.toString().padStart(2, '0')))
            val m = WidgetMath.nextMilestone(since)
            line = stringResource(R.string.detail_milestone, n(m.target), pluralStringResource(R.plurals.wd_in_days, m.daysLeft.toInt(), n(m.daysLeft)))
        }
        EventType.PERIOD -> {
            val s = EventCalculator.period(event.date, event.endDate!!, today)
            when (s.phase) {
                PeriodPhase.UPCOMING -> {
                    eyebrow = R.string.detail_eyebrow_starts
                    groups += TileGroup(R.string.unit_hrs, n((s.daysToStart * 24).toString().padStart(2, '0')))
                }
                PeriodPhase.ONGOING -> {
                    eyebrow = R.string.detail_eyebrow_ends
                    val minutes = minutesLeft(event.endDate!!.plusDays(1).atStartOfDay(), now)
                    groups += TileGroup(R.string.unit_hrs, n((minutes / 60).toString().padStart(2, '0')))
                    progress = s.progress.toFloat()
                    line = stringResource(R.string.period_progress, n(s.dayNumber), n(s.totalDays))
                }
                PeriodPhase.ENDED -> {
                    eyebrow = R.string.detail_eyebrow_lasted
                    groups += TileGroup(R.string.unit_wks, n((s.totalDays / 7).toString().padStart(2, '0')))
                    progress = 1f
                    line = stringResource(R.string.period_completed)
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(eyebrow), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        val (_, unit) = headline(event, today)
        Text(stringResource(unit), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        FlipNumber(big, description, tileHeight = 88.dp)
        if (groups.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                groups.forEach { g ->
                    val label = stringResource(g.label)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        FlipNumber(g.text, "${g.text} $label", tileHeight = 40.dp)
                    }
                }
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.detail_percent, n((progress * 100).toInt())), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }
        if (line != null) Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        if (event.type != EventType.PERIOD) Text(date(event.date), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun minutesLeft(target: LocalDateTime, now: LocalDateTime): Long = Duration.between(now, target).toMinutes().coerceAtLeast(0)
