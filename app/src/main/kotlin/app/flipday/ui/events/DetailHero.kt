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
private data class TileGroup(val label: String, val text: String)

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
    val unitHrs = stringResource(R.string.unit_hrs); val unitMin = stringResource(R.string.unit_min); val unitMth = stringResource(R.string.unit_mth)
    val unitDays = stringResource(R.string.unit_days); val unitWks = stringResource(R.string.unit_wks); val unitYrs = stringResource(R.string.unit_yrs)
    val unitLived = stringResource(R.string.unit_lived)
    val groups = ArrayList<TileGroup>()
    var bigText = big
    var unitLabel: String? = null
    var spoken = description
    var eyebrow = R.string.detail_eyebrow_down
    var progress: Float? = null
    var line: String? = null

    when (event.type) {
        EventType.COUNTDOWN -> {
            when (EventCalculator.phase(event.date, today)) {
                EventPhase.TODAY -> { eyebrow = R.string.detail_eyebrow_today; line = stringResource(R.string.detail_today_msg) }
                EventPhase.DONE -> {
                    eyebrow = R.string.detail_eyebrow_done
                    val ago = EventCalculator.daysSince(event.date, today).coerceAtLeast(0)
                    bigText = "+" + n(ago.toString().padStart(2, '0'))
                    unitLabel = stringResource(R.string.detail_days_ago)
                    spoken = "${event.title}, ${n(ago)} $unitLabel"
                    line = stringResource(R.string.detail_done_msg)
                }
                EventPhase.UPCOMING -> {
                    val minutes = minutesLeft(event.date.atTime(event.time ?: LocalTime.MIDNIGHT), now)
                    groups += TileGroup(unitHrs, n((minutes / 60).toString().padStart(2, '0')))
                    groups += TileGroup(unitMin, n(minutes.toString().padStart(2, '0')))
                    progress = EventCalculator.progress(event.startDate, event.date, today)?.toFloat()
                }
            }
        }
        EventType.AGE -> {
            val age = EventCalculator.age(event.date, today)
            eyebrow = R.string.detail_eyebrow_age
            groups += TileGroup(unitMth, n(age.months.toString().padStart(2, '0')))
            groups += TileGroup(unitDays, n(age.days.toString().padStart(2, '0')))
            val daysToBirthday = EventCalculator.daysUntil(EventCalculator.nextBirthday(event.date, today), today)
            groups += TileGroup(stringResource(R.string.detail_turns_in, n(EventCalculator.turnsAge(event.date, today))), n(daysToBirthday.toString().padStart(2, '0')))
            groups += TileGroup(unitLived, n(WidgetMath.lived(event.date, today).first.toString()))
            progress = WidgetMath.ringProgress(event, today)
        }
        EventType.COUNTUP -> {
            val since = EventCalculator.daysSince(event.date, today).coerceAtLeast(0)
            eyebrow = R.string.detail_eyebrow_since
            groups += TileGroup(unitWks, n((since / 7).toString().padStart(2, '0')))
            groups += TileGroup(unitYrs, n(EventCalculator.age(event.date, today).years.toString().padStart(2, '0')))
            val m = WidgetMath.nextMilestone(since)
            line = stringResource(R.string.detail_milestone, n(m.target), pluralStringResource(R.plurals.wd_in_days, m.daysLeft.toInt(), n(m.daysLeft)))
        }
        EventType.PERIOD -> {
            val s = EventCalculator.period(event.date, event.endDate!!, today)
            when (s.phase) {
                PeriodPhase.UPCOMING -> {
                    eyebrow = R.string.detail_eyebrow_starts
                    groups += TileGroup(unitHrs, n((s.daysToStart * 24).toString().padStart(2, '0')))
                }
                PeriodPhase.ONGOING -> {
                    eyebrow = R.string.detail_eyebrow_ends
                    val minutes = minutesLeft(event.endDate!!.plusDays(1).atStartOfDay(), now)
                    groups += TileGroup(unitHrs, n((minutes / 60).toString().padStart(2, '0')))
                    progress = s.progress.toFloat()
                    line = stringResource(R.string.period_progress, n(s.dayNumber), n(s.totalDays))
                }
                PeriodPhase.ENDED -> {
                    eyebrow = R.string.detail_eyebrow_lasted
                    groups += TileGroup(unitWks, n((s.totalDays / 7).toString().padStart(2, '0')))
                    progress = 1f
                    line = stringResource(R.string.period_completed)
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(eyebrow), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        val (_, unit) = headline(event, today)
        Text(unitLabel ?: stringResource(unit), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        FlipNumber(bigText, spoken, tileHeight = 88.dp)
        if (groups.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            groups.chunked(2).forEach { rowGroups ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                    rowGroups.forEach { g ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(g.label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            FlipNumber(g.text, "${g.text} ${g.label}", tileHeight = 40.dp)
                        }
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
