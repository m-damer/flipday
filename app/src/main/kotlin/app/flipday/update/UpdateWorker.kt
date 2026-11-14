package app.flipday.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.flipday.FlipdayApp
import java.util.concurrent.TimeUnit

/** Background update check. Runs without the app open; at most one notification per new version. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = (applicationContext as FlipdayApp).container.settings
        return when (val r = UpdateChecker.checkNow(applicationContext, store)) {
            is UpdateOutcome.Ready -> {
                if (store.readNotifiedCode() != r.info.code) { UpdateChecker.notifyReady(applicationContext, r.info); store.setNotifiedCode(r.info.code) }
                Result.success()
            }
            is UpdateOutcome.Available, UpdateOutcome.Failed -> if (runAttemptCount < 2) Result.retry() else Result.success()
            UpdateOutcome.UpToDate -> Result.success()
        }
    }
}

object UpdateScheduler {
    private const val NAME = "flipday-update-check"

    /** Every 3.5 days is twice a week. Any network, because the check is a few kilobytes; Android may shift it to save battery. */
    fun apply(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(NAME); return }
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(84, TimeUnit.HOURS, 12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
