package app.flipday.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.ui.theme.FlipdayTheme
import kotlinx.coroutines.launch

/** "Setup on drop": opens when a widget is added (and when a widget with a missing event is tapped). */
class WidgetConfigActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Backing out must cancel, so the launcher removes the widget that was just dropped.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val container = (application as FlipdayApp).container

        setContent {
            FlipdayTheme(dark = isSystemInDarkTheme()) {
                val events by container.repository.observeEvents().collectAsState(initial = emptyList())
                val active = events.filter { it.archivedOn == null }
                var eventId by remember { mutableLongStateOf(0L) }
                var template by remember { mutableStateOf(WidgetTemplate.FLAP) }
                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.widget_setup_title)) },
                        navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) } },
                    )
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        LazyColumn(Modifier.weight(1f)) {
                            item { ChoiceRow(stringResource(R.string.widget_nearest), eventId == 0L) { eventId = 0L } }
                            items(active, key = { it.id }) { e -> ChoiceRow(e.title, eventId == e.id) { eventId = e.id } }
                        }
                        Text(stringResource(R.string.widget_style), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            FilterChip(selected = template == WidgetTemplate.FLAP, onClick = { template = WidgetTemplate.FLAP }, label = { Text(stringResource(R.string.style_flap)) })
                            FilterChip(selected = template == WidgetTemplate.PAPER, onClick = { template = WidgetTemplate.PAPER }, label = { Text(stringResource(R.string.style_paper)) })
                        }
                        Button(onClick = {
                            lifecycleScope.launch {
                                val gid = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                                updateAppWidgetState(this@WidgetConfigActivity, gid) { p ->
                                    p[WidgetKeys.EVENT_ID] = eventId
                                    p[WidgetKeys.TEMPLATE] = template.name
                                }
                                FlipdayWidget().update(this@WidgetConfigActivity, gid)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                finish()
                            }
                        }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text(stringResource(R.string.save)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
