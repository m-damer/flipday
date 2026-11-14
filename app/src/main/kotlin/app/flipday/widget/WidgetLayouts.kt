package app.flipday.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventType
import app.flipday.core.PeriodPhase
import app.flipday.core.WidgetMath
import java.time.Period
import kotlin.math.roundToInt

internal data class Look(val template: WidgetTemplate, val accent: WidgetAccent, val showTitle: Boolean, val showUnit: Boolean)

private val OnDark = ColorProvider(Color(0xFFF2EFE6))
private val OnDarkMuted = ColorProvider(Color(0xFFA39F94))

@Composable
internal fun Frame(open: Action, dark: Boolean = false, pad: Dp = 12.dp, content: @Composable () -> Unit) {
    val st = LocalStyle.current
    var m = GlanceModifier.fillMaxSize().appWidgetBackground()
    m = when {
        dark -> m.background(TileBg)
        st.bg == BgMode.CLEAR -> m
        st.bgImage != null -> m.background(ImageProvider(st.bgImage), androidx.glance.layout.ContentScale.Crop)
        else -> m.background(Card)
    }
    Box(m.cornerRadius(st.radiusDp.dp).padding(pad).clickable(open), contentAlignment = Alignment.Center) { content() }
}

/** Empty, deleted and setup states (design 06G.1): an icon, one bold line and one muted hint. Tapping opens setup or the app. */
@Composable
internal fun StateCard(@androidx.annotation.DrawableRes icon: Int, title: String, hint: String?, action: Action, look: Look? = null, eyebrow: String? = null) = Frame(action) {
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        if (eyebrow != null && look != null) { Label(eyebrow, look.accent); VGap(8.dp) }
        Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(24.dp), colorFilter = ColorFilter.tint(accent((look?.accent ?: WidgetAccent.TEAL))))
        VGap(6.dp)
        Text(title, style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
        if (hint != null) Text(hint, style = TextStyle(color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}

/** Period widgets reuse the big layout (design 06G.2): the eyebrow, title and sub line follow the period's phase. */
private fun eyebrowFor(c: WCtx, e: Event, fallback: Int): String =
    if (e.type != EventType.PERIOD) c.s(fallback) else when (EventCalculator.period(e.date, e.endDate!!, c.today).phase) {
        PeriodPhase.UPCOMING -> c.s(R.string.wd_p_starts)
        PeriodPhase.ONGOING -> c.s(R.string.wd_p_ends)
        PeriodPhase.ENDED -> c.s(R.string.wd_p_lasted)
    }

private fun titleFor(c: WCtx, e: Event): String =
    if (e.type == EventType.PERIOD && EventCalculator.period(e.date, e.endDate!!, c.today).phase == PeriodPhase.ENDED) "\u2713 " + e.title else e.title

private fun subFor(c: WCtx, e: Event): String =
    if (e.type == EventType.PERIOD) app.flipday.ui.events.periodSubtitle(c.context, e, c.today, c.numerals, c.locale) else c.dateLong(e.date)

@Composable private fun Icon(e: Event, look: Look, size: Dp = 20.dp, tint: ColorProvider = accent(look.accent)) =
    Image(ImageProvider(iconFor(e)), contentDescription = null, modifier = GlanceModifier.size(size), colorFilter = ColorFilter.tint(tint))

@Composable private fun Gap(w: Dp) = Spacer(GlanceModifier.width(w))
@Composable private fun VGap(h: Dp) = Spacer(GlanceModifier.height(h))

/** Small flap-tile row for the "other events" lists: title on the left, two tiles on the right. */
@Composable
private fun ListRow(c: WCtx, e: Event, look: Look, withBar: Boolean = false) {
    val (v, _) = c.head(e)
    Column(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) { Title(e.title, 12); if (withBar) DateSub(c.dateShort(e.date), 10) }
            Tiles(c.tiles(v), 18.dp, look.accent, 2.dp)
        }
    }
}

@Composable
private fun Others(c: WCtx, look: Look, max: Int) {
    for (i in 0 until max) c.other(i)?.let { ListRow(c, it, look) }
}

// ---------------------------------------------------------------- Big number

