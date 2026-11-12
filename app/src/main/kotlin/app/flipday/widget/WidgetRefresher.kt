package app.flipday.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
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

    fun scheduleNext(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = LocalDate.now().plusDays(1).atTime(0, 1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, pending(context))
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
            } finally { pending.finish() }
        }
    }
}
