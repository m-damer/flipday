package app.flipday.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.core.EventCalculator
import app.flipday.core.EventType
import app.flipday.ui.MainActivity
import java.time.LocalDate

/**
 * Phase 1 widget: days left to the nearest upcoming countdown, in flap tiles.
 * Phase 2: per-widget event choice, sizes and the studio (see docs/ROADMAP.md).
 * TODO: refresh at local midnight and on time change (WorkManager + receiver), then call FlipdayWidget().updateAll(context).
 */
class FlipdayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as FlipdayApp).container
        val today = LocalDate.now()
        val next = container.repository.all()
            .filter { it.archivedOn == null && it.type == EventType.COUNTDOWN && !it.date.isBefore(today) }
            .minByOrNull { it.date }
        val digits = next?.let { EventCalculator.daysUntil(it.date, today).toString().padStart(2, '0') }

        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color(0xFFFFFFFF)).padding(12.dp).clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                if (next == null || digits == null) {
                    Text(context.getString(R.string.widget_empty), style = TextStyle(color = ColorProvider(Color(0xFF1A1A1C)), fontSize = 14.sp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(next.title, maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xFF1A1A1C)), fontSize = 14.sp, fontWeight = FontWeight.Medium))
                        Row(modifier = GlanceModifier.padding(vertical = 6.dp)) {
                            digits.forEach { ch ->
                                Box(modifier = GlanceModifier.width(34.dp).height(52.dp).background(Color(0xFF050506)), contentAlignment = Alignment.Center) {
                                    Text(ch.toString(), style = TextStyle(color = ColorProvider(Color(0xFF5EE0C4)), fontSize = 34.sp, fontWeight = FontWeight.Bold))
                                }
                                Box(modifier = GlanceModifier.width(3.dp)) {}
                            }
                        }
                        Text(context.getString(R.string.label_days), style = TextStyle(color = ColorProvider(Color(0xFF625E54)), fontSize = 12.sp))
                    }
                }
            }
        }
    }
}

class FlipdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlipdayWidget()
}