/** The count as one group of tiles, or as years, months and days when the studio's "Y\u00b7M\u00b7D" is on. */
@Composable
private fun BigTiles(c: WCtx, e: Event, digits: String, h: Dp, look: Look) {
    val ymd = if (LocalStyle.current.timeMode == TimeMode.YMD) when {
        e.type == EventType.COUNTDOWN && e.date.isAfter(c.today) -> Period.between(c.today, e.date)
        (e.type == EventType.COUNTUP || e.type == EventType.PERIOD) && !e.date.isAfter(c.today) -> Period.between(e.date, c.today)
        else -> null
    } else null
    if (ymd == null) Tiles(digits, h, look.accent) else Row(verticalAlignment = Alignment.Bottom) {
        listOf(ymd.years to R.string.wd_years, ymd.months to R.string.wd_months, ymd.days to R.string.wd_days).forEachIndexed { i, (v, label) ->
            if (i > 0) Gap(4.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(c.tiles(v.toLong()), h * 0.5f, look.accent, 2.dp); Sub(c.s(label), 8) }
        }
    }
}

@Composable
internal fun BigLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val (v, unit) = c.head(e)
    val progress = WidgetMath.ringProgress(e, c.today)
    val digits = c.tiles(v)
    Frame(open) {
        when (size) {
            WidgetSize.STRIP -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(e, look); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) {
                    if (look.showTitle) Title(titleFor(c, e), 13)
                    DateSub(subFor(c, e), 10)
                    Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
                }
                Gap(8.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(digits, 26.dp, look.accent, 2.dp); if (look.showUnit) Sub(unit, 9) }
            }
            WidgetSize.SMALL -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                if (e.type == EventType.COUNTDOWN) Label(c.s(R.string.wd_due_in), look.accent) else if (e.type == EventType.PERIOD) Label(eyebrowFor(c, e, R.string.wd_upcoming), look.accent)
                VGap(4.dp); BigTiles(c, e, digits, 52.dp, look); VGap(4.dp)
                Sub(listOfNotNull(titleFor(c, e).takeIf { look.showTitle }, unit.takeIf { look.showUnit }).joinToString(" · "), 11)
            }
            WidgetSize.MEDIUM -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Label(eyebrowFor(c, e, R.string.wd_upcoming), look.accent)
                        if (look.showTitle) Title(titleFor(c, e), 15, 2)
                        DateSub(subFor(c, e), 11)
                    }
                    BigTiles(c, e, digits, 56.dp, look)
                }
                Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                Label("${eyebrowFor(c, e, R.string.wd_upcoming)} · ${c.dayMonth(c.today)}", look.accent)
                if (look.showTitle) Title(titleFor(c, e), 17)
                Box(GlanceModifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { BigTiles(c, e, digits, 84.dp, look) }
                Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
                VGap(8.dp); Others(c, look, 3)
            }
        }
    }
}

// ---------------------------------------------------------------- Age counter

@Composable
internal fun AgeLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val age = EventCalculator.age(e.date, c.today)
    val next = EventCalculator.nextBirthday(e.date, c.today)
    val toNext = EventCalculator.daysUntil(next, c.today)
    val turns = c.n(EventCalculator.turnsAge(e.date, c.today))
    val progress = WidgetMath.ringProgress(e, c.today)
    val (lived, weeks) = WidgetMath.lived(e.date, c.today)
    val turnsLine = if (toNext <= 0L) c.s(R.string.state_today) else c.p(R.plurals.wd_turns_in, toNext, turns, c.n(toNext))
    val years = c.p(R.plurals.wd_n_years, age.years.toLong(), c.n(age.years))
    val months = c.p(R.plurals.wd_n_months, age.months.toLong(), c.n(age.months))
    val days = c.p(R.plurals.wd_n_days, age.days.toLong(), c.n(age.days))
    Frame(open) {
        when (size) {
            WidgetSize.STRIP -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(e, look); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) {
                    if (look.showTitle) Title(e.title, 13)
                    Sub(turnsLine, 10)
                    Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
                }
                Gap(8.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Tiles(c.tiles(age.years.toLong(), 1), 26.dp, look.accent)
                    Sub("$years", 9)
                }
            }
            WidgetSize.SMALL -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                if (look.showTitle) Title(e.title, 12)
                VGap(4.dp); Tiles(c.tiles(age.years.toLong(), 1), 52.dp, look.accent); VGap(4.dp)
                Sub(years, 11)
                Sub("${c.n(age.months)} · ${c.n(age.days)}", 10)
            }
            WidgetSize.MEDIUM -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        if (look.showTitle) Title(e.title, 14)
                        Sub(years, 11); VGap(4.dp); Chip(months); VGap(3.dp); Chip(days)
                    }
                    Tiles(c.tiles(age.years.toLong(), 1), 56.dp, look.accent)
                }
                Sub("$turnsLine · ${c.percent(progress)}", 10)
                Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                if (look.showTitle) Title(e.title, 17)
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Unit3(c.tiles(age.years.toLong(), 1), c.s(R.string.wd_years), look)
                    Gap(14.dp); Unit3(c.tiles(age.months.toLong()), c.s(R.string.wd_months), look)
                    Gap(14.dp); Unit3(c.tiles(age.days.toLong()), c.s(R.string.wd_days), look)
                }
                Sub("$turnsLine · ${c.percent(progress)}", 11)
                Bar(progress, look.accent, GlanceModifier.fillMaxWidth())
                VGap(8.dp)
                Row(GlanceModifier.fillMaxWidth()) {
                    Box(GlanceModifier.defaultWeight()) { Sub(c.s(R.string.wd_days_lived, c.grouped(lived)), 10) }
                    Sub(c.s(R.string.wd_weeks_lived, c.grouped(weeks)), 10)
                }
            }
        }
    }
}

