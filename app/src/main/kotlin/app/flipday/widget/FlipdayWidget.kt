package app.flipday.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventType
import app.flipday.core.WidgetMath
import app.flipday.core.NumeralFormatter
import app.flipday.core.ReminderPlanner
import app.flipday.ui.MainActivity
import app.flipday.ui.events.headline
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.glance.color.ColorProvider as DayNight

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 250.dp)

// Glance cannot read the Compose theme, so widget colours live here (allowed by CLAUDE.md, phase 1 note).
private fun dn(day: Long, night: Long) = DayNight(Color(day), Color(night))
private val TileBg = ColorProvider(Color(0xFF050506))

/**
 * Shows one event as flap tiles. Which event and which style come from per-widget state (set in the setup screen);
 * with no choice it follows the nearest upcoming event. Layout follows the widget size.
 * Refreshes at local midnight and on time changes: see [WidgetRefresher].
 */
class FlipdayWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as FlipdayApp).container
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val events = container.repository.all().filter { it.archivedOn == null }
        val numerals = container.settings.settings.first().numerals
        val tag = context.resources.configuration.locales[0].toLanguageTag()

        provideContent {
            val prefs = currentState<Preferences>()
            val chosen = prefs[WidgetKeys.EVENT_ID] ?: 0L
            val template = WidgetTemplate.parse(prefs[WidgetKeys.TEMPLATE])
            val look = Look(template, WidgetAccent.parse(prefs[WidgetKeys.ACCENT]), prefs[WidgetKeys.SHOW_TITLE] ?: true, prefs[WidgetKeys.SHOW_UNIT] ?: true)
            val today = LocalDate.now()
            val size = LocalSize.current

            val event: Event? = if (chosen > 0) events.firstOrNull { it.id == chosen } else nearest(events, today)
            when {
                chosen > 0 && event == null -> Message(context.getString(R.string.widget_choose), look, configureAction(context, appWidgetId))
                event == null -> Message(context.getString(R.string.widget_empty), look, actionStartActivity(Intent(context, MainActivity::class.java)))
                else -> {
                    val (value, unitRes) = headline(event, today)
                    val digits = NumeralFormatter.format(value.toString().padStart(2, '0'), numerals, tag)
                    val open = actionStartActivity(
                        Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, event.id).setData(Uri.parse("flipday://event/${event.id}")),
                    )
                    val unit = context.getString(unitRes)
                    val locale = context.resources.configuration.locales[0]
                    fun n(v: Any) = NumeralFormatter.format(v.toString(), numerals, tag)
                    val progress = WidgetMath.ringProgress(event, today)
                    val ticker = if (look.template == WidgetTemplate.TICKER) WidgetMath.tickerParts(event, today, LocalDateTime.now()) else null
                    when {
                        look.template == WidgetTemplate.RING ->
                            RingContent(context, event.title, digits, unit, progress, n("${(progress * 100).roundToInt()}%"), n(event.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))), look, size, open)
                        look.template == WidgetTemplate.DATE -> {
                            val anchor = ReminderPlanner.anchorDate(event, today) ?: event.date
                            DateContent(
                                event.title, digits, unit, anchor.format(DateTimeFormatter.ofPattern("MMM", locale)).uppercase(locale),
                                n(anchor.dayOfMonth.toString().padStart(2, '0')), anchor.format(DateTimeFormatter.ofPattern("EEEE", locale)), look, size, open,
                            )
                        }
                        ticker != null -> {
                            val (d, h, m) = ticker
                            val updated = context.getString(R.string.widget_updated, n(LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))))
                            TickerContent(
                                event.title, n(d.toString().padStart(2, '0')), n(h.toString().padStart(2, '0')), n(m.toString().padStart(2, '0')),
                                context.getString(R.string.ticker_days), context.getString(R.string.ticker_hours), context.getString(R.string.ticker_minutes), updated, look, size, open,
                            )
                        }
                        look.template == WidgetTemplate.AGE && event.type == EventType.AGE ->
                            Content(event.title, digits, unit, look, size, open, caption = context.getString(R.string.widget_turns, n(EventCalculator.turnsAge(event.date, today))))
                        else -> Content(event.title, digits, unit, look, size, open)
                    }
                }
            }
        }
    }

    private fun nearest(events: List<Event>, today: LocalDate): Event? =
        events.filter { it.type != EventType.COUNTUP }
            .mapNotNull { e -> ReminderPlanner.anchorDate(e, today)?.takeIf { !it.isBefore(today) }?.let { e to it } }
            .minByOrNull { it.second }?.first

    private fun configureAction(context: Context, appWidgetId: Int): Action = actionStartActivity(
        Intent(context, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId).setData(Uri.parse("flipday://widget/$appWidgetId")),
    )
}

