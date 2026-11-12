package app.flipday.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.BackupCodec
import app.flipday.core.BackupException
import app.flipday.core.Event
import app.flipday.core.NumeralFormatter
import app.flipday.ui.BackupViewModel
import app.flipday.ui.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

/** Export events to a JSON file and import them back (design 06C.4). Files go through the system picker, so no storage permission. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val vm: BackupViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsState()
    val context = LocalContext.current
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pending by remember { mutableStateOf<List<Event>?>(null) }
    var skipped by remember { mutableStateOf(0) }
    fun n(v: Int) = NumeralFormatter.format(v.toString(), settings.numerals, tag)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = vm.exportText()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) } }
            }.isSuccess
            snackbar.showSnackbar(context.getString(if (ok) R.string.backup_exported else R.string.backup_export_failed))
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_IMPORT_BYTES) throw BackupException("File too large")
                            out.write(buffer, 0, read)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }
                }
                val contents = BackupCodec.decode(text)
                val fresh = vm.newEvents(contents)
                if (fresh.isEmpty()) context.getString(R.string.backup_nothing_new) else { skipped = contents.skipped; pending = fresh; null }
            }.getOrElse { e ->
                context.getString(if (e is BackupException && e.newerVersion) R.string.backup_newer_file else R.string.backup_bad_file)
            }
            if (message != null) snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ValueRow(stringResource(R.string.backup_export), "") {
                exportLauncher.launch(context.getString(R.string.backup_export_name, LocalDate.now().toString()))
            }
            ValueRow(stringResource(R.string.backup_import), "", last = true) { importLauncher.launch(arrayOf("*/*")) }
            Text(
                stringResource(R.string.backup_note), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp),
            )
        }
    }

    pending?.let { events ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = {
                Column {
                    Text(pluralStringResource(R.plurals.backup_import_body, events.size, n(events.size)))
                    if (skipped > 0) Text(pluralStringResource(R.plurals.backup_skipped, skipped, n(skipped)), modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        vm.add(events)
                        snackbar.showSnackbar(context.resources.getQuantityString(R.plurals.backup_import_done, events.size, n(events.size)))
                    }
                }) { Text(stringResource(R.string.backup_import_add)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
