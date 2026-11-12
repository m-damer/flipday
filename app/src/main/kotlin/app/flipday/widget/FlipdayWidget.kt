package app.flipday.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
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
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.ReminderPlanner
import app.flipday.ui.MainActivity
import app.flipday.ui.events.headline
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import androidx.glance.color.ColorProvider as DayNight

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 250.dp)

// Glance cannot read the Compose theme, so widget colours live here (allowed by CLAUDE.md, phase 1 note).
private fun dn(day: Long, night: Long) = DayNight(Color(day), Color(night))
private val TileBg = ColorProvider(Color(0xFF050506))
private val Mint = ColorProvider(Color(0xFF5EE0C4))

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
            val today = LocalDate.now()
            val size = LocalSize.current

            val event: Event? = if (chosen > 0) events.firstOrNull { it.id == chosen } else nearest(events, today)
            when {
                chosen > 0 && event == null -> Message(context.getString(R.string.widget_choose), template, configureAction(context, appWidgetId))
                event == null -> Message(context.getString(R.string.widget_empty), template, actionStartActivity(Intent(context, MainActivity::class.java)))
                else -> {
                    val (value, unitRes) = headline(event, today)
                    val digits = NumeralFormatter.format(value.toString().padStart(2, '0'), numerals, tag)
                    val open = actionStartActivity(
                        Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, event.id).setData(Uri.parse("flipday://event/${event.id}")),
                    )
                    Content(event.title, digits, context.getString(unitRes), template, size, open)
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

private fun bg(t: WidgetTemplate) = if (t == WidgetTemplate.FLAP) dn(0xFFFFFFFF, 0xFF171719) else dn(0xFF0F766E, 0xFF0B4F49)
private fun ink(t: WidgetTemplate) = if (t == WidgetTemplate.FLAP) dn(0xFF1A1A1C, 0xFFF2EFE6) else ColorProvider(Color(0xFFFFFFFF))
private fun muted(t: WidgetTemplate) = if (t == WidgetTemplate.FLAP) dn(0xFF625E54, 0xFFB8B4A8) else ColorProvider(Color(0xFFD7EFEA))

@Composable
private fun Message(text: String, template: WidgetTemplate, action: Action) {
    Box(GlanceModifier.fillMaxSize().background(bg(template)).padding(12.dp).clickable(action), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = ink(template), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun Content(title: String, digits: String, unit: String, template: WidgetTemplate, size: DpSize, open: Action) {
    val wide = size.width >= 200.dp && size.height < 200.dp
    val large = size.height >= 200.dp
    val tileHeight = when { large -> 84.dp; wide -> 56.dp; else -> 52.dp }
    Box(GlanceModifier.fillMaxSize().background(bg(template)).padding(12.dp).clickable(open), contentAlignment = Alignment.Center) {
        if (wide) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(title, maxLines = 2, style = TextStyle(color = ink(template), fontSize = 16.sp, fontWeight = FontWeight.Medium))
                    Text(unit, style = TextStyle(color = muted(template), fontSize = 13.sp))
                }
                Numbers(digits, tileHeight, template)
            }
        } else {
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Text(title, maxLines = if (large) 2 else 1, style = TextStyle(color = ink(template), fontSize = if (large) 18.sp else 14.sp, fontWeight = FontWeight.Medium))
                Box(GlanceModifier.padding(vertical = 6.dp)) { Numbers(digits, tileHeight, template) }
                Text(unit, style = TextStyle(color = muted(template), fontSize = if (large) 15.sp else 12.sp))
            }
        }
    }
}

/** Digits read left to right in every language, like the in-app tiles. */
@Composable
private fun Numbers(digits: String, height: androidx.compose.ui.unit.Dp, template: WidgetTemplate) {
    Row {
        digits.forEach { ch ->
            if (template == WidgetTemplate.FLAP) {
                Box(GlanceModifier.width(height * 0.66f).height(height).background(TileBg), contentAlignment = Alignment.Center) {
                    Text(ch.toString(), style = TextStyle(color = Mint, fontSize = (height.value * 0.66f).sp, fontWeight = FontWeight.Bold))
                }
                Box(GlanceModifier.width(3.dp)) {}
            } else {
                Text(ch.toString(), style = TextStyle(color = ink(template), fontSize = (height.value * 0.9f).sp, fontWeight = FontWeight.Bold))
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
