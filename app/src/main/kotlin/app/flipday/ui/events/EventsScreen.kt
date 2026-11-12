package app.flipday.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventCalculator
import app.flipday.core.EventPhase
import app.flipday.core.EventType
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.ui.EventFilter
import app.flipday.ui.EventsViewModel
import app.flipday.ui.SettingsViewModel
import app.flipday.ui.components.FlipNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun EventsScreen(factory: ViewModelProvider.Factory) {
    val vm: EventsViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val events by vm.events.collectAsState()
    val selected by vm.selected.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.upcoming) + " · " + today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.my_events), style = MaterialTheme.typography.headlineLarge)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                EventFilter.ALL to R.string.filter_all, EventFilter.COUNTDOWN to R.string.filter_countdown,
                EventFilter.BIRTHDAYS to R.string.filter_birthdays, EventFilter.COUNTUP to R.string.filter_countup,
            ).forEach { (f, label) ->
                FilterChip(selected = selected == f, onClick = { vm.setFilter(f) }, label = { Text(stringResource(label)) })
            }
        }
        Spacer(Modifier.height(8.dp))
        val list = events
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState()
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { EventRow(it, today, settings.numerals, locale.toLanguageTag()) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EventRow(event: Event, today: LocalDate, numerals: NumeralMode, tag: String) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val (value, unit) = when (event.type) {
        EventType.COUNTDOWN -> {
            if (EventCalculator.phase(event.date, today) == EventPhase.TODAY) 0L to R.string.state_today
            else maxOf(0L, EventCalculator.daysUntil(event.date, today)) to R.string.label_days
        }
        EventType.AGE -> EventCalculator.age(event.date, today).years.toLong() to R.string.label_years_old
        EventType.COUNTUP -> EventCalculator.daysSince(event.date, today) to R.string.label_since
    }
    val digits = NumeralFormatter.format(value.toString().padStart(2, '0'), numerals, tag)
    val description = context.resources.getQuantityString(R.plurals.cd_event, value.toInt(), event.title, value.toInt())
    val shownDate = event.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(shownDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlipNumber(digits, description, tileHeight = 36.dp)
                Text(stringResource(unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
