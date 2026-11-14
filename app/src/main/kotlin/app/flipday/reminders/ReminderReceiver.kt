package app.flipday.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.flipday.FlipdayApp
import app.flipday.R
import app.flipday.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val days = intent.getIntExtra(ReminderScheduler.EXTRA_DAYS_BEFORE, 0)
        if (id < 0 || title.isEmpty()) return
        if (intent.action == ACTION_SNOOZE) { snooze(context, id, title, days); return }

        val text = when (days) {
            0 -> context.getString(R.string.notif_today, title)
            1 -> context.getString(R.string.notif_tomorrow, title)
            else -> context.resources.getQuantityString(R.plurals.notif_in_days, days, days, title)
        }
        val open = PendingIntent.getActivity(
            context, id.toInt(), Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_snooze), PendingIntent.getBroadcast(
                context, id.toInt() xor SNOOZE_MASK,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_SNOOZE)
                    .putExtra(ReminderScheduler.EXTRA_ID, id).putExtra(ReminderScheduler.EXTRA_TITLE, title).putExtra(ReminderScheduler.EXTRA_DAYS_BEFORE, days),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            .build()
        val allowed = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(id.toInt(), n)

        // Schedule the next reminder for this event.
        val pending = goAsync()
        val container = (context.applicationContext as FlipdayApp).container
        CoroutineScope(Dispatchers.IO).launch {
            try { container.repository.all().firstOrNull { it.id == id }?.let { container.scheduler.schedule(it) } } finally { pending.finish() }
        }
    }

    /** "Remind me tomorrow": drop the notification and ask again at 09:00 tomorrow. Inexact, like every other reminder. */
    private fun snooze(context: Context, id: Long, title: String, days: Int) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
        val at = java.time.LocalDate.now().plusDays(1).atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val again = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderScheduler.EXTRA_ID, id).putExtra(ReminderScheduler.EXTRA_TITLE, title).putExtra(ReminderScheduler.EXTRA_DAYS_BEFORE, maxOf(0, days - 1))
        val pending = PendingIntent.getBroadcast(context, id.toInt() xor SNOOZE_MASK xor 0x10000000, again, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pending)
    }

    companion object {
        const val ACTION_SNOOZE = "app.flipday.action.SNOOZE"
        private const val SNOOZE_MASK = 0x20000000
    }
}

class ReschedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as FlipdayApp).container
        CoroutineScope(Dispatchers.IO).launch {
            try { container.repository.rescheduleAll() } finally { pending.finish() }
        }
    }
}