@Composable
private fun Unit3(digits: String, label: String, look: Look) =
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(digits, 64.dp, look.accent); VGap(3.dp); Sub(label, 10) }

// ---------------------------------------------------------------- Count-up

@Composable
internal fun CountUpLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val since = EventCalculator.daysSince(e.date, c.today).coerceAtLeast(0)
    val m = WidgetMath.nextMilestone(since)
    val ymd = Period.between(e.date, c.today.coerceAtLeast(e.date))
    val digits = "+" + c.tiles(since, 1)
    val sinceLine = c.s(R.string.wd_since, c.dateLong(e.date))
    val unit = c.s(R.string.label_days)
    val inDays = c.p(R.plurals.wd_in_days, m.daysLeft, c.n(m.daysLeft))
    Frame(open) {
        when (size) {
            WidgetSize.STRIP -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(e, look); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) { if (look.showTitle) Title(e.title, 13); Sub(sinceLine, 10) }
                Gap(8.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(digits, 26.dp, look.accent, 2.dp); if (look.showUnit) Sub(unit, 9) }
            }
            WidgetSize.SMALL -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                if (look.showTitle) Title(e.title, 12)
                VGap(4.dp); Tiles(digits, 44.dp, look.accent, 2.dp); VGap(4.dp)
                if (look.showUnit) Sub("$unit · ${c.dateShort(e.date)}", 10)
            }
            WidgetSize.MEDIUM -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    if (look.showTitle) Title(e.title, 14)
                    VGap(4.dp); Tiles(digits, 44.dp, look.accent, 2.dp); VGap(4.dp); Sub(sinceLine, 10)
                }
                Gap(10.dp)
                Column(GlanceModifier.defaultWeight()) {
                    Label(c.s(R.string.wd_next_milestone), look.accent)
                    Title("${c.grouped(m.target)} $unit", 15)
                    Sub(inDays, 11); VGap(4.dp)
                    Row { m.shown.forEachIndexed { i, v -> if (i > 0) Gap(4.dp); Chip(c.grouped(v)) } }
                }
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (look.showTitle) Title(e.title, 17)
                VGap(10.dp); Tiles(digits, 72.dp, look.accent, 4.dp); VGap(4.dp)
                Sub(unit, 12); VGap(10.dp)
                Sub(listOf(
                    c.p(R.plurals.wd_n_years, ymd.years.toLong(), c.n(ymd.years)),
                    c.p(R.plurals.wd_n_months, ymd.months.toLong(), c.n(ymd.months)),
                    c.p(R.plurals.wd_n_days, ymd.days.toLong(), c.n(ymd.days)),
                ).joinToString(" "), 12)
                VGap(10.dp)
                Bar((since.toFloat() / m.target).coerceIn(0f, 1f), look.accent, GlanceModifier.fillMaxWidth())
                VGap(4.dp)
                Sub(c.s(R.string.wd_next_line, c.grouped(m.target), inDays), 10)
            }
        }
    }
}

// ---------------------------------------------------------------- Agenda

