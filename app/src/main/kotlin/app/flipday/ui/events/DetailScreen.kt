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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import app.flipday.core.NumeralFormatter
import app.flipday.ui.DetailViewModel
import app.flipday.ui.SettingsViewModel
import app.flipday.ui.components.FlipNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(factory: ViewModelProvider.Factory, eventId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, onOpenEvent: (Long) -> Unit) {
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
    val e = event

    // Event gone (deleted elsewhere or just now): leave quietly.
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(e) { if (e != null) seen = true else if (seen) onBack() }

    Scaffold(topBar = {
        TopAppBar(
            title = {},
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            actions = {
                if (e != null && e.archivedOn == null) Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menuOpen = false; onEdit(eventId) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_duplicate)) }, onClick = {
                            menuOpen = false
                            vm.duplicate(e, context.getString(R.string.duplicate_title, e.title), onOpenEvent)
                        })
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
        val description = context.resources.getQuantityString(R.plurals.cd_event, value.toInt(), e.title, value.toInt())
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(e.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            FlipNumber(digits, description, tileHeight = 96.dp)
            Text(stringResource(unit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(d(e.date), style = MaterialTheme.typography.bodyLarge)
            when (e.type) {
                EventType.AGE -> {
                    Text(stringResource(R.string.detail_next_birthday, d(EventCalculator.nextBirthday(e.date, today))), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.detail_turns, n(EventCalculator.turnsAge(e.date, today))), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                EventType.COUNTUP -> Text(stringResource(R.string.detail_started, d(e.date)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
