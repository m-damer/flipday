package app.flipday.ui.events

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.flipday.R
import java.io.File
import kotlin.math.min

enum class ShareFormat(val width: Int, val height: Int) { SQUARE(1080, 1080), STORY(1080, 1920), WIDE(1920, 1080) }

/** Colours come from the Compose theme (the caller passes them), so the card matches light and dark. */
data class SharePalette(val background: Int, val ink: Int, val muted: Int, val accent: Int, val tile: Int, val digit: Int)

/** What count-up and period cards add (design 06G.3): a milestone chip, or a progress bar with its label. */
data class ShareExtras(val chip: String? = null, val progress: Float? = null, val progressLabel: String? = null)

/** Draws the share card (design 06F.1, 06G.3): caption, title, flap tiles, unit and date, app name. */
object ShareCard {
    fun render(
        context: Context, format: ShareFormat, title: String, digits: String, unit: String, subtitle: String,
        caption: String?, appName: String, p: SharePalette, extras: ShareExtras = ShareExtras(),
    ): Bitmap {
        val w = format.width; val h = format.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(p.background)
        val base = min(w, h).toFloat()
        val pad = base * 0.08f
        val regular = ResourcesCompat.getFont(context, R.font.ibm_plex_sans) ?: Typeface.DEFAULT
        val bold = Typeface.create(regular, Typeface.BOLD)
        val numerals = ResourcesCompat.getFont(context, R.font.space_grotesk)?.let { Typeface.create(it, Typeface.BOLD) } ?: bold

        fun paint(color: Int, size: Float, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = size; typeface = face }
        fun layout(text: String, tp: TextPaint, width: Int, maxLines: Int) =
            StaticLayout.Builder.obtain(text, 0, text.length, tp, width).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

        caption?.let { c.drawText(it, pad, pad + base * 0.035f, paint(p.accent, base * 0.032f, bold).apply { letterSpacing = 0.12f }) }
        c.drawText(appName, pad, h - pad, paint(p.muted, base * 0.032f, bold))

        // Tiles: as large as fits, always left to right.
        val gap = base * 0.012f
        val tileH = min(base * 0.30f, (w - 2 * pad) / (digits.length * 0.66f + (digits.length - 1) * 0.02f + 0.02f))
        val tileW = tileH * 0.66f
        val rowW = digits.length * tileW + (digits.length - 1) * gap

        val titleLayout = layout(title, paint(p.ink, base * 0.07f, bold), (w - 2 * pad).toInt(), 3)
        val unitLayout = layout("$unit · $subtitle", paint(p.muted, base * 0.036f, regular), (w - 2 * pad).toInt(), 2)
        val chipPaint = paint(p.accent, base * 0.034f, bold)
        val labelLayout = extras.progressLabel?.let { layout(it, paint(p.muted, base * 0.032f, regular), (w - 2 * pad).toInt(), 1) }
        val barH = base * 0.016f
        var extraH = 0f
        if (extras.progress != null) extraH += base * 0.03f + barH + (labelLayout?.let { base * 0.015f + it.height } ?: 0f)
        if (extras.chip != null) extraH += base * 0.03f + base * 0.085f
        val blockH = titleLayout.height + base * 0.04f + tileH + base * 0.03f + unitLayout.height + extraH
        var y = (h - blockH) / 2f

        c.save(); c.translate(pad, y); titleLayout.draw(c); c.restore()
        y += titleLayout.height + base * 0.04f

        val tileBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.tile }
        val line = Paint().apply { color = 0x8C000000.toInt(); strokeWidth = base * 0.004f }
        val digitPaint = paint(p.digit, tileH * 0.72f, numerals).apply { textAlign = Paint.Align.CENTER }
        var x = (w - rowW) / 2f
        digits.forEach { ch ->
            c.drawRoundRect(RectF(x, y, x + tileW, y + tileH), tileH / 9, tileH / 9, tileBg)
            val baseline = y + tileH / 2f - (digitPaint.descent() + digitPaint.ascent()) / 2f
            c.drawText(ch.toString(), x + tileW / 2f, baseline, digitPaint)
            c.drawLine(x, y + tileH / 2f, x + tileW, y + tileH / 2f, line)
            x += tileW + gap
        }
        y += tileH + base * 0.03f
        c.save(); c.translate(pad, y); unitLayout.draw(c); c.restore()
        y += unitLayout.height
        if (extras.progress != null) {
            y += base * 0.03f
            val barW = (w - 2 * pad) * 0.6f
            val left = (w - barW) / 2f
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (p.muted and 0x00FFFFFF) or 0x33000000 }
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent }
            c.drawRoundRect(RectF(left, y, left + barW, y + barH), barH / 2, barH / 2, track)
            c.drawRoundRect(RectF(left, y, left + barW * extras.progress.coerceIn(0f, 1f), y + barH), barH / 2, barH / 2, fill)
            y += barH
            labelLayout?.let { y += base * 0.015f; c.save(); c.translate(pad, y); it.draw(c); c.restore(); y += it.height }
        }
        extras.chip?.let { chip ->
            y += base * 0.03f
            val textW = chipPaint.measureText(chip)
            val chipH = base * 0.085f
            val left = (w - (textW + base * 0.07f)) / 2f
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent; style = Paint.Style.STROKE; strokeWidth = base * 0.004f }
            c.drawRoundRect(RectF(left, y, left + textW + base * 0.07f, y + chipH), chipH / 2, chipH / 2, stroke)
            c.drawText(chip, left + base * 0.035f, y + chipH / 2f - (chipPaint.descent() + chipPaint.ascent()) / 2f, chipPaint)
        }
        return bmp
    }

    /** Writes [bitmap] to the app cache and opens the system share sheet with the image and [text]. */
    fun share(context: Context, bitmap: Bitmap, text: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "flipday-${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".share", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
