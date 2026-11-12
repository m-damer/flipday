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