@Composable
internal fun AgendaLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val rows = listOf(e) + c.others
    Frame(open) {
        when (size) {
            WidgetSize.STRIP, WidgetSize.SMALL -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(e, look); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) {
                    rows.take(2).forEach { r -> val (v, u) = c.head(r); Title("${r.title} · ${c.n(v)} $u", 12) }
                }
                if (rows.size > 2) Sub(c.s(R.string.wd_more, c.n(rows.size - 2)), 10)
            }
            WidgetSize.MEDIUM, WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth()) {
                    Box(GlanceModifier.defaultWeight()) { Label(c.s(R.string.wd_upcoming), look.accent) }
                    Sub(c.dayMonth(c.today), 10)
                }
                VGap(4.dp)
                rows.take(if (size == WidgetSize.LARGE) 4 else 2).forEach { r ->
                    val (v, u) = c.head(r)
                    Column(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(r, look, 16.dp, Muted); Gap(8.dp)
                            Column(GlanceModifier.defaultWeight()) { Title(r.title, 13); DateSub(c.dateShort(r.date), 10) }
                            Text("${c.n(v)} $u", maxLines = 1, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        if (size == WidgetSize.LARGE) Bar(WidgetMath.ringProgress(r, c.today), look.accent, GlanceModifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Progress ring

private fun ringBitmap(context: Context, sizeDp: Float, progress: Float, arc: Int, track: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).roundToInt().coerceAtLeast(2)
    val stroke = 9f * density * (sizeDp / 110f).coerceIn(0.5f, 1.3f)
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
    val inset = stroke / 2
    val rect = RectF(inset, inset, px - inset, px - inset)
    paint.color = track; canvas.drawArc(rect, 0f, 360f, false, paint)
    if (progress > 0f) { paint.color = arc; canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0f, 1f), false, paint) }
    return bmp
}

@Composable
private fun Ring(c: WCtx, look: Look, progress: Float, sizeDp: Float, tileHeight: Dp, digits: String, unit: String?) {
    val dark = (c.context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val bitmap = ringBitmap(c.context, sizeDp, progress, (if (dark) look.accent.bgNight else look.accent.bgDay).toInt(), if (dark) 0xFF37373B.toInt() else 0xFFDAD6CB.toInt())
    Box(GlanceModifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(sizeDp.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(digits, tileHeight, look.accent, 2.dp); if (unit != null) Sub(unit, 10) }
    }
}

@Composable
internal fun RingLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val (v, unit) = c.head(e)
    val progress = WidgetMath.ringProgress(e, c.today)
    val digits = c.tiles(v)
    val u = unit.takeIf { look.showUnit }
    Frame(open) {
        when (size) {
            WidgetSize.STRIP -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Ring(c, look, progress, 40f, 16.dp, digits, null); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) { if (look.showTitle) Title(e.title, 13); DateSub(c.dateLong(e.date), 10) }
                Text(c.percent(progress), style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            WidgetSize.SMALL -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Ring(c, look, progress, 96f, 30.dp, digits, u)
                if (look.showTitle) Sub(e.title, 10)
            }
            WidgetSize.MEDIUM -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Ring(c, look, progress, 96f, 30.dp, digits, u); Gap(14.dp)
                Column(GlanceModifier.defaultWeight()) {
                    Label(c.s(R.string.wd_upcoming), look.accent)
                    if (look.showTitle) Title(e.title, 15, 2)
                    DateSub(c.dateLong(e.date), 11); Sub(c.percent(progress), 11)
                }
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Ring(c, look, progress, 120f, 36.dp, digits, u); Gap(14.dp)
                    Column(GlanceModifier.defaultWeight()) {
                        Label(c.s(R.string.wd_upcoming), look.accent)
                        if (look.showTitle) Title(e.title, 16, 2)
                        DateSub(c.dateLong(e.date), 11); Sub(c.percent(progress), 11)
                    }
                }
                VGap(10.dp); Others(c, look, 4)
            }
        }
    }
}

// ---------------------------------------------------------------- Date card

