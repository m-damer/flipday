package app.flipday.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.ReminderPlanner
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules the next reminder per event with an INEXACT alarm. Exact alarms are a later, opt-in feature.
 * One pending alarm per event id (request code = id); the receiver reschedules the one after it.
 */
class ReminderScheduler(private val context: Context) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun schedule(event: Event, now: LocalDateTime = LocalDateTime.now()) {
        cancel(event.id)
        val next = ReminderPlanner.upcoming(event, now).firstOrNull()
            ?: ReminderPlanner.milestoneTrigger(event, now.toLocalDate())
            ?: return
        val millis = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(event.id, event.title, next.daysBefore))
    }

    fun cancel(eventId: Long) {
        alarms.cancel(pendingIntent(eventId, "", 0))
    }

    private fun pendingIntent(id: Long, title: String, daysBefore: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_DAYS_BEFORE, daysBefore)
        return PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val EXTRA_ID = "event_id"
        const val EXTRA_TITLE = "event_title"
        const val EXTRA_DAYS_BEFORE = "days_before"
    }
}
