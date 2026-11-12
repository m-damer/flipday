package app.flipday.ui.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.EventType
import app.flipday.ui.ArchiveViewModel
import app.flipday.ui.SettingsViewModel
import java.time.LocalDate

/** Archived events, newest first (design 06B.3). Child screen: Back arrow, no bottom bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(factory: ViewModelProvider.Factory, onBack: () -> Unit, onOpen: (Long) -> Unit, onChangeArchiveTime: () -> Unit) {
    val vm: ArchiveViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val events by vm.events.collectAsState()
    val selected by vm.selected.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    val today = LocalDate.now()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.menu_archive)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to R.string.filter_all, EventType.COUNTDOWN to R.string.filter_past, EventType.COUNTUP to R.string.filter_countup, EventType.AGE to R.string.filter_birthdays)
                    .forEach { (t, label) -> FilterChip(selected = selected == t, onClick = { vm.setType(t) }, label = { Text(stringResource(label)) }) }
            }
            val list = events
            when {
                list == null -> Unit
                list.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.archive_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.archive_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(onClick = onChangeArchiveTime) { Text(stringResource(R.string.archive_change_time)) }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { EventRow(it, today, settings.numerals, tag, onClick = { onOpen(it.id) }) }
                }
            }
        }
    }
}