@Composable
private fun Leaf(c: WCtx, anchor: java.time.LocalDate, look: Look, big: Dp) {
    Column(GlanceModifier.width(big * 1.6f).background(Card).cornerRadius(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(GlanceModifier.fillMaxWidth().background(accent(look.accent)).cornerRadius(6.dp).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text("${c.month(anchor)} ${c.n(anchor.year)}", maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        VGap(4.dp)
        Tiles(c.tiles(anchor.dayOfMonth.toLong(), 1), big, look.accent, 2.dp)
        VGap(2.dp)
        Sub(c.weekday(anchor), 10)
    }
}

@Composable
internal fun DateLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val anchor = app.flipday.core.ReminderPlanner.anchorDate(e, c.today) ?: e.date
    val (v, unit) = c.head(e)
    val daysText = "${c.n(v)} $unit"
    Frame(open) {
        when (size) {
            WidgetSize.STRIP -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.background(accent(look.accent)).cornerRadius(6.dp).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text("${c.month(anchor)} ${c.n(anchor.dayOfMonth)}", maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
                Gap(10.dp)
                Column(GlanceModifier.defaultWeight()) { if (look.showTitle) Title(e.title, 13); DateSub(c.dateLong(anchor), 10) }
                Text(daysText, maxLines = 1, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
            WidgetSize.SMALL -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Leaf(c, anchor, look, 40.dp); VGap(4.dp)
                Sub(listOfNotNull(e.title.takeIf { look.showTitle }, daysText).joinToString(" · "), 10)
            }
            WidgetSize.MEDIUM -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Leaf(c, anchor, look, 44.dp); Gap(14.dp)
                Column(GlanceModifier.defaultWeight()) {
                    Label(c.s(R.string.wd_next_up), look.accent)
                    if (look.showTitle) Title(e.title, 15, 2)
                    VGap(4.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) { Tiles(c.tiles(v), 30.dp, look.accent, 2.dp); Gap(6.dp); Sub(c.s(R.string.label_days_left), 10) }
                }
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth()) {
                    Box(GlanceModifier.defaultWeight()) { Title(c.monthYear(anchor), 15) }
                    Text(daysText, style = TextStyle(color = accent(look.accent), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
                VGap(8.dp)
                val start = c.weekStart
                Row(GlanceModifier.fillMaxWidth()) {
                    for (i in 0..6) {
                        val d = start.plus(i.toLong())
                        Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            Sub(d.getDisplayName(java.time.format.TextStyle.NARROW, c.locale), 10)
                        }
                    }
                }
                WidgetMath.monthGrid(anchor, start).forEach { week ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        week.forEach { day ->
                            Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    val mark = day == anchor.dayOfMonth
                                    Box(
                                        GlanceModifier.size(22.dp).let { if (mark) it.background(accent(look.accent)).cornerRadius(11.dp) else it },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(c.n(day), style = TextStyle(color = if (mark) ColorProvider(Color.White) else Ink, fontSize = 11.sp, fontWeight = if (mark) FontWeight.Bold else FontWeight.Normal))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Live ticker

@Composable
private fun Segment(value: String, label: String, h: Dp, look: Look) =
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Tiles(value, h, look.accent, 2.dp); VGap(2.dp); Sub(label, 9) }

@Composable
internal fun TickerLayout(c: WCtx, e: Event, look: Look, size: WidgetSize, open: Action) {
    val parts = WidgetMath.tickerParts(e, c.today, c.now)
    val (d, h, m) = parts ?: Triple(0L, 0, 0)
    val dd = c.tiles(d); val hh = c.tiles(h.toLong()); val mm = c.tiles(m.toLong())
    val updated = c.s(R.string.widget_updated, c.n(c.now.toLocalTime().format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(c.locale))))
    val colon = @Composable { Text(":", style = TextStyle(color = accent(look.accent), fontSize = 18.sp, fontWeight = FontWeight.Bold)) }
    when (size) {
        WidgetSize.STRIP, WidgetSize.SMALL -> Frame(open, dark = true) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(e, look, 20.dp, digitColor(look.accent)); Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) {
                    if (look.showTitle) Text(e.title, maxLines = 1, style = TextStyle(color = OnDark, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Text(c.s(R.string.wd_due_on, c.dateShort(e.date)), maxLines = 1, style = TextStyle(color = OnDarkMuted, fontSize = 10.sp))
                }
                Text(c.s(R.string.wd_ticker_short, dd, hh, mm), maxLines = 1, style = TextStyle(color = digitColor(look.accent), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
        else -> Frame(open) {
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Label(c.s(R.string.wd_upcoming), look.accent)
                if (look.showTitle) Title(e.title, 14)
                VGap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val th = if (size == WidgetSize.LARGE) 48.dp else 40.dp
                    Segment(dd, c.s(R.string.ticker_days), th, look); Gap(4.dp); colon(); Gap(4.dp)
                    Segment(hh, c.s(R.string.ticker_hours), th, look); Gap(4.dp); colon(); Gap(4.dp)
                    Segment(mm, c.s(R.string.ticker_minutes), th, look)
                }
                VGap(4.dp); Sub(updated, 10)
                if (size == WidgetSize.LARGE) { VGap(8.dp); Others(c, look, 3) }
            }
        }
    }
}

// ---------------------------------------------------------------- Year progress

@Composable
internal fun YearLayout(c: WCtx, e: Event?, look: Look, size: WidgetSize, open: Action) {
    val y = WidgetMath.yearProgress(c.today)
    val pct = c.percent(y.progress)
    val yearText = c.n(y.year)
    val nextIn = e?.let { daysTo(it, c.today) }
    Frame(open, dark = true) {
        when (size) {
            WidgetSize.STRIP, WidgetSize.SMALL -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_grid), contentDescription = null, modifier = GlanceModifier.size(20.dp), colorFilter = ColorFilter.tint(digitColor(look.accent)))
                Gap(8.dp)
                Column(GlanceModifier.defaultWeight()) {
                    Text(c.s(R.string.wd_year_week, yearText, c.n(y.week)), maxLines = 1, style = TextStyle(color = OnDark, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Text("${c.n(y.daysLeft)} ${c.s(R.string.label_days_left)}", maxLines = 1, style = TextStyle(color = OnDarkMuted, fontSize = 10.sp))
                    androidx.glance.appwidget.LinearProgressIndicator(progress = y.progress, modifier = GlanceModifier.fillMaxWidth(), color = digitColor(look.accent), backgroundColor = ColorProvider(Color(0xFF37373B)))
                }
                Gap(8.dp)
                Text(pct, style = TextStyle(color = digitColor(look.accent), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
            WidgetSize.MEDIUM -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(c.s(R.string.wd_week_of, c.n(y.week), c.n(y.weeksTotal)).uppercase(), maxLines = 1, style = TextStyle(color = OnDarkMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    Text(yearText, style = TextStyle(color = digitColor(look.accent), fontSize = 26.sp, fontWeight = FontWeight.Bold))
                    Text(pct, style = TextStyle(color = OnDark, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Text("${c.n(y.daysLeft)} ${c.s(R.string.label_days_left)}", maxLines = 1, style = TextStyle(color = OnDarkMuted, fontSize = 10.sp))
                }
                WeekGrid(y, look, 9.dp)
            }
            WidgetSize.LARGE -> Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.defaultWeight()) { Text(yearText, style = TextStyle(color = digitColor(look.accent), fontSize = 26.sp, fontWeight = FontWeight.Bold)) }
                    Text(pct, style = TextStyle(color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
                Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) { WeekGrid(y, look, 17.dp) }
                Row(GlanceModifier.fillMaxWidth()) {
                    Stat(c.s(R.string.wd_week_label), "${c.n(y.week)} / ${c.n(y.weeksTotal)}", Modifier0)
                    Stat(c.s(R.string.wd_days_left_stat), c.n(y.daysLeft), Modifier0)
                    Stat(c.s(R.string.wd_next_event), nextIn?.let { "${c.n(it)} ${c.s(R.string.wd_day_short)}" } ?: "–", Modifier0)
                }
            }
        }
    }
}

private val Modifier0 = GlanceModifier

@Composable
private fun androidx.glance.layout.RowScope.Stat(label: String, value: String, m: GlanceModifier) =
    Column(m.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, maxLines = 1, style = TextStyle(color = OnDarkMuted, fontSize = 9.sp))
        Text(value, maxLines = 1, style = TextStyle(color = OnDark, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }

/** 52 weeks as 13 columns by 4 rows: past weeks filled, this week white, future weeks dim. */
@Composable
private fun WeekGrid(y: WidgetMath.YearStatus, look: Look, cell: Dp) {
    Column {
        for (r in 0 until 4) {
            Row {
                for (col in 0 until 13) {
                    val week = r * 13 + col + 1
                    val color = when {
                        week < y.week -> digitColor(look.accent)
                        week == y.week -> ColorProvider(Color.White)
                        else -> ColorProvider(Color(0xFF37373B))
                    }
                    Box(GlanceModifier.padding(1.dp)) { Box(GlanceModifier.size(cell).background(color).cornerRadius(2.dp)) {} }
                }
            }
        }
    }
}
