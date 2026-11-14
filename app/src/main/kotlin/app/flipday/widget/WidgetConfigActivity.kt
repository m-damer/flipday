package app.flipday.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.core.NumeralMode
import app.flipday.ui.theme.FlipdayTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the studio edits, kept in one place so it can be turned into widget state and into a preview. */
private class Draft {
    var eventId by mutableLongStateOf(0L)
    var template by mutableStateOf(WidgetTemplate.BIG)
    var accent by mutableStateOf(WidgetAccent.TEAL)
    var showTitle by mutableStateOf(true)
    var showUnit by mutableStateOf(true)
    var showDate by mutableStateOf(true)
    var showProgress by mutableStateOf(true)
    var dynamic by mutableStateOf(false)
    var bg by mutableStateOf(BgMode.SOLID)
    var radius by mutableIntStateOf(WidgetStyle.SYSTEM_RADIUS)
    var customRadius by mutableStateOf(false)
    var numberSize by mutableStateOf(NumberSize.MEDIUM)
    var font by mutableStateOf(FontChoice.SANS)
    var numerals by mutableStateOf<NumeralMode?>(null)
    var timeMode by mutableStateOf(TimeMode.DAYS)
    var photoVersion by mutableIntStateOf(0)

    fun toPrefs(): Preferences = mutablePreferencesOf(
        WidgetKeys.EVENT_ID to eventId, WidgetKeys.TEMPLATE to template.name, WidgetKeys.ACCENT to accent.name,
        WidgetKeys.SHOW_TITLE to showTitle, WidgetKeys.SHOW_UNIT to showUnit, WidgetKeys.SHOW_DATE to showDate, WidgetKeys.SHOW_PROGRESS to showProgress,
        WidgetKeys.DYNAMIC to dynamic, WidgetKeys.BG_MODE to bg.name, WidgetKeys.RADIUS to (if (customRadius) radius else WidgetStyle.SYSTEM_RADIUS),
        WidgetKeys.NUMBER_SIZE to numberSize.name, WidgetKeys.FONT to font.name, WidgetKeys.TIME_MODE to timeMode.name,
    ).also { p -> numerals?.let { p[WidgetKeys.NUMERALS] = it.name } }

    fun load(saved: Preferences) {
        saved[WidgetKeys.EVENT_ID]?.let { eventId = it }
        saved[WidgetKeys.TEMPLATE]?.let { template = WidgetTemplate.parse(it) }
        saved[WidgetKeys.ACCENT]?.let { accent = WidgetAccent.parse(it) }
        saved[WidgetKeys.SHOW_TITLE]?.let { showTitle = it }
        saved[WidgetKeys.SHOW_UNIT]?.let { showUnit = it }
        saved[WidgetKeys.SHOW_DATE]?.let { showDate = it }
        saved[WidgetKeys.SHOW_PROGRESS]?.let { showProgress = it }
        saved[WidgetKeys.DYNAMIC]?.let { dynamic = it }
        saved[WidgetKeys.BG_MODE]?.let { m -> BgMode.entries.firstOrNull { it.name == m }?.let { bg = it } }
        saved[WidgetKeys.RADIUS]?.let { radius = it; customRadius = it != WidgetStyle.SYSTEM_RADIUS }
        saved[WidgetKeys.NUMBER_SIZE]?.let { m -> NumberSize.entries.firstOrNull { it.name == m }?.let { numberSize = it } }
        saved[WidgetKeys.FONT]?.let { m -> FontChoice.entries.firstOrNull { it.name == m }?.let { font = it } }
        saved[WidgetKeys.NUMERALS]?.let { m -> numerals = runCatching { NumeralMode.valueOf(m) }.getOrNull() }
        saved[WidgetKeys.TIME_MODE]?.let { m -> TimeMode.entries.firstOrNull { it.name == m }?.let { timeMode = it } }
    }
}

