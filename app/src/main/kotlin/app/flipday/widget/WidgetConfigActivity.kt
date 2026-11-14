package app.flipday.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.ReminderPlanner
import app.flipday.data.AppSettings
import app.flipday.ui.events.headline
import java.time.LocalDate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.ui.theme.FlipdayTheme
import kotlinx.coroutines.launch

/** "Setup on drop": opens when a widget is added (and when a widget with a missing event is tapped). */
class WidgetConfigActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Backing out must cancel, so the launcher removes the widget that was just dropped.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val container = (application as FlipdayApp).container

        setContent {
            FlipdayTheme(dark = isSystemInDarkTheme()) {
                val events by container.repository.observeEvents().collectAsState(initial = emptyList())
                val settings by container.settings.settings.collectAsState(initial = AppSettings())
                val active = events.filter { it.archivedOn == null }
                var eventId by remember { mutableLongStateOf(0L) }
                var template by remember { mutableStateOf(providerTemplate(appWidgetId)) }
                var accent by remember { mutableStateOf(WidgetAccent.TEAL) }
                var showTitle by remember { mutableStateOf(true) }
                var showUnit by remember { mutableStateOf(true) }
                // Reconfiguring an existing widget starts from its saved choices.
                LaunchedEffect(Unit) {
                    runCatching {
                        val gid = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                        val saved = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, gid)
                        saved[WidgetKeys.EVENT_ID]?.let { eventId = it }
                        saved[WidgetKeys.TEMPLATE]?.let { template = WidgetTemplate.parse(it) }
                        saved[WidgetKeys.ACCENT]?.let { accent = WidgetAccent.parse(it) }
                        saved[WidgetKeys.SHOW_TITLE]?.let { showTitle = it }
                        saved[WidgetKeys.SHOW_UNIT]?.let { showUnit = it }
                    }
                }
                val today = LocalDate.now()
                val shown = if (eventId > 0) active.firstOrNull { it.id == eventId } else active.filter { it.type != EventType.COUNTUP }
                    .mapNotNull { e -> ReminderPlanner.anchorDate(e, today)?.takeIf { !it.isBefore(today) }?.let { e to it } }.minByOrNull { it.second }?.first
                val tag = resources.configuration.locales[0].toLanguageTag()
                val (value, unitRes) = shown?.let { headline(it, today) } ?: (24L to R.string.label_days)
                val previewTitle = shown?.title ?: stringResource(R.string.widget_sample_title)
                val digits = NumeralFormatter.format(value.toString().padStart(2, '0'), settings.numerals, tag)
                val unitText = stringResource(unitRes)

                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.widget_setup_title)) },
                        navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
                    )
                }, bottomBar = {
                    Button(onClick = {
                        lifecycleScope.launch {
                            val gid = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(this@WidgetConfigActivity, gid) { p ->
                                p[WidgetKeys.EVENT_ID] = eventId
                                p[WidgetKeys.TEMPLATE] = template.name
                                p[WidgetKeys.ACCENT] = accent.name
                                p[WidgetKeys.SHOW_TITLE] = showTitle
                                p[WidgetKeys.SHOW_UNIT] = showUnit
                            }
                            widgetFor(this@WidgetConfigActivity, gid)?.update(this@WidgetConfigActivity, gid)
                            WidgetRefresher.scheduleTickerIfNeeded(applicationContext)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text(stringResource(R.string.save)) }
                }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                WidgetPreview(previewTitle, digits, unitText, "$previewTitle, $digits $unitText", template, accent, showTitle, showUnit, Modifier.width(170.dp))
                            }
                        }
                        item { Label(stringResource(R.string.widget_content)) }
                        item { ChoiceRow(stringResource(R.string.widget_nearest), eventId == 0L) { eventId = 0L } }
                        items(active, key = { it.id }) { e -> ChoiceRow(e.title, eventId == e.id) { eventId = e.id } }
                        item { Label(stringResource(R.string.widget_style)) }
                        item {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetTemplate.entries.map { it to templateName(it) }.forEach { (t, label) -> FilterChip(selected = template == t, onClick = { template = t }, label = { Text(stringResource(label)) }) }
                            }
                        }
                        item { Label(stringResource(R.string.widget_colors)) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                WidgetAccent.entries.forEach { a ->
                                    val name = stringResource(accentName(a))
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape).background(Color(a.bgDay))
                                            .border(if (a == accent) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            .selectable(selected = a == accent, role = Role.RadioButton, onClick = { accent = a })
                                            .semantics { contentDescription = name },
                                    )
                                }
                            }
                        }
                        item { Label(stringResource(R.string.widget_text)) }
                        item { SwitchRow(stringResource(R.string.widget_show_title), showTitle) { showTitle = it } }
                        item { SwitchRow(stringResource(R.string.widget_show_unit), showUnit) { showUnit = it } }
                    }
                }
            }
        }
    }
}

/** The family of the launcher widget that was dropped: the receiver that owns [appWidgetId]. */
private fun AppCompatActivity.providerTemplate(appWidgetId: Int): WidgetTemplate {
    val cls = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className ?: return WidgetDraft.template
    return when (cls.substringAfterLast('.')) {
        "AgeWidgetReceiver" -> WidgetTemplate.AGE; "CountUpWidgetReceiver" -> WidgetTemplate.COUNTUP
        "AgendaWidgetReceiver" -> WidgetTemplate.AGENDA; "RingWidgetReceiver" -> WidgetTemplate.RING
        "DateWidgetReceiver" -> WidgetTemplate.DATE; "TickerWidgetReceiver" -> WidgetTemplate.TICKER
        "YearWidgetReceiver" -> WidgetTemplate.YEAR; else -> WidgetTemplate.BIG
    }
}

private suspend fun widgetFor(context: android.content.Context, gid: androidx.glance.GlanceId): androidx.glance.appwidget.GlanceAppWidget? {
    val manager = GlanceAppWidgetManager(context)
    return ALL_WIDGETS.firstOrNull { w -> manager.getGlanceIds(w.javaClass).contains(gid) }
}

internal fun templateName(t: WidgetTemplate) = when (t) {
    WidgetTemplate.BIG -> R.string.style_big; WidgetTemplate.AGE -> R.string.style_age; WidgetTemplate.COUNTUP -> R.string.style_countup
    WidgetTemplate.AGENDA -> R.string.style_agenda; WidgetTemplate.RING -> R.string.style_ring; WidgetTemplate.DATE -> R.string.style_date
    WidgetTemplate.TICKER -> R.string.style_ticker; WidgetTemplate.YEAR -> R.string.style_year
}

internal fun templateDesc(t: WidgetTemplate) = when (t) {
    WidgetTemplate.BIG -> R.string.style_big_desc; WidgetTemplate.AGE -> R.string.style_age_desc; WidgetTemplate.COUNTUP -> R.string.style_countup_desc
    WidgetTemplate.AGENDA -> R.string.style_agenda_desc; WidgetTemplate.RING -> R.string.style_ring_desc; WidgetTemplate.DATE -> R.string.style_date_desc
    WidgetTemplate.TICKER -> R.string.style_ticker_desc; WidgetTemplate.YEAR -> R.string.style_year_desc
}

private fun accentName(a: WidgetAccent) = when (a) {
    WidgetAccent.TEAL -> R.string.accent_teal; WidgetAccent.INDIGO -> R.string.accent_indigo
    WidgetAccent.ROSE -> R.string.accent_rose; WidgetAccent.AMBER -> R.string.accent_amber
}

@Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
