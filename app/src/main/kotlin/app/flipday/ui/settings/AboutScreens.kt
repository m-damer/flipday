package app.flipday.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.core.NumeralFormatter
import app.flipday.ui.SettingsViewModel
import app.flipday.widget.WidgetAccent
import app.flipday.widget.WidgetDraft
import app.flipday.widget.WidgetPreview
import app.flipday.widget.WidgetTemplate
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.flipday.DeveloperInfo
import app.flipday.R
import app.flipday.ui.components.FlipNumber
import app.flipday.widget.FlipdayWidgetReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
        )
    }) { padding -> Column(Modifier.fillMaxSize().padding(padding)) { content() } }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onDeveloper: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    ChildScaffold(stringResource(R.string.about_title), onBack) {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FlipNumber("7", stringResource(R.string.app_name), tileHeight = 72.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column {
                    ValueRow(stringResource(R.string.about_developer), "", onClick = onDeveloper)
                    ValueRow(stringResource(R.string.about_privacy), stringResource(R.string.coming_soon), enabled = false)
                    ValueRow(stringResource(R.string.about_licenses), stringResource(R.string.coming_soon), enabled = false)
                    ValueRow(stringResource(R.string.about_feedback), "", onClick = { openEmail(context) })
                    ValueRow(stringResource(R.string.about_rate), stringResource(R.string.coming_soon), enabled = false, last = true)
                }
            }
        }
    }
}

@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ChildScaffold(stringResource(R.string.about_developer), onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.dev_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.dev_role), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.dev_bio), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    ValueRow(stringResource(R.string.dev_whatsapp), DeveloperInfo.WHATSAPP_DISPLAY, onClick = { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(DeveloperInfo.WHATSAPP_URL))) })
                    ValueRow(stringResource(R.string.dev_email), DeveloperInfo.EMAIL_DISPLAY, last = true, onClick = { openEmail(context) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.dev_footer), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WidgetsScreen(factory: ViewModelProvider.Factory) {
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsState()
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    val digits = NumeralFormatter.format("24", settings.numerals, tag)
    val title = stringResource(R.string.widget_sample_title)
    val unit = stringResource(R.string.label_days)
    var unsupported by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.tab_widgets), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.widgets_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            Triple(WidgetTemplate.FLAP, R.string.style_flap, R.string.style_flap_desc),
            Triple(WidgetTemplate.PAPER, R.string.style_paper, R.string.style_paper_desc),
        ).forEach { (template, name, desc) ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WidgetPreview(title, digits, unit, "$title, $digits $unit", template, WidgetAccent.TEAL, true, true, Modifier.width(170.dp))
                    }
                    Text(stringResource(name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        WidgetDraft.template = template
                        val manager = AppWidgetManager.getInstance(context)
                        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, FlipdayWidgetReceiver::class.java), null, null)
                        else unsupported = true
                    }) { Text(stringResource(R.string.widget_add)) }
                }
            }
        }
        if (unsupported) Text(stringResource(R.string.widgets_pin_unsupported), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openEmail(context: Context) =
    open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${DeveloperInfo.EMAIL}")))

private fun open(context: Context, intent: Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.dev_no_app, Toast.LENGTH_SHORT).show()
    }
}
