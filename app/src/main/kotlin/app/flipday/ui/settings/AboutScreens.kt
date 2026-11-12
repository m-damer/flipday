package app.flipday.ui.settings

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
fun WidgetsScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tab_widgets), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.widgets_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, FlipdayWidgetReceiver::class.java), null, null)
        }) { Text(stringResource(R.string.widget_add)) }
    }
}

private fun openEmail(context: Context) =
    open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${DeveloperInfo.EMAIL}")))

private fun open(context: Context, intent: Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.dev_no_app, Toast.LENGTH_SHORT).show()
    }
}
