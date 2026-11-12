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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import app.flipday.core.EventQuery
import app.flipday.core.EventSort
import app.flipday.core.EventType
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
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.ui.EventsViewModel
import app.flipday.ui.SettingsViewModel
import app.flipday.ui.components.FlipNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(factory: ViewModelProvider.Factory, onOpen: (Long) -> Unit, onSearch: () -> Unit, onArchive: () -> Unit) {
    val vm: EventsViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val events by vm.events.collectAsState()
    val query by vm.query.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    val settings by settingsVm.settings.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.upcoming) + " · " + NumeralFormatter.format(today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)), settings.numerals, locale.toLanguageTag()),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.my_events), style = MaterialTheme.typography.headlineLarge)
            }
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_events)) }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_sort_filter)) }, onClick = { menuOpen = false; sheetOpen = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_archive)) }, onClick = { menuOpen = false; onArchive() })
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                null to R.string.filter_all, EventType.COUNTDOWN to R.string.filter_countdown,
                EventType.AGE to R.string.filter_birthdays, EventType.COUNTUP to R.string.filter_countup,
            ).forEach { (f, label) ->
                FilterChip(selected = query.type == f, onClick = { vm.setType(f) }, label = { Text(stringResource(label)) })
            }
        }
        Spacer(Modifier.height(8.dp))
        val list = events
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState()
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { EventRow(it, today, settings.numerals, locale.toLanguageTag(), onClick = { onOpen(it.id) }) }
            }
        }
    }
    if (sheetOpen) SortFilterSheet(query, onDismiss = { sheetOpen = false }, onApply = { s, c, ty -> vm.apply(s, c, ty); sheetOpen = false }, onReset = { vm.reset(); sheetOpen = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortFilterSheet(query: EventQuery, onDismiss: () -> Unit, onApply: (EventSort, Boolean, EventType?) -> Unit, onReset: () -> Unit) {
    var sort by remember { mutableStateOf(query.sort) }
    var type by remember { mutableStateOf(query.type) }
    var completed by remember { mutableStateOf(query.showCompleted) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.menu_sort_filter), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(EventSort.NEAREST to R.string.sort_nearest, EventSort.DATE_ADDED to R.string.sort_added, EventSort.NAME to R.string.sort_name).forEach { (s, label) ->
                Row(Modifier.fillMaxWidth().selectable(selected = sort == s, role = Role.RadioButton, onClick = { sort = s }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = sort == s, onClick = null)
                    Text(stringResource(label), Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
                }
            }
            Text(stringResource(R.string.filter_show), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to R.string.filter_all, EventType.COUNTDOWN to R.string.filter_countdown, EventType.AGE to R.string.filter_birthdays, EventType.COUNTUP to R.string.filter_countup)
                    .forEach { (ty, label) -> FilterChip(selected = type == ty, onClick = { type = ty }, label = { Text(stringResource(label)) }) }
            }
            Row(Modifier.fillMaxWidth().toggleable(value = completed, role = Role.Checkbox, onValueChange = { completed = it }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = completed, onCheckedChange = null)
                Text(stringResource(R.string.show_completed), Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = { onApply(sort, completed, type) }) { Text(stringResource(R.string.apply)) }
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
internal fun EventRow(event: Event, today: LocalDate, numerals: NumeralMode, tag: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val (value, unit) = headline(event, today)
    val digits = NumeralFormatter.format(value.toString().padStart(2, '0'), numerals, tag)
    val description = context.resources.getQuantityString(R.plurals.cd_event, value.toInt(), event.title, value.toInt())
    val shownDate = event.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
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
