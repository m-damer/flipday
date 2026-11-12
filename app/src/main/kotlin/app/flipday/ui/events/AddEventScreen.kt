package app.flipday.ui.events

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.EventType
import app.flipday.ui.AddEventViewModel
import app.flipday.ui.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(factory: ViewModelProvider.Factory, onClose: () -> Unit) {
    val vm: AddEventViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsState()
    val locale = LocalConfiguration.current.locales[0]

    var type by remember { mutableStateOf(EventType.COUNTDOWN) }
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var picking by remember { mutableStateOf(false) }
    val pastCountdown = type == EventType.COUNTDOWN && date.isBefore(LocalDate.now())

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.new_countdown)) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
            actions = {
                TextButton(onClick = {
                    if (title.isBlank()) titleError = true
                    else if (!pastCountdown) vm.save(title, type, date, settings.defaultReminderDaysBefore, onClose)
                }) { Text(stringResource(R.string.save)) }
            },
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
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.field_date) + ": " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
            }
            if (pastCountdown) {
                Text(stringResource(R.string.err_date_past), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.suggest_countup), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { type = EventType.COUNTUP }) { Text(stringResource(R.string.switch_to_countup)) }
            }
        }
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}
