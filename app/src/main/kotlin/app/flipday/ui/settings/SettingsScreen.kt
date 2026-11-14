package app.flipday.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.R
import app.flipday.core.ArchivePolicy
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.data.ThemeMode
import app.flipday.ui.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon

/**
 * Grouped in the order users need them (see design board 08): Reminders, Events and widgets,
 * Language and display, Data, About. Settings is a tab root, so there is no back arrow.
 */
@Composable
fun SettingsScreen(factory: ViewModelProvider.Factory, onAbout: () -> Unit, onBackup: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val s by vm.settings.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setNotifications(granted) }
    val lang = LocalConfiguration.current.locales[0].language

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp))

        Section(R.string.settings_reminders) {
            ToggleRow(stringResource(R.string.set_notifications), s.notificationsEnabled) { on ->
                if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setNotifications(on)
            }
            ValueRow(stringResource(R.string.set_default_reminder), reminderLabel(s.defaultReminderDaysBefore, s.numerals, lang)) { dialog = "reminder" }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var resumes by remember { mutableStateOf(0) }
            val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(owner) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumes++ }
                owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
            }
            val exactOk = remember(resumes) { app.flipday.reminders.ReminderScheduler(ctx.applicationContext).canScheduleExact() }
            ValueRow(stringResource(R.string.set_exact), stringResource(if (exactOk) R.string.value_allowed else R.string.value_not_allowed), last = true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ctx.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + ctx.packageName)),
                )
            }
        }
        Section(R.string.settings_events_widgets) {
            ValueRow(stringResource(R.string.set_archive_finished), archiveLabel(s.archiveAfterDays, s.numerals, lang)) { dialog = "archive" }
            ValueRow(stringResource(R.string.set_widget_refresh), stringResource(R.string.value_automatic), last = true) { dialog = "widgetrefresh" }
        }
        Section(R.string.settings_language_display) {
            ValueRow(stringResource(R.string.set_language), if (lang == "ar") stringResource(R.string.value_arabic) else stringResource(R.string.value_english)) { dialog = "language" }
            ValueRow(stringResource(R.string.set_numerals), numeralLabel(s.numerals)) { dialog = "numerals" }
            ValueRow(stringResource(R.string.set_theme), themeLabel(s.theme), last = true) { dialog = "theme" }
        }
        Section(R.string.settings_data) {
            ValueRow(stringResource(R.string.set_backup), "", last = true, onClick = onBackup)
        }
        Section(R.string.settings_about) {
            ValueRow(stringResource(R.string.set_about_app), "", last = true, onClick = onAbout)
        }
    }

    when (dialog) {
        "widgetrefresh" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.widget_refresh_title)) },
            text = { Text(stringResource(R.string.widget_refresh_body)) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.ok_got_it)) } },
        )
        "reminder" -> ChoiceDialog(
            title = R.string.set_default_reminder,
            options = listOf(-1, 0, 1, 3, 7).map { reminderLabel(it, s.numerals, lang) to it.toString() },
            selected = s.defaultReminderDaysBefore.toString(),
            onPick = { vm.setDefaultReminder(it.toInt()); dialog = null },
            onDismiss = { dialog = null },
        )
        "archive" -> ChoiceDialog(
            title = R.string.set_archive_finished,
            options = listOf(1, 7, 30, ArchivePolicy.NEVER).map { archiveLabel(it, s.numerals, lang) to it.toString() },
            selected = s.archiveAfterDays.toString(),
            onPick = { vm.setArchiveAfter(it.toInt()); dialog = null },
            onDismiss = { dialog = null },
        )
        "language" -> ChoiceDialog(
            title = R.string.dialog_language_title,
            options = listOf(stringResource(R.string.value_system) to "", stringResource(R.string.value_english) to "en", stringResource(R.string.value_arabic) to "ar"),
            selected = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(','),
            onPick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)); dialog = null },
            onDismiss = { dialog = null },
        )
        "numerals" -> ChoiceDialog(
            title = R.string.dialog_numerals_title,
            options = listOf(stringResource(R.string.value_match_language) to NumeralMode.MATCH_LANGUAGE.name, stringResource(R.string.value_western) to NumeralMode.WESTERN.name, stringResource(R.string.value_arabic_indic) to NumeralMode.ARABIC_INDIC.name),
            selected = s.numerals.name,
            onPick = { vm.setNumerals(NumeralMode.valueOf(it)); dialog = null },
            onDismiss = { dialog = null },
        )
        "theme" -> ChoiceDialog(
            title = R.string.dialog_theme_title,
            options = listOf(stringResource(R.string.value_system) to ThemeMode.SYSTEM.name, stringResource(R.string.value_light) to ThemeMode.LIGHT.name, stringResource(R.string.value_dark) to ThemeMode.DARK.name),
            selected = s.theme.name,
            onPick = { vm.setTheme(ThemeMode.valueOf(it)); dialog = null },
            onDismiss = { dialog = null },
        )
    }
}

@Composable private fun reminderLabel(days: Int, numerals: NumeralMode, lang: String): String = when {
    days < 0 -> stringResource(R.string.default_reminder_off)
    days == 0 -> stringResource(R.string.remind_on_day)
    else -> pluralStringResource(R.plurals.remind_days_before, days, NumeralFormatter.format(days.toString(), numerals, lang))
}

@Composable private fun archiveLabel(days: Int, numerals: NumeralMode, lang: String): String =
    if (days < 0) stringResource(R.string.archive_never)
    else pluralStringResource(R.plurals.archive_after_days, days, NumeralFormatter.format(days.toString(), numerals, lang))

@Composable private fun numeralLabel(m: NumeralMode) = stringResource(
    when (m) { NumeralMode.MATCH_LANGUAGE -> R.string.value_match_language; NumeralMode.WESTERN -> R.string.value_western; NumeralMode.ARABIC_INDIC -> R.string.value_arabic_indic },
)
@Composable private fun themeLabel(m: ThemeMode) = stringResource(
    when (m) { ThemeMode.SYSTEM -> R.string.value_system; ThemeMode.LIGHT -> R.string.value_light; ThemeMode.DARK -> R.string.value_dark },
)

@Composable
internal fun Section(title: Int, content: @Composable () -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp))
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
internal fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun ValueRow(label: String, value: String, enabled: Boolean = true, last: Boolean = false, onClick: () -> Unit = {}) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).let { if (enabled) it.clickable(onClick = onClick) else it }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (enabled) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceDialog(title: Int, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                options.forEach { (label, key) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(key) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == key, onClick = { onPick(key) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
