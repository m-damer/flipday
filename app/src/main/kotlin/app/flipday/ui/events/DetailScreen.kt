package app.flipday.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import app.flipday.ui.theme.LocalFlap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.ArchivePolicy
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventPhase
import app.flipday.core.EventType
import app.flipday.core.PeriodPhase
import app.flipday.core.NumeralFormatter
import app.flipday.ui.DetailViewModel
import app.flipday.ui.SettingsViewModel
import app.flipday.ui.components.FlipNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(factory: ViewModelProvider.Factory, eventId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, onOpenEvent: (Long) -> Unit, showBack: Boolean = true) {
    val vm: DetailViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsState()
    val event by remember(eventId) { vm.event(eventId) }.collectAsState(initial = null)
    val locale = LocalConfiguration.current.locales[0]
    val tag = locale.toLanguageTag()
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    fun n(v: Any) = NumeralFormatter.format(v.toString(), settings.numerals, tag)
    fun d(date: LocalDate) = NumeralFormatter.format(date.format(fmt), settings.numerals, tag)

    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var offerRepeat by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val flap = LocalFlap.current
    val e = event

    // Event gone (deleted elsewhere or just now): leave quietly.
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(e) { if (e != null) seen = true else if (seen) onBack() }

    Scaffold(topBar = {
        TopAppBar(
            title = {},
            navigationIcon = { if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            actions = {
                if (e != null && e.archivedOn == null) Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menuOpen = false; onEdit(eventId) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_duplicate)) }, onClick = {
                            menuOpen = false
                            vm.duplicate(e, context.getString(R.string.duplicate_title, e.title), onOpenEvent)
                        })
                        if (e.type == EventType.COUNTUP) DropdownMenuItem(text = { Text(stringResource(R.string.action_mark_ended)) }, onClick = { menuOpen = false; confirmEnd = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_share)) }, onClick = { menuOpen = false; sharing = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_archive)) }, onClick = { menuOpen = false; vm.archive(e); onBack() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            },
        )
    }) { padding ->
        if (e == null) return@Scaffold
        val (value, unit) = headline(e, today)
        val digits = n(value.toString().padStart(2, '0'))
        val isPeriod = e.type == EventType.PERIOD
        val subtitle = if (isPeriod) periodSubtitle(context, e, today, settings.numerals, locale) else ""
        val description = if (isPeriod) "${e.title}, $subtitle, $value ${context.getString(unit)}"
        else context.resources.getQuantityString(R.plurals.cd_event, value.toInt(), e.title, value.toInt())
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(e.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            FlipNumber(digits, description, tileHeight = 96.dp)
            Text(stringResource(unit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (!isPeriod) Text(d(e.date), style = MaterialTheme.typography.bodyLarge)
            when (e.type) {
                EventType.AGE -> {
                    Text(stringResource(R.string.detail_next_birthday, d(EventCalculator.nextBirthday(e.date, today))), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.detail_turns, n(EventCalculator.turnsAge(e.date, today))), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                EventType.COUNTUP -> Text(stringResource(R.string.detail_started, d(e.date)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                EventType.PERIOD -> {
                    val end = e.endDate!!
                    val st = EventCalculator.period(e.date, end, today)
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(progress = { st.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    if (st.phase == PeriodPhase.ONGOING) Text(
                        stringResource(R.string.period_progress, n(st.dayNumber), n(st.totalDays)),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) else if (st.phase == PeriodPhase.ENDED) Text(
                        stringResource(R.string.period_completed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_from), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(d(e.date), style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_to), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(d(end), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                EventType.COUNTDOWN -> Unit
            }
            if (e.archivedOn != null) {
                Text(stringResource(R.string.archived_on, d(e.archivedOn!!)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { vm.restore(e) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore)) }
                OutlinedButton(
                    onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete_forever)) }
            } else if (ArchivePolicy.shouldOfferRepeat(e, today)) {
                TextButton(onClick = { offerRepeat = true }) { Text(stringResource(R.string.repeat_title)) }
            }
        }

        if (confirmDelete) AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body, e.title)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(e, onBack) }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
        if (sharing) AlertDialog(
            onDismissRequest = { sharing = false },
            title = { Text(stringResource(R.string.share_title)) },
            text = {
                Column {
                    listOf(ShareFormat.SQUARE to R.string.share_square, ShareFormat.STORY to R.string.share_story, ShareFormat.WIDE to R.string.share_wide).forEach { (format, label) ->
                        TextButton(onClick = {
                            sharing = false
                            val subtitle = if (e.type == EventType.PERIOD) periodSubtitle(context, e, today, settings.numerals, locale) else d(e.date)
                            val unitText = context.getString(unit)
                            val palette = SharePalette(colors.surface.toArgb(), colors.onSurface.toArgb(), colors.onSurfaceVariant.toArgb(), colors.primary.toArgb(), flap.tile.toArgb(), flap.digit.toArgb())
                            val caption = if (e.type == EventType.COUNTDOWN) context.getString(R.string.share_caption_due) else null
                            scope.launch {
                                val ok = runCatching {
                                    val bitmap = withContext(Dispatchers.Default) {
                                        ShareCard.render(context, format, e.title, digits, unitText, subtitle, caption, context.getString(R.string.app_name), palette)
                                    }
                                    ShareCard.share(context, bitmap, context.getString(R.string.share_text, e.title, digits, unitText), context.getString(R.string.share_chooser))
                                }.isSuccess
                                if (!ok) android.widget.Toast.makeText(context, R.string.share_failed, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { sharing = false }) { Text(stringResource(R.string.cancel)) } },
        )
        if (confirmEnd) {
            val days = EventCalculator.daysSince(e.date, today).coerceAtLeast(0).toInt()
            AlertDialog(
                onDismissRequest = { confirmEnd = false },
                title = { Text(stringResource(R.string.mark_ended_title, e.title)) },
                text = { Text(pluralStringResource(R.plurals.mark_ended_body, days, n(days))) },
                confirmButton = { TextButton(onClick = { confirmEnd = false; vm.markEnded(e) }) { Text(stringResource(R.string.mark_ended_confirm)) } },
                dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        if (offerRepeat) AlertDialog(
            onDismissRequest = { offerRepeat = false },
            title = { Text(stringResource(R.string.repeat_title)) },
            text = { Text(stringResource(R.string.repeat_body, e.title, d(e.date.plusYears(1)))) },
            confirmButton = { TextButton(onClick = { offerRepeat = false; vm.repeatNextYear(e) }) { Text(stringResource(R.string.repeat_yes)) } },
            dismissButton = { TextButton(onClick = { offerRepeat = false }) { Text(stringResource(R.string.repeat_no)) } },
        )
    }
}

/** Big number and its unit label for the hero tiles. Same rules as the list rows. */
internal fun headline(event: Event, today: LocalDate): Pair<Long, Int> = when (event.type) {
    EventType.COUNTDOWN -> when (EventCalculator.phase(event.date, today)) {
        EventPhase.TODAY -> 0L to R.string.state_today
        EventPhase.DONE -> 0L to R.string.state_done
        EventPhase.UPCOMING -> EventCalculator.daysUntil(event.date, today) to R.string.label_days
    }
    EventType.AGE -> EventCalculator.age(event.date, today).years.toLong() to R.string.label_years_old
    EventType.COUNTUP -> EventCalculator.daysSince(event.date, today) to R.string.label_since
    EventType.PERIOD -> {
        val s = EventCalculator.period(event.date, event.endDate!!, today)
        when (s.phase) {
            PeriodPhase.UPCOMING -> s.daysToStart to R.string.label_to_start
            PeriodPhase.ONGOING -> s.daysLeft to R.string.label_days_left
            PeriodPhase.ENDED -> s.totalDays to R.string.label_days_long
        }
    }
}
