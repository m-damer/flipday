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
import app.flipday.widget.WidgetDraft
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.flipday.ui.EventsViewModel
import app.flipday.widget.LiveWidgetPreview
import app.flipday.widget.WidgetKeys
import app.flipday.widget.WidgetSize
import app.flipday.widget.withSamples
import app.flipday.widget.AgeWidgetReceiver
import app.flipday.widget.AgendaWidgetReceiver
import app.flipday.widget.BigNumberWidgetReceiver
import app.flipday.widget.CountUpWidgetReceiver
import app.flipday.widget.DateWidgetReceiver
import app.flipday.widget.RingWidgetReceiver
import app.flipday.widget.TickerWidgetReceiver
import app.flipday.widget.YearWidgetReceiver
import app.flipday.widget.templateDesc
import app.flipday.widget.templateName

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
fun AboutScreen(onBack: () -> Unit, onDeveloper: () -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit) {
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
                    ValueRow(stringResource(R.string.about_privacy), "", onClick = onPrivacy)
                    ValueRow(stringResource(R.string.about_licenses), "", onClick = onLicenses)
                    ValueRow(stringResource(R.string.about_feedback), "", onClick = { openEmail(context) })
                    ValueRow(stringResource(R.string.about_rate), "", last = true, onClick = { rateApp(context) })
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
    val eventsVm: EventsViewModel = viewModel(factory = factory)
    val events by eventsVm.allEvents.collectAsState()
    var unsupported by remember { mutableStateOf(false) }
    // null means "All": every family at its natural size. Otherwise only the families that come in that size.
    var filter by remember { mutableStateOf<WidgetSize?>(null) }
    val sample = withSamples(context, events.filter { it.archivedOn == null })

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.tab_widgets), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.widgets_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.gallery_all)) })
            WidgetSize.entries.forEach { s ->
                FilterChip(selected = filter == s, onClick = { filter = s }, label = { Text(stringResource(galleryLabel(s))) })
            }
        }
        WidgetTemplate.entries.filter { t -> filter == null || filter != WidgetSize.SMALL || t.hasSmall }.forEach { template ->
            val size = filter ?: if (template.hasSmall) WidgetSize.SMALL else WidgetSize.MEDIUM
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LiveWidgetPreview(mutablePreferencesOf(WidgetKeys.TEMPLATE to template.name), size, sample, settings.numerals)
                    }
                    Text(stringResource(templateName(template)), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(templateDesc(template)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        WidgetDraft.template = template
                        val manager = AppWidgetManager.getInstance(context)
                        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, receiverFor(template)), null, null)
                        else unsupported = true
                    }) { Text(stringResource(R.string.widget_add)) }
                }
            }
        }
        if (unsupported) Text(stringResource(R.string.widgets_pin_unsupported), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun galleryLabel(s: WidgetSize) = when (s) {
    WidgetSize.STRIP -> R.string.gallery_strip; WidgetSize.SMALL -> R.string.gallery_small
    WidgetSize.MEDIUM -> R.string.gallery_medium; WidgetSize.LARGE -> R.string.gallery_large
}

private fun receiverFor(t: WidgetTemplate): Class<*> = when (t) {
    WidgetTemplate.BIG -> BigNumberWidgetReceiver::class.java; WidgetTemplate.AGE -> AgeWidgetReceiver::class.java
    WidgetTemplate.COUNTUP -> CountUpWidgetReceiver::class.java; WidgetTemplate.AGENDA -> AgendaWidgetReceiver::class.java
    WidgetTemplate.RING -> RingWidgetReceiver::class.java; WidgetTemplate.DATE -> DateWidgetReceiver::class.java
    WidgetTemplate.TICKER -> TickerWidgetReceiver::class.java; WidgetTemplate.YEAR -> YearWidgetReceiver::class.java
}

private fun rateApp(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.packageName))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName))
    if (!runCatching { context.startActivity(market) }.isSuccess && !runCatching { context.startActivity(web) }.isSuccess) {
        android.widget.Toast.makeText(context, R.string.rate_failed, android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ChildScaffold(stringResource(R.string.privacy_title), onBack) {
        Text(
            stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        )
    }
}

private val Components = listOf(
    "Kotlin and Kotlin Coroutines" to R.string.licenses_apache,
    "AndroidX Core, AppCompat, Activity, Lifecycle" to R.string.licenses_apache,
    "Jetpack Compose and Material 3" to R.string.licenses_apache,
    "Jetpack Navigation" to R.string.licenses_apache,
    "Room" to R.string.licenses_apache,
    "DataStore" to R.string.licenses_apache,
    "Jetpack Glance" to R.string.licenses_apache,
    "IBM Plex Sans, IBM Plex Sans Arabic" to R.string.licenses_ofl,
    "Space Grotesk" to R.string.licenses_ofl,
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    ChildScaffold(stringResource(R.string.licenses_title), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Components.forEach { (name, license) ->
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(license), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun openEmail(context: Context) =
    open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${DeveloperInfo.EMAIL}")))

private fun open(context: Context, intent: Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.dev_no_app, Toast.LENGTH_SHORT).show()
    }
}