/** "Setup on drop" and the studio: opens when a widget is added, when a widget with a missing event is tapped, and when a widget is reconfigured. */
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
                val active = events.filter { it.archivedOn == null }
                val draft = remember { Draft().also { it.template = providerTemplate(appWidgetId) } }
                var tab by remember { mutableIntStateOf(0) }
                var sizeChoice by remember { mutableStateOf(WidgetSize.MEDIUM) }
                // Reconfiguring an existing widget starts from its saved choices.
                LaunchedEffect(Unit) {
                    runCatching {
                        val gid = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                        draft.load(getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, gid))
                    }
                }
                val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { savePhoto(this@WidgetConfigActivity, appWidgetId, it) }
                            }
                        }
                        draft.bg = BgMode.PHOTO; draft.photoVersion++
                    }
                }

                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.studio_title)) },
                        navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
                    )
                }, bottomBar = {
                    Button(onClick = {
                        lifecycleScope.launch {
                            val gid = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                            val prefs = draft.toPrefs()
                            updateAppWidgetState(this@WidgetConfigActivity, gid) { p -> p.clear(); prefs.asMap().forEach { (k, v) -> @Suppress("UNCHECKED_CAST") p[k as Preferences.Key<Any>] = v } }
                            widgetFor(this@WidgetConfigActivity, gid)?.update(this@WidgetConfigActivity, gid)
                            WidgetRefresher.scheduleTickerIfNeeded(applicationContext)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text(stringResource(R.string.studio_save)) }
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        // Real preview: the same layouts the home screen shows, at the chosen size.
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val sizes = WidgetSize.entries.filter { it != WidgetSize.SMALL || draft.template.hasSmall }
                            if (sizeChoice !in sizes) sizeChoice = WidgetSize.MEDIUM
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sizes.forEach { s -> FilterChip(selected = sizeChoice == s, onClick = { sizeChoice = s }, label = { Text(stringResource(sizeLabel(s))) }) }
                            }
                            Box(Modifier.padding(vertical = 8.dp)) { LivePreview(draft, appWidgetId, sizeChoice) }
                        }
                        TabRow(selectedTabIndex = tab) {
                            listOf(R.string.studio_tab_template, R.string.studio_tab_colors, R.string.studio_tab_text, R.string.studio_tab_content).forEachIndexed { i, r ->
                                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(r)) })
                            }
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                            when (tab) {
                                0 -> TemplateTab(draft, active)
                                1 -> ColorsTab(draft) { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                2 -> TextTab(draft)
                                else -> ContentTab(draft)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sizeLabel(s: WidgetSize) = when (s) {
    WidgetSize.STRIP -> R.string.size_strip; WidgetSize.SMALL -> R.string.size_small
    WidgetSize.MEDIUM -> R.string.size_medium; WidgetSize.LARGE -> R.string.size_large
}

/** Launcher-cell sizes from the design: strip 4x1 (338x72), small 2x2 (158), medium 4x2 (338x158), large 4x4 (338x354), scaled to fit a phone. */
private fun previewSize(s: WidgetSize) = when (s) {
    WidgetSize.STRIP -> DpSize(250.dp, 40.dp); WidgetSize.SMALL -> DpSize(110.dp, 110.dp)
    WidgetSize.MEDIUM -> DpSize(250.dp, 110.dp); WidgetSize.LARGE -> DpSize(250.dp, 250.dp)
}

@OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun LivePreview(draft: Draft, appWidgetId: Int, size: WidgetSize) {
    val context = LocalContext.current
    val container = (context.applicationContext as FlipdayApp).container
    val events by container.repository.observeEvents().collectAsState(initial = emptyList())
    val appSettings by container.settings.settings.collectAsState(initial = app.flipday.data.AppSettings())
    val prefs = draft.toPrefs()
    val d = previewSize(size)
    var views by remember { mutableStateOf<android.widget.RemoteViews?>(null) }
    // The photo is read from the widget's own file, so a new photo (photoVersion) must redraw too.
    LaunchedEffect(prefs, size, draft.photoVersion, events, appSettings) {
        val locale = context.resources.configuration.locales[0]
        views = runCatching {
            androidx.glance.appwidget.GlanceRemoteViews().compose(context = context, size = d, state = prefs) {
                WidgetBody(context, appWidgetId, events.filter { it.archivedOn == null }, appSettings.numerals, locale, WidgetTemplate.BIG, prefs)
            }.remoteViews
        }.getOrNull()
    }
    AndroidView(
        factory = { FrameLayout(it) },
        update = { frame -> views?.let { rv -> frame.removeAllViews(); frame.addView(rv.apply(frame.context, frame)) } },
        modifier = Modifier.size(d.width, d.height).clip(MaterialTheme.shapes.large),
    )
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

@Composable
private fun <T> Chips(items: List<T>, selected: T, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { t -> FilterChip(selected = t == selected, onClick = { onPick(t) }, label = { Text(label(t)) }) }
    }
}

@Composable
private fun TemplateTab(d: Draft, events: List<app.flipday.core.Event>) {
    SectionLabel(stringResource(R.string.widget_style))
    WidgetTemplate.entries.forEach { t ->
        Surface(
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .border(if (d.template == t) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                .selectable(selected = d.template == t, role = Role.RadioButton, onClick = { d.template = t }),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(templateName(t)), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(templateDesc(t)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    SectionLabel(stringResource(R.string.studio_event))
    ChoiceRow(stringResource(R.string.widget_nearest), d.eventId == 0L) { d.eventId = 0L }
    events.forEach { e -> ChoiceRow(e.title, d.eventId == e.id) { d.eventId = e.id } }
}

@Composable
private fun ColorsTab(d: Draft, pickPhoto: () -> Unit) {
    val canDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (canDynamic) SwitchRow(stringResource(R.string.studio_dynamic), d.dynamic) { d.dynamic = it }
    SectionLabel(stringResource(R.string.studio_background))
    Chips(BgMode.entries, d.bg, { stringResource(bgLabel(it)) }) { d.bg = it }
    if (d.bg == BgMode.PHOTO) OutlinedButton(onClick = pickPhoto, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.studio_pick_photo)) }
    SectionLabel(stringResource(R.string.studio_accent))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WidgetAccent.entries.forEach { a ->
            val name = stringResource(accentName(a))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color(a.bgDay))
                    .border(if (a == d.accent) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .selectable(selected = a == d.accent, role = Role.RadioButton, onClick = { d.accent = a })
                    .semantics { contentDescription = name },
            )
        }
    }
    SectionLabel(stringResource(R.string.studio_radius))
    Chips(listOf(false, true), d.customRadius, { stringResource(if (it) R.string.radius_custom else R.string.radius_system) }) { d.customRadius = it }
    if (d.customRadius) Slider(value = d.radius.toFloat(), onValueChange = { d.radius = it.toInt() }, valueRange = 0f..32f)
}

private fun bgLabel(m: BgMode) = when (m) {
    BgMode.SOLID -> R.string.bg_solid; BgMode.GRADIENT -> R.string.bg_gradient; BgMode.PHOTO -> R.string.bg_photo; BgMode.CLEAR -> R.string.bg_clear
}

@Composable
private fun TextTab(d: Draft) {
    SectionLabel(stringResource(R.string.studio_font))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FontChoice.entries.forEach { f ->
            FilterChip(
                selected = d.font == f, onClick = { d.font = f },
                label = { Text(stringResource(fontLabel(f)), fontFamily = when (f) { FontChoice.SANS -> FontFamily.SansSerif; FontChoice.SERIF -> FontFamily.Serif; FontChoice.MONO -> FontFamily.Monospace }) },
            )
        }
    }
    SectionLabel(stringResource(R.string.studio_number_size))
    Chips(NumberSize.entries, d.numberSize, { stringResource(numberSizeLabel(it)) }) { d.numberSize = it }
    SectionLabel(stringResource(R.string.studio_numerals))
    Chips(listOf<NumeralMode?>(null, NumeralMode.WESTERN, NumeralMode.ARABIC_INDIC), d.numerals, {
        stringResource(when (it) { null -> R.string.studio_num_match; NumeralMode.WESTERN -> R.string.studio_num_western; else -> R.string.studio_num_arabic })
    }) { d.numerals = it }
}

private fun fontLabel(f: FontChoice) = when (f) { FontChoice.SANS -> R.string.font_sans; FontChoice.SERIF -> R.string.font_serif; FontChoice.MONO -> R.string.font_mono }
private fun numberSizeLabel(n: NumberSize) = when (n) {
    NumberSize.SMALL -> R.string.size_small; NumberSize.MEDIUM -> R.string.size_medium; NumberSize.LARGE -> R.string.size_large; NumberSize.XL -> R.string.size_xl
}

@Composable
private fun ContentTab(d: Draft) {
    SwitchRow(stringResource(R.string.widget_show_title), d.showTitle) { d.showTitle = it }
    SwitchRow(stringResource(R.string.studio_show_date), d.showDate) { d.showDate = it }
    SwitchRow(stringResource(R.string.studio_show_progress), d.showProgress) { d.showProgress = it }
    SwitchRow(stringResource(R.string.widget_show_unit), d.showUnit) { d.showUnit = it }
    SectionLabel(stringResource(R.string.studio_time_as))
    Chips(TimeMode.entries, d.timeMode, {
        stringResource(when (it) { TimeMode.DAYS -> R.string.time_days; TimeMode.WEEKS -> R.string.time_weeks; TimeMode.YMD -> R.string.time_ymd; TimeMode.LIVE -> R.string.time_live })
    }) { d.timeMode = it }
    if (d.timeMode == TimeMode.LIVE) Text(stringResource(R.string.studio_time_live_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
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

private suspend fun widgetFor(context: android.content.Context, gid: GlanceId): GlanceAppWidget? {
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
