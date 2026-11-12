package app.flipday.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.flipday.FlipdayApp
import app.flipday.data.ThemeMode
import app.flipday.ui.theme.FlipdayTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
            FlipdayTheme(dark = dark) { FlipdayRoot(factory) }
        }
    }
}
