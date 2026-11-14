package app.flipday.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.flipday.R
import app.flipday.core.NumeralFormatter
import app.flipday.core.NumeralMode
import app.flipday.ui.components.FlipNumber

/** First run (design 01): three sample counters, the promise, a language switch and Get started. */
@Composable
fun OnboardingScreen(numerals: NumeralMode, onStart: () -> Unit) {
    val lang = LocalConfiguration.current.locales[0].language
    val tag = LocalConfiguration.current.locales[0].toLanguageTag()
    fun n(s: String) = NumeralFormatter.format(s, numerals, tag)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            listOf(R.string.onboarding_trip to "68", R.string.onboarding_due to "23", R.string.sample_birthday to "06").forEach { (label, digits) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FlipNumber(n(digits), stringResource(label) + " " + n(digits), tileHeight = 44.dp)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "ar" to "العربية").forEach { (code, name) ->
                    FilterChip(selected = lang == code, onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code)) }, label = { Text(name) })
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_start)) }
        }
    }
}
