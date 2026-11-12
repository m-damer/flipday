package app.flipday.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Day counts change at local midnight, so widgets refresh then. An inexact alarm is enough: a few minutes of delay
 * is fine. Date, time and time-zone changes also refresh, because they move "today".
 */
object WidgetRefresher {
    const val ACTION_MIDNIGHT = "app.flipday.action.WIDGET_MIDNIGHT"
    const val ACTION_TICKER = "app.flipday.action.WIDGET_TICKER"
    private const val TICKER_EVERY_MS = 15 * 60 * 1000L

    fun scheduleNext(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = LocalDate.now().plusDays(1).atTime(0, 1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, pending(context))
    }

    /** Live-ticker widgets show minutes, so they refresh about every 15 minutes (inexact alarm, no exact-alarm permission). */
    suspend fun scheduleTickerIfNeeded(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val any = manager.getGlanceIds(FlipdayWidget::class.java).any { id ->
            runCatching { WidgetTemplate.parse(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[WidgetKeys.TEMPLATE]) }
                .getOrNull() == WidgetTemplate.TICKER
        }
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 1, Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_TICKER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (any) alarms.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + TICKER_EVERY_MS, pending) else alarms.cancel(pending)
    }

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0, Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                FlipdayWidget().updateAll(app)
                WidgetRefresher.scheduleNext(app)
                WidgetRefresher.scheduleTickerIfNeeded(app)
            } finally { pending.finish() }
        }
    }
}
