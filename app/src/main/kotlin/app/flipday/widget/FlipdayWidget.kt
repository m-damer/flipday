package app.flipday.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceTheme
import app.flipday.core.NumeralMode
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

private val STRIP = DpSize(250.dp, 40.dp)
private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 250.dp)

/**
 * One launcher widget per design family ([defaultTemplate]). Which event shows, and which family, come from per-widget
 * state set in the setup screen; with no event chosen it follows the nearest one. Layout follows the widget size
 * (strip, small, medium, large). Refreshes at local midnight and on time changes: see [WidgetRefresher].
 */
abstract class FlipdayWidget(private val defaultTemplate: WidgetTemplate) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(STRIP, SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as FlipdayApp).container
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val events = container.repository.all().filter { it.archivedOn == null }
        val numerals = container.settings.settings.first().numerals
        val locale = context.resources.configuration.locales[0]

        provideContent {
            WidgetBody(context, appWidgetId, events, numerals, locale, defaultTemplate, currentState<Preferences>())
        }
    }

}

class BigNumberWidget : FlipdayWidget(WidgetTemplate.BIG)
class AgeWidget : FlipdayWidget(WidgetTemplate.AGE)
class CountUpWidget : FlipdayWidget(WidgetTemplate.COUNTUP)
class AgendaWidget : FlipdayWidget(WidgetTemplate.AGENDA)
class RingWidget : FlipdayWidget(WidgetTemplate.RING)
class DateWidget : FlipdayWidget(WidgetTemplate.DATE)
class TickerWidget : FlipdayWidget(WidgetTemplate.TICKER)
class YearWidget : FlipdayWidget(WidgetTemplate.YEAR)

/** All launcher widgets, for refreshes that must reach every family. */
internal val ALL_WIDGETS: List<GlanceAppWidget> get() = listOf(
    BigNumberWidget(), AgeWidget(), CountUpWidget(), AgendaWidget(), RingWidget(), DateWidget(), TickerWidget(), YearWidget(),
)

class BigNumberWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = BigNumberWidget() }
class AgeWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = AgeWidget() }
class CountUpWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = CountUpWidget() }
class AgendaWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = AgendaWidget() }
class RingWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = RingWidget() }
class DateWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = DateWidget() }
class TickerWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = TickerWidget() }
class YearWidgetReceiver : FlipdayReceiver() { override val glanceAppWidget: GlanceAppWidget = YearWidget() }

abstract class FlipdayReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefresher.scheduleNext(context)
    }
}

/**
 * The widget's content for one set of per-widget [prefs]. Shared by the home-screen widget and the studio preview,
 * so what the preview shows is what the launcher shows.
 */
@Composable
internal fun WidgetBody(
    context: Context, appWidgetId: Int, events: List<Event>, appNumerals: NumeralMode, locale: java.util.Locale,
    defaultTemplate: WidgetTemplate, prefs: Preferences,
) {
    val tag = locale.toLanguageTag()
    val numerals = appNumerals
                val chosen = prefs[WidgetKeys.EVENT_ID] ?: 0L
        val picked = prefs[WidgetKeys.TEMPLATE]?.let { WidgetTemplate.parse(it) } ?: defaultTemplate
        val accentChoice = WidgetAccent.parse(prefs[WidgetKeys.ACCENT])
        val style = remember(prefs) { styleFrom(prefs, context, appWidgetId, accentChoice) }
        // "Live" in the studio's Content tab turns any family into the live ticker (the year widget has no live form).
        val template = if (style.timeMode == TimeMode.LIVE && picked != WidgetTemplate.YEAR) WidgetTemplate.TICKER else picked
        val look = Look(template, accentChoice, prefs[WidgetKeys.SHOW_TITLE] ?: true, prefs[WidgetKeys.SHOW_UNIT] ?: true)
        val widgetNumerals = prefs[WidgetKeys.NUMERALS]?.let { runCatching { NumeralMode.valueOf(it) }.getOrNull() } ?: numerals
        val today = LocalDate.now()
        val size = sizeClass(LocalSize.current).let { if (it == WidgetSize.SMALL && !template.hasSmall) WidgetSize.MEDIUM else it }

        val sorted = upcomingSorted(events, today)
        val event: Event? = when {
            chosen > 0 -> events.firstOrNull { it.id == chosen }
            template == WidgetTemplate.AGE -> sorted.firstOrNull { it.type == EventType.AGE } ?: sorted.firstOrNull()
            template == WidgetTemplate.COUNTUP -> events.filter { it.type == EventType.COUNTUP }.maxByOrNull { it.date } ?: sorted.firstOrNull()
            else -> sorted.firstOrNull()
        }
        val others = sorted.filter { it.id != event?.id }
        val c = WCtx(context, today, LocalDateTime.now(), widgetNumerals, locale, tag, others, style.timeMode)
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java))

        GlanceTheme {
            CompositionLocalProvider(LocalStyle provides style, LocalPalette provides paletteFor(style)) {
            when {
                template == WidgetTemplate.YEAR -> YearLayout(c, event, look, size, openApp)
                chosen > 0 && event == null -> StateCard(R.drawable.ic_state_trash, context.getString(R.string.wd_deleted), context.getString(R.string.wd_deleted_hint), configureAction(context, appWidgetId), look)
                event == null && template == WidgetTemplate.AGENDA -> StateCard(R.drawable.ic_state_calendar, context.getString(R.string.wd_agenda_none), context.getString(R.string.wd_agenda_none_hint), openApp, look, context.getString(R.string.wd_agenda_eyebrow))
                event == null -> StateCard(R.drawable.ic_state_plus, context.getString(R.string.wd_noevents), context.getString(R.string.wd_noevents_hint), openApp, look)
                else -> {
                    val open = actionStartActivity(
                        Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, event.id).setData(Uri.parse("flipday://event/${event.id}")),
                    )
                    when (template) {
                        WidgetTemplate.AGE -> if (event.type == EventType.AGE) AgeLayout(c, event, look, size, open) else BigLayout(c, event, look, size, open)
                        WidgetTemplate.COUNTUP -> if (event.type == EventType.COUNTUP) CountUpLayout(c, event, look, size, open) else BigLayout(c, event, look, size, open)
                        WidgetTemplate.AGENDA -> AgendaLayout(c, event, look, size, open)
                        WidgetTemplate.RING -> RingLayout(c, event, look, size, open)
                        WidgetTemplate.DATE -> DateLayout(c, event, look, size, open)
                        WidgetTemplate.TICKER -> TickerLayout(c, event, look, size, open)
                        else -> BigLayout(c, event, look, size, open)
                    }
                }
            }
            }
        }
}

private fun configureAction(context: Context, appWidgetId: Int): Action = actionStartActivity(
    Intent(context, WidgetConfigActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId).setData(Uri.parse("flipday://widget/$appWidgetId")),
)