private data class Look(val template: WidgetTemplate, val accent: WidgetAccent, val showTitle: Boolean, val showUnit: Boolean)

private fun bg(l: Look) = if (l.template.onCard) dn(0xFFFFFFFF, 0xFF171719) else dn(l.accent.bgDay, l.accent.bgNight)
private fun ink(l: Look) = if (l.template.onCard) dn(0xFF1A1A1C, 0xFFF2EFE6) else ColorProvider(Color(0xFFFFFFFF))
private fun muted(l: Look) = if (l.template.onCard) dn(0xFF625E54, 0xFFB8B4A8) else ColorProvider(Color(0xFFE8F4F1))

@Composable
private fun Message(text: String, look: Look, action: Action) {
    Box(GlanceModifier.fillMaxSize().background(bg(look)).padding(12.dp).clickable(action), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = ink(look), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun Content(title: String, digits: String, unit: String, look: Look, size: DpSize, open: Action, caption: String? = null) {
    val wide = size.width >= 200.dp && size.height < 200.dp
    val large = size.height >= 200.dp
    val tileHeight = when { large -> 84.dp; wide -> 56.dp; else -> 52.dp }
    Box(GlanceModifier.fillMaxSize().background(bg(look)).padding(12.dp).clickable(open), contentAlignment = Alignment.Center) {
        if (wide) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    if (caption != null) Text(caption, style = TextStyle(color = accentInk(look), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    if (look.showTitle) Text(title, maxLines = 2, style = TextStyle(color = ink(look), fontSize = 16.sp, fontWeight = FontWeight.Medium))
                    if (look.showUnit) Text(unit, style = TextStyle(color = muted(look), fontSize = 13.sp))
                }
                Numbers(digits, tileHeight, look)
            }
        } else {
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                if (caption != null) Text(caption, style = TextStyle(color = accentInk(look), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (look.showTitle) Text(title, maxLines = if (large) 2 else 1, style = TextStyle(color = ink(look), fontSize = if (large) 18.sp else 14.sp, fontWeight = FontWeight.Medium))
                Box(GlanceModifier.padding(vertical = 6.dp)) { Numbers(digits, tileHeight, look) }
                if (look.showUnit) Text(unit, style = TextStyle(color = muted(look), fontSize = if (large) 15.sp else 12.sp))
            }
        }
    }
}

private fun accentInk(l: Look) = if (l.template.onCard) dn(l.accent.bgDay, l.accent.digit) else ColorProvider(Color(0xFFFFFFFF))

private fun ringBitmap(context: Context, sizeDp: Float, progress: Float, arc: Int, track: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).roundToInt().coerceAtLeast(2)
    val stroke = 9f * density
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
private fun RingContent(context: Context, title: String, digits: String, unit: String, progress: Float, percent: String, date: String, look: Look, size: DpSize, open: Action) {
    val wide = size.width >= 200.dp && size.height < 200.dp
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val ringDp = if (wide) 110f else minOf(size.width.value, size.height.value) - 24f
    val bitmap = ringBitmap(context, ringDp, progress, (if (dark) look.accent.digit else look.accent.bgDay).toInt(), if (dark) 0x33FFFFFF else 0x1F000000)
    val ring: @Composable () -> Unit = {
        Box(GlanceModifier.size(ringDp.dp), contentAlignment = Alignment.Center) {
            Image(ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(ringDp.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Numbers(digits, if (wide) 32.dp else 30.dp, look)
                if (look.showUnit) Text(unit, style = TextStyle(color = muted(look), fontSize = 11.sp))
            }
        }
    }
    Box(GlanceModifier.fillMaxSize().background(bg(look)).padding(12.dp).clickable(open), contentAlignment = Alignment.Center) {
        if (wide) Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            ring()
            Box(GlanceModifier.width(14.dp)) {}
            Column(GlanceModifier.defaultWeight()) {
                if (look.showTitle) Text(title, maxLines = 2, style = TextStyle(color = ink(look), fontSize = 16.sp, fontWeight = FontWeight.Medium))
                Text(date, style = TextStyle(color = muted(look), fontSize = 12.sp))
                Text(percent, style = TextStyle(color = muted(look), fontSize = 12.sp))
            }
        } else ring()
    }
}

@Composable
private fun DateContent(title: String, digits: String, unit: String, month: String, day: String, weekday: String, look: Look, size: DpSize, open: Action) {
    val wide = size.width >= 200.dp && size.height < 200.dp
    val dateBlock: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(month, style = TextStyle(color = accentInk(look), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Box(GlanceModifier.padding(vertical = 4.dp)) { Numbers(day, if (wide) 56.dp else 50.dp, look) }
            Text(weekday, style = TextStyle(color = muted(look), fontSize = 12.sp))
        }
    }
    Box(GlanceModifier.fillMaxSize().background(bg(look)).padding(12.dp).clickable(open), contentAlignment = Alignment.Center) {
        if (wide) Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            dateBlock()
            Box(GlanceModifier.width(18.dp)) {}
            Column(GlanceModifier.defaultWeight()) {
                if (look.showTitle) Text(title, maxLines = 2, style = TextStyle(color = ink(look), fontSize = 16.sp, fontWeight = FontWeight.Medium))
                Box(GlanceModifier.padding(vertical = 4.dp)) { Numbers(digits, 32.dp, look) }
                if (look.showUnit) Text(unit, style = TextStyle(color = muted(look), fontSize = 12.sp))
            }
        } else Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            dateBlock()
            if (look.showUnit) Text("$digits $unit", style = TextStyle(color = muted(look), fontSize = 12.sp))
        }
    }
}

