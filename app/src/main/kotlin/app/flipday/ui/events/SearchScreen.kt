package app.flipday.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.EventQueries
import app.flipday.ui.SearchViewModel
import app.flipday.ui.SettingsViewModel
import java.time.LocalDate

/** Searches active, completed and archived events (design 06B.2). Back arrow and clear button, no bottom bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(factory: ViewModelProvider.Factory, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: SearchViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val all by vm.all.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    val today = LocalDate.now()
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = EventQueries.search(all, text, today)

    Scaffold(topBar = {
        TopAppBar(
            title = {
                TextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_events)) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            actions = { if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear)) } },
        )
    }) { padding ->
        when {
            text.isBlank() -> Unit
            results.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.search_none_title, text.trim()), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.search_none_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.id }) { EventRow(it, today, settings.numerals, tag, onClick = { onOpen(it.id) }) }
            }
        }
    }
}
