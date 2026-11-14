package app.flipday.ui.events

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.material3.ButtonDefaults
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
import app.flipday.reminders.ReminderScheduler
import app.flipday.ui.EditorViewModel
import app.flipday.ui.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ReminderPresets = listOf(0, 1, 3, 7)
private val PeriodReminderPresets = listOf(0, 1, 7)

/** Add (eventId == null) and edit share one form, as in the design ("Same form with Delete"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(factory: ViewModelProvider.Factory, eventId: Long?, onClose: () -> Unit, preset: String? = null) {
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
    var endDate by remember { mutableStateOf(today.plusDays(7)) }
    /** 0 = closed, 1 = start date, 2 = end date. */
    var pickingDate by remember { mutableStateOf(0) }
    var pickingTime by remember { mutableStateOf(false) }
    var pendingEvent by remember { mutableStateOf<Event?>(null) }
    var reminderTime by remember { mutableStateOf(ReminderScheduler.DEFAULT_TIME) }
    var pickingReminderTime by remember { mutableStateOf(false) }
    var exactEvent by remember { mutableStateOf<Event?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        pendingEvent?.let { vm.save(it, onClose) }
    }

    // Suggestion chips on the empty state start a new event with a type, a title and, for the new year, the date.
    val presetTitle = when (preset) {
        "birthday" -> context.getString(R.string.chip_birthday); "trip" -> context.getString(R.string.chip_trip)
        "baby" -> context.getString(R.string.chip_baby); "anniversary" -> context.getString(R.string.chip_anniversary)
        "exam" -> context.getString(R.string.chip_exam); "newyear" -> context.getString(R.string.chip_new_year)
        else -> null
    }
    LaunchedEffect(eventId, preset) {
        if (eventId == null && presetTitle != null) {
            title = presetTitle
            when (preset) {
                "birthday" -> { type = EventType.AGE; date = today.minusYears(1) }
                "anniversary" -> { type = EventType.COUNTUP; date = today }
                "newyear" -> date = LocalDate.of(today.year + 1, 1, 1)
            }
        }
        if (eventId != null) vm.load(eventId)?.let { e ->
            original = e; type = e.type; title = e.title; date = e.date; endDate = e.endDate ?: e.date.plusDays(7); time = e.time
            reminderDays = e.reminders.map { it.daysBefore }.toSet(); repeatYearly = e.repeatYearly
            e.reminders.firstOrNull()?.let { reminderTime = it.at }
        }
    }
    // New events start with the default reminder from Settings (negative means off).
    val selectedReminders = reminderDays ?: if (settings.defaultReminderDaysBefore >= 0) setOf(settings.defaultReminderDaysBefore) else emptySet()

    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val pastCountdown = eventId == null && type == EventType.COUNTDOWN && date.isBefore(today)
    val hasReminders = type != EventType.COUNTUP
    val endBeforeStart = type == EventType.PERIOD && endDate.isBefore(date)
    val periodDays = ChronoUnit.DAYS.between(date, endDate) + 1

    fun save() {
        if (title.isBlank()) { titleError = true; return }
        if (pastCountdown || endBeforeStart) return
        val base = original
        val rules = if (hasReminders) selectedReminders.sorted().map { d ->
            ReminderRule(d, reminderTime)
        } else emptyList()
        val event = (base ?: Event(title = title.trim(), type = type, date = date, endDate = if (type == EventType.PERIOD) endDate else null)).copy(
            title = title.trim(), type = type, date = date,
            endDate = if (type == EventType.PERIOD) endDate else null,
            time = if (type == EventType.COUNTDOWN) time else null,
            startDate = base?.startDate ?: if (type == EventType.COUNTDOWN) today else null,
            reminders = rules,
            repeatYearly = type == EventType.COUNTDOWN && repeatYearly,
        )
        val needsRationale = event.reminders.isNotEmpty() && Build.VERSION.SDK_INT >= 33 && !settings.notificationsAsked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val needsExact = event.reminders.isNotEmpty() && reminderTime != ReminderScheduler.DEFAULT_TIME && !scheduler.canScheduleExact()
        when {
            needsRationale -> pendingEvent = event
            needsExact -> exactEvent = event
            else -> vm.save(event, onClose)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (eventId == null) R.string.new_event else R.string.edit_event)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
            actions = { TextButton(onClick = ::save, enabled = !endBeforeStart) { Text(stringResource(R.string.save)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EventType.COUNTDOWN to R.string.type_countdown, EventType.AGE to R.string.type_birthday, EventType.COUNTUP to R.string.type_countup, EventType.PERIOD to R.string.type_period)
                    .forEach { (t, label) -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(label)) }) }
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it; titleError = false }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_title)) }, isError = titleError,
                supportingText = if (titleError) ({ Text(stringResource(R.string.err_title_empty)) }) else null,
            )
            OutlinedButton(onClick = { pickingDate = 1 }, modifier = Modifier.fillMaxWidth()) {
                val label = if (type == EventType.PERIOD) R.string.field_from else R.string.field_date
                Text(stringResource(label) + ": " + NumeralFormatter.format(date.format(fmt), settings.numerals, tag))
            }
            if (type == EventType.PERIOD) {
                OutlinedButton(
                    onClick = { pickingDate = 2 }, modifier = Modifier.fillMaxWidth(),
                    colors = if (endBeforeStart) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors(),
                ) { Text(stringResource(R.string.field_to) + ": " + NumeralFormatter.format(endDate.format(fmt), settings.numerals, tag)) }
                if (endBeforeStart) Text(stringResource(R.string.err_end_before_start), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                else Text(
                    pluralStringResource(R.plurals.period_length, periodDays.toInt(), NumeralFormatter.format(periodDays.toString(), settings.numerals, tag)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    (if (type == EventType.PERIOD) PeriodReminderPresets else ReminderPresets).forEach { d ->
                        FilterChip(
                            selected = d in selectedReminders,
                            onClick = { reminderDays = if (d in selectedReminders) selectedReminders - d else selectedReminders + d },
                            label = {
                                Text(if (d == 0) stringResource(if (type == EventType.PERIOD) R.string.remind_on_last_day else R.string.remind_on_day)
                                else pluralStringResource(R.plurals.remind_days_before, d, NumeralFormatter.format(d.toString(), settings.numerals, tag)))
                            },
                        )
                    }
                }
            }
            if (hasReminders && selectedReminders.isNotEmpty()) {
                OutlinedButton(onClick = { pickingReminderTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.remind_time) + ": " + NumeralFormatter.format(reminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), settings.numerals, tag))
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

    pendingEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.rationale_title)) },
            text = { Text(stringResource(R.string.rationale_body, ev.title)) },
            confirmButton = { TextButton(onClick = { settingsVm.markNotificationsAsked(); permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.continue_label)) } },
            dismissButton = { TextButton(onClick = { settingsVm.markNotificationsAsked(); pendingEvent = null; vm.save(ev, onClose) }) { Text(stringResource(R.string.not_now)) } },
        )
    }

    exactEvent?.let { ev ->
        val shown = NumeralFormatter.format(reminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), settings.numerals, tag)
        AlertDialog(
            onDismissRequest = { exactEvent = null },
            title = { Text(stringResource(R.string.exact_title)) },
            text = { Text(stringResource(R.string.exact_body, shown)) },
            confirmButton = {
                TextButton(onClick = {
                    exactEvent = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)))
                    vm.save(ev, onClose)
                }) { Text(stringResource(R.string.exact_open)) }
            },
            dismissButton = { TextButton(onClick = { exactEvent = null; vm.save(ev, onClose) }) { Text(stringResource(R.string.exact_keep)) } },
        )
    }

    if (pickingReminderTime) {
        val state = rememberTimePickerState(initialHour = reminderTime.hour, initialMinute = reminderTime.minute, is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { pickingReminderTime = false },
            title = { Text(stringResource(R.string.remind_time)) },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { reminderTime = LocalTime.of(state.hour, state.minute); pickingReminderTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickingReminderTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (pickingDate != 0) {
        val editingEnd = pickingDate == 2
        val initial = if (editingEnd) endDate else date
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val picked = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (editingEnd) endDate = picked else date = picked
                    }
                    pickingDate = 0
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = 0 }) { Text(stringResource(R.string.cancel)) } },
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
