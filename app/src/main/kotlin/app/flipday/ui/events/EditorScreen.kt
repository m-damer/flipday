package app.flipday.ui.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.ReminderRule
import app.flipday.ui.EditorViewModel
import app.flipday.ui.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ReminderPresets = listOf(0, 1, 3, 7)

/** Add (eventId == null) and edit share one form, as in the design ("Same form with Delete"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(factory: ViewModelProvider.Factory, eventId: Long?, onClose: () -> Unit) {
    val vm: EditorViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val tag = locale.toLanguageTag()
    val today = LocalDate.now()

    var original by remember { mutableStateOf<Event?>(null) }
    var type by remember { mutableStateOf(EventType.COUNTDOWN) }
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(today.plusDays(7)) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var reminderDays by remember { mutableStateOf<Set<Int>?>(null) }
    var repeatYearly by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (eventId != null) vm.load(eventId)?.let { e ->
            original = e; type = e.type; title = e.title; date = e.date; time = e.time
            reminderDays = e.reminders.map { it.daysBefore }.toSet(); repeatYearly = e.repeatYearly
        }
    }
    // New events start with the default reminder from Settings (negative means off).
    val selectedReminders = reminderDays ?: if (settings.defaultReminderDaysBefore >= 0) setOf(settings.defaultReminderDaysBefore) else emptySet()

    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val pastCountdown = eventId == null && type == EventType.COUNTDOWN && date.isBefore(today)
    val hasReminders = type != EventType.COUNTUP

    fun save() {
        if (title.isBlank()) { titleError = true; return }
        if (pastCountdown) return
        val base = original
        val rules = if (hasReminders) selectedReminders.sorted().map { d ->
            ReminderRule(d, base?.reminders?.firstOrNull { it.daysBefore == d }?.at ?: LocalTime.of(9, 0))
        } else emptyList()
        val event = (base ?: Event(title = title.trim(), type = type, date = date)).copy(
            title = title.trim(), type = type, date = date,
            time = if (type == EventType.COUNTDOWN) time else null,
            startDate = base?.startDate ?: if (type == EventType.COUNTDOWN) today else null,
            reminders = rules,
            repeatYearly = type == EventType.COUNTDOWN && repeatYearly,
        )
        vm.save(event, onClose)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (eventId == null) R.string.new_event else R.string.edit_event)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
            actions = { TextButton(onClick = ::save) { Text(stringResource(R.string.save)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EventType.COUNTDOWN to R.string.type_countdown, EventType.AGE to R.string.type_birthday, EventType.COUNTUP to R.string.type_countup)
                    .forEach { (t, label) -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(label)) }) }
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it; titleError = false }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_title)) }, isError = titleError,
                supportingText = if (titleError) ({ Text(stringResource(R.string.err_title_empty)) }) else null,
            )
            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.field_date) + ": " + NumeralFormatter.format(date.format(fmt), settings.numerals, tag))
            }
            if (pastCountdown) {
                Text(stringResource(R.string.err_date_past), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.suggest_countup), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { type = EventType.COUNTUP }) { Text(stringResource(R.string.switch_to_countup)) }
            }
            if (type == EventType.COUNTDOWN) {
                OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.fillMaxWidth()) {
                    val shown = time?.let { NumeralFormatter.format(it.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), settings.numerals, tag) }
                    Text(stringResource(R.string.field_time) + ": " + (shown ?: stringResource(R.string.time_none)))
                }
                if (time != null) TextButton(onClick = { time = null }) { Text(stringResource(R.string.time_none)) }
            }
            if (hasReminders) {
                Text(stringResource(R.string.remind_title), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderPresets.forEach { d ->
                        FilterChip(
                            selected = d in selectedReminders,
                            onClick = { reminderDays = if (d in selectedReminders) selectedReminders - d else selectedReminders + d },
                            label = {
                                Text(if (d == 0) stringResource(R.string.remind_on_day)
                                else pluralStringResource(R.plurals.remind_days_before, d, NumeralFormatter.format(d.toString(), settings.numerals, tag)))
                            },
                        )
                    }
                }
            }
            if (type == EventType.COUNTDOWN) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.repeat_yearly), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = repeatYearly, onCheckedChange = { repeatYearly = it })
                }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (pickingTime) {
        val start = time ?: LocalTime.of(9, 0)
        val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current))
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(R.string.time_set)) },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); pickingTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
