package app.flipday.widget

import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.Preferences
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.EventType
import app.flipday.core.NumeralMode
import java.time.LocalDate

/** Launcher-cell sizes from the design: strip 4x1, small 2x2, medium 4x2, large 4x4 (dp as the widget layouts see them). */
internal fun previewSize(s: WidgetSize) = when (s) {
    WidgetSize.STRIP -> DpSize(250.dp, 40.dp); WidgetSize.SMALL -> DpSize(110.dp, 110.dp)
    WidgetSize.MEDIUM -> DpSize(250.dp, 110.dp); WidgetSize.LARGE -> DpSize(250.dp, 250.dp)
}

/**
 * The real widget, drawn in the app: the same [WidgetBody] the home screen uses, rendered to RemoteViews and inflated here.
 * What this shows is what the launcher shows, because there is only one set of layouts.
 */
@OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun LiveWidgetPreview(prefs: Preferences, size: WidgetSize, events: List<Event>, numerals: NumeralMode, appWidgetId: Int = -1, refresh: Int = 0) {
    val context = LocalContext.current
    val d = previewSize(size)
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(prefs, size, events, numerals, refresh) {
        val locale = context.resources.configuration.locales[0]
        views = runCatching {
            androidx.glance.appwidget.GlanceRemoteViews().compose(context = context, size = d, state = prefs) {
                WidgetBody(context, appWidgetId, events, numerals, locale, WidgetTemplate.BIG, prefs)
            }.remoteViews
        }.getOrNull()
    }
    AndroidView(
        factory = { FrameLayout(it) },
        update = { frame -> views?.let { rv -> frame.removeAllViews(); frame.addView(rv.apply(frame.context, frame)) } },
        modifier = Modifier.size(d.width, d.height).clip(MaterialTheme.shapes.large),
    )
}

/** Example events for previews: your own events when you have them, and a sample of each kind that is missing. */
internal fun withSamples(context: Context, real: List<Event>, today: LocalDate = LocalDate.now()): List<Event> {
    val samples = listOf(
        Event(id = -1, title = context.getString(R.string.widget_sample_title), type = EventType.COUNTDOWN, date = today.plusDays(23), startDate = today.minusDays(280)),
        Event(id = -2, title = context.getString(R.string.sample_project), type = EventType.COUNTDOWN, date = today.plusDays(9), startDate = today.minusDays(60)),
        Event(id = -3, title = context.getString(R.string.sample_birthday), type = EventType.AGE, date = today.minusYears(6).minusMonths(10).minusDays(20)),
        Event(id = -4, title = context.getString(R.string.sample_countup), type = EventType.COUNTUP, date = today.minusDays(894)),
    )
    return real + samples.filter { s -> real.none { it.type == s.type } }
}