@Composable
private fun Segment(value: String, label: String, height: androidx.compose.ui.unit.Dp, look: Look) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Numbers(value, height, look)
        Text(label, style = TextStyle(color = muted(look), fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun TickerContent(
    title: String, days: String, hours: String, minutes: String, dLabel: String, hLabel: String, mLabel: String, updated: String,
    look: Look, size: DpSize, open: Action,
) {
    val wide = size.width >= 200.dp && size.height < 200.dp
    val colon = @Composable { Text(":", style = TextStyle(color = accentInk(look), fontSize = 20.sp, fontWeight = FontWeight.Bold)) }
    Box(GlanceModifier.fillMaxSize().background(bg(look)).padding(12.dp).clickable(open), contentAlignment = Alignment.Center) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            if (look.showTitle) Text(title, maxLines = 1, style = TextStyle(color = ink(look), fontSize = if (wide) 15.sp else 13.sp, fontWeight = FontWeight.Medium))
            Box(GlanceModifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Segment(days, dLabel, if (wide) 40.dp else 32.dp, look)
                    Box(GlanceModifier.padding(horizontal = 4.dp)) { colon() }
                    Segment(hours, hLabel, if (wide) 40.dp else 32.dp, look)
                    if (wide) {
                        Box(GlanceModifier.padding(horizontal = 4.dp)) { colon() }
                        Segment(minutes, mLabel, 40.dp, look)
                    }
                }
            }
            if (wide) Text(updated, style = TextStyle(color = muted(look), fontSize = 11.sp))
        }
    }
}

/** Digits read left to right in every language, like the in-app tiles. */
@Composable
private fun Numbers(digits: String, height: androidx.compose.ui.unit.Dp, look: Look) {
    Row {
        digits.forEach { ch ->
            if (look.template.onCard) {
                Box(GlanceModifier.width(height * 0.66f).height(height).background(TileBg), contentAlignment = Alignment.Center) {
                    Text(ch.toString(), style = TextStyle(color = ColorProvider(Color(look.accent.digit)), fontSize = (height.value * 0.66f).sp, fontWeight = FontWeight.Bold))
                }
                Box(GlanceModifier.width(3.dp)) {}
            } else {
                Text(ch.toString(), style = TextStyle(color = ink(look), fontSize = (height.value * 0.9f).sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

class FlipdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlipdayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefresher.scheduleNext(context)
    }
}
