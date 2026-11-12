package app.flipday.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.flipday.R
import app.flipday.core.Event
import app.flipday.core.ReminderPlanner
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the next reminder per event with an INEXACT alarm. An event whose reminder time was changed from the default
 * (the opt-in "reminder time" feature) gets an exact alarm, but only when the user has allowed exact alarms.
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
        val pending = pendingIntent(event.id, event.title, next.daysBefore)
        if (wantsExact(event) && canScheduleExact()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    private fun wantsExact(event: Event) = event.reminders.any { it.at != DEFAULT_TIME }

    fun cancel(eventId: Long) {
        alarms.cancel(pendingIntent(eventId, "", 0))
    }

    private fun pendingIntent(id: Long, title: String, daysBefore: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_DAYS_BEFORE, daysBefore)
        return PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /** Reminder time of every event unless the person picks another one. */
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
        const val CHANNEL_ID = "event_reminders"
        const val EXTRA_ID = "event_id"
        const val EXTRA_TITLE = "event_title"
        const val EXTRA_DAYS_BEFORE = "days_before"
    }
}
