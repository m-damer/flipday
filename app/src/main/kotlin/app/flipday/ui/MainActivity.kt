package app.flipday.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.FlipdayApp
import app.flipday.data.ThemeMode
import app.flipday.ui.theme.FlipdayTheme

class MainActivity : AppCompatActivity() {
    private val openEvent = mutableStateOf<Long?>(null)
    private val openUpdate = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openEvent.value = intent.getLongExtra(EXTRA_EVENT_ID, 0L).takeIf { it > 0 }
        openUpdate.value = intent.getBooleanExtra(EXTRA_OPEN_UPDATE, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openEvent.value = intent.getLongExtra(EXTRA_EVENT_ID, 0L).takeIf { it > 0 }
        openUpdate.value = intent.getBooleanExtra(EXTRA_OPEN_UPDATE, false)
        val container = (application as FlipdayApp).container
        val factory: ViewModelProvider.Factory = AppViewModelFactory(container)
        setContent {
            val settingsVm: SettingsViewModel = viewModel(factory = factory)
            val settings by settingsVm.settings.collectAsState()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FlipdayTheme(dark = dark) { FlipdayRoot(factory, openEvent.value, onOpened = { openEvent.value = null }, openUpdate = openUpdate.value, onUpdateOpened = { openUpdate.value = false }) }
        }
    }

    companion object { const val EXTRA_EVENT_ID = "event_id"; const val EXTRA_OPEN_UPDATE = "open_update" }
}
