package app.flipday.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import app.flipday.BuildConfig
import app.flipday.R
import app.flipday.data.SettingsStore
import app.flipday.ui.MainActivity
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A newer signed APK published as a GitHub Release. [code] is the versionCode, read from the tag (v0.1.14 means 14). */
data class UpdateInfo(val code: Int, val name: String, val url: String, val size: Long)

sealed interface UpdateOutcome {
    data object UpToDate : UpdateOutcome
    data class Ready(val info: UpdateInfo) : UpdateOutcome
    /** Newer version exists but the download did not finish. */
    data class Available(val info: UpdateInfo) : UpdateOutcome
    data object Failed : UpdateOutcome
}

/**
 * In-app updates for a sideloaded app (design 06G.4, 06G.5). Asks the public GitHub Releases API for the latest release,
 * downloads its APK into the app cache and leaves the install to Android, which always asks the person to confirm
 * and only accepts an APK signed with the same key as the installed app.
 */
object UpdateChecker {
    private const val REPO = "m-damer/flipday"
    private const val CHANNEL = "updates"
    private const val NOTIFICATION_ID = 7001

    fun latest(): UpdateInfo? = runCatching {
        val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "Flipday")
        }
        try {
            if (conn.responseCode != 200) return@runCatching null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val code = tag.substringAfterLast('.').toIntOrNull() ?: return@runCatching null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) return@runCatching UpdateInfo(code, tag.removePrefix("v"), a.getString("browser_download_url"), a.getLong("size"))
            }
            null
        } finally { conn.disconnect() }
    }.getOrNull()

    fun apkFile(context: Context, code: Int) = File(File(context.cacheDir, "updates"), "flipday-$code.apk")

    fun isReady(context: Context, code: Int) = code > 0 && apkFile(context, code).let { it.exists() && it.length() > 0 }

    /** Downloads into the cache. A half-finished file never counts: it is written as .part and renamed when the size matches. */
    private fun download(context: Context, info: UpdateInfo): Boolean {
        val target = apkFile(context, info.code)
        if (target.exists() && target.length() == info.size) return true
        target.parentFile?.apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val part = File(target.parentFile, target.name + ".part")
        return runCatching {
            val conn = (URL(info.url).openConnection() as HttpURLConnection).apply { connectTimeout = 15_000; readTimeout = 30_000; setRequestProperty("User-Agent", "Flipday") }
            try {
                conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
            } finally { conn.disconnect() }
            part.length() == info.size && part.renameTo(target)
        }.getOrDefault(false).also { if (!it) part.delete() }
    }

    /** One check: record what GitHub says, download when newer. Safe to call from a worker or from the Settings screen. */
    suspend fun checkNow(context: Context, store: SettingsStore): UpdateOutcome {
        val info = latest() ?: return UpdateOutcome.Failed
        if (info.code <= BuildConfig.VERSION_CODE) { store.setUpdate(0, ""); return UpdateOutcome.UpToDate }
        store.setUpdate(info.code, info.name)
        return if (download(context, info)) UpdateOutcome.Ready(info) else UpdateOutcome.Available(info)
    }

    /** The intent that installs the downloaded APK, or the one that lets the person allow installs from Flipday first. */
    fun installIntent(context: Context, code: Int): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".share", apkFile(context, code))
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** "Update ready": tapping opens Settings, where the Check for updates row offers Install. */
    fun notifyReady(context: Context, info: UpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_update_channel), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_UPDATE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.notif_update_title))
            .setContentText(context.getString(R.string.notif_update_text, info.name))
            .setContentIntent(open).setAutoCancel(true).build()
        val allowed = Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}
