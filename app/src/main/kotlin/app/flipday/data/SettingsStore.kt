package app.flipday.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.flipday.core.ArchivePolicy
import app.flipday.core.NumeralMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val numerals: NumeralMode = NumeralMode.MATCH_LANGUAGE,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val archiveAfterDays: Int = ArchivePolicy.DEFAULT_AFTER_DAYS,
    val defaultReminderDaysBefore: Int = 7,
    val notificationsAsked: Boolean = false,
    /** True until the first-run screen has been seen. Defaults to true so existing users never flash it while settings load. */
    val onboarded: Boolean = true,
    val autoUpdates: Boolean = true,
    /** versionCode of the newest release found, 0 when up to date or never checked. */
    val updateCode: Int = 0,
    val updateName: String = "",
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object K {
        val numerals = stringPreferencesKey("numerals")
        val theme = stringPreferencesKey("theme")
        val notifications = booleanPreferencesKey("notifications")
        val archiveAfter = intPreferencesKey("archive_after_days")
        val defaultReminder = intPreferencesKey("default_reminder_days")
        val asked = booleanPreferencesKey("notifications_asked")
        val onboarded = booleanPreferencesKey("onboarded")
        val autoUpdates = booleanPreferencesKey("auto_updates")
        val updateCode = intPreferencesKey("update_code")
        val updateName = stringPreferencesKey("update_name")
        val notifiedCode = intPreferencesKey("update_notified_code")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            numerals = p[K.numerals]?.let { runCatching { NumeralMode.valueOf(it) }.getOrNull() } ?: NumeralMode.MATCH_LANGUAGE,
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            notificationsEnabled = p[K.notifications] ?: true,
            archiveAfterDays = p[K.archiveAfter] ?: ArchivePolicy.DEFAULT_AFTER_DAYS,
            defaultReminderDaysBefore = p[K.defaultReminder] ?: 7,
            notificationsAsked = p[K.asked] ?: false,
            onboarded = p[K.onboarded] ?: false,
            autoUpdates = p[K.autoUpdates] ?: true,
            updateCode = p[K.updateCode] ?: 0,
            updateName = p[K.updateName].orEmpty(),
        )
    }

    suspend fun setNumerals(m: NumeralMode) = edit { it[K.numerals] = m.name }
    suspend fun setTheme(m: ThemeMode) = edit { it[K.theme] = m.name }
    suspend fun setNotifications(on: Boolean) = edit { it[K.notifications] = on }
    suspend fun setAutoUpdates(on: Boolean) = edit { it[K.autoUpdates] = on }
    suspend fun setUpdate(code: Int, name: String) = edit { it[K.updateCode] = code; it[K.updateName] = name }
    suspend fun setNotifiedCode(code: Int) = edit { it[K.notifiedCode] = code }
    suspend fun readNotifiedCode(): Int = context.dataStore.data.map { it[K.notifiedCode] ?: 0 }.first()
    suspend fun markOnboarded() = edit { it[K.onboarded] = true }
    suspend fun markNotificationsAsked() = edit { it[K.asked] = true }
    suspend fun setDefaultReminder(daysBefore: Int) = edit { it[K.defaultReminder] = daysBefore }
    suspend fun setArchiveAfter(days: Int) = edit { it[K.archiveAfter] = days }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
