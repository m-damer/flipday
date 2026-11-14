package app.flipday.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceTheme
import androidx.glance.text.FontFamily
import androidx.glance.unit.ColorProvider
import java.io.File
import androidx.glance.color.ColorProvider as DayNight

/** Studio choices that apply to a whole widget, read once per render and passed down as composition locals. */
internal enum class BgMode { SOLID, GRADIENT, PHOTO, CLEAR }
internal enum class TimeMode { DAYS, WEEKS, YMD, LIVE }
internal enum class FontChoice(val family: FontFamily) { SANS(FontFamily.SansSerif), SERIF(FontFamily.Serif), MONO(FontFamily.Monospace) }
internal enum class NumberSize(val scale: Float) { SMALL(0.8f), MEDIUM(1f), LARGE(1.15f), XL(1.3f) }

internal data class WidgetStyle(
    val dynamic: Boolean = false,
    val bg: BgMode = BgMode.SOLID,
    val bgImage: Bitmap? = null,
    val radiusDp: Int = SYSTEM_RADIUS,
    val numberSize: NumberSize = NumberSize.MEDIUM,
    val font: FontChoice = FontChoice.SANS,
    val showDate: Boolean = true,
    val showProgress: Boolean = true,
    val timeMode: TimeMode = TimeMode.DAYS,
) {
    /** Text sits on a picture or on the wallpaper, so it must be light. */
    val onMedia: Boolean get() = bg != BgMode.SOLID
    companion object { const val SYSTEM_RADIUS = 16 }
}

internal class Palette(
    val surface: ColorProvider, val ink: ColorProvider, val muted: ColorProvider, val divider: ColorProvider,
    val chipBg: ColorProvider, val chipInk: ColorProvider,
)

internal val LocalStyle = staticCompositionLocalOf { WidgetStyle() }
internal val LocalPalette = compositionLocalOf<Palette> { error("palette not provided") }

private fun dn(day: Long, night: Long) = DayNight(Color(day), Color(night))
private fun c(v: Long) = ColorProvider(Color(v))

/** The design tokens (board CD-04) as widget colours. */
internal val StaticPalette = Palette(
    surface = dn(0xFFFFFFFF, 0xFF242427), ink = dn(0xFF1A1A1C, 0xFFF2EFE6), muted = dn(0xFF625E54, 0xFFA39F94),
    divider = dn(0xFFDAD6CB, 0xFF37373B), chipBg = dn(0xFFCFDCD4, 0xFF23403B), chipInk = dn(0xFF1A1A1C, 0xFFF2EFE6),
)
private val MediaPalette = Palette(
    surface = c(0x00000000), ink = c(0xFFFFFFFF), muted = c(0xFFE3E0D6), divider = c(0x55FFFFFF), chipBg = c(0x33FFFFFF), chipInk = c(0xFFFFFFFF),
)

@Composable
internal fun paletteFor(style: WidgetStyle): Palette = when {
    style.onMedia -> MediaPalette
    style.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val t = GlanceTheme.colors
        Palette(t.surface, t.onSurface, t.onSurfaceVariant, t.surfaceVariant, t.secondaryContainer, t.onSecondaryContainer)
    }
    else -> StaticPalette
}

/** Accent for labels, bars and highlights. */
@Composable
internal fun accent(a: WidgetAccent): ColorProvider {
    val st = LocalStyle.current
    return when {
        st.onMedia -> ColorProvider(Color(a.digit))
        st.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlanceTheme.colors.primary
        else -> dn(a.bgDay, a.bgNight)
    }
}

internal fun photoFile(context: Context, appWidgetId: Int) = File(context.filesDir, "widget_bg_$appWidgetId.jpg")

/** Decodes the saved photo, or draws the accent gradient. Both are small so they fit in a RemoteViews update. */
internal fun backgroundBitmap(context: Context, mode: BgMode, accent: WidgetAccent, appWidgetId: Int): Bitmap? = when (mode) {
    BgMode.GRADIENT -> {
        val bmp = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
        val top = accent.bgDay.toInt()
        val bottom = darken(top, 0.55f)
        Canvas(bmp).drawRect(0f, 0f, 360f, 360f, Paint().apply { shader = LinearGradient(0f, 0f, 360f, 360f, top, bottom, Shader.TileMode.CLAMP) })
        bmp
    }
    BgMode.PHOTO -> photoFile(context, appWidgetId).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
    else -> null
}

private fun darken(color: Int, f: Float): Int {
    val r = ((color shr 16 and 0xFF) * f).toInt(); val g = ((color shr 8 and 0xFF) * f).toInt(); val b = ((color and 0xFF) * f).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

/** Saves a picked picture at a size that fits in the widget, with a dark layer so white text stays readable. */
internal fun savePhoto(context: Context, appWidgetId: Int, source: Bitmap) {
    val max = 600f
    val scale = (max / maxOf(source.width, source.height)).coerceAtMost(1f)
    val scaled = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
    val out = scaled.copy(Bitmap.Config.ARGB_8888, true)
    Canvas(out).drawColor(0x66000000)
    photoFile(context, appWidgetId).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
}

internal fun styleFrom(prefs: Preferences, context: Context, appWidgetId: Int, accent: WidgetAccent): WidgetStyle {
    val bg = BgMode.entries.firstOrNull { it.name == prefs[WidgetKeys.BG_MODE] } ?: BgMode.SOLID
    return WidgetStyle(
        dynamic = prefs[WidgetKeys.DYNAMIC] ?: false,
        bg = bg, bgImage = backgroundBitmap(context, bg, accent, appWidgetId),
        radiusDp = prefs[WidgetKeys.RADIUS] ?: WidgetStyle.SYSTEM_RADIUS,
        numberSize = NumberSize.entries.firstOrNull { it.name == prefs[WidgetKeys.NUMBER_SIZE] } ?: NumberSize.MEDIUM,
        font = FontChoice.entries.firstOrNull { it.name == prefs[WidgetKeys.FONT] } ?: FontChoice.SANS,
        showDate = prefs[WidgetKeys.SHOW_DATE] ?: true,
        showProgress = prefs[WidgetKeys.SHOW_PROGRESS] ?: true,
        timeMode = TimeMode.entries.firstOrNull { it.name == prefs[WidgetKeys.TIME_MODE] } ?: TimeMode.DAYS,
    )
}
