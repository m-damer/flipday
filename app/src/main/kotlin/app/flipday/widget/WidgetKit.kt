package app.flipday.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNight

internal val TileBg = ColorProvider(Color(0xFF050506))
internal val Card: ColorProvider @Composable get() = LocalPalette.current.surface
internal val Ink: ColorProvider @Composable get() = LocalPalette.current.ink
internal val Muted: ColorProvider @Composable get() = LocalPalette.current.muted
internal val Divider: ColorProvider @Composable get() = LocalPalette.current.divider
internal val ChipBg: ColorProvider @Composable get() = LocalPalette.current.chipBg
internal val ChipInk: ColorProvider @Composable get() = LocalPalette.current.chipInk

internal fun digitColor(a: WidgetAccent) = ColorProvider(Color(a.digit))

/** Size class from the Glance responsive size (one of the sizes in the widget's size set). */
internal fun sizeClass(size: DpSize): WidgetSize = when {
    size.width < 200.dp -> WidgetSize.SMALL
    size.height < 90.dp -> WidgetSize.STRIP
    size.height < 200.dp -> WidgetSize.MEDIUM
    else -> WidgetSize.LARGE
}

private fun isRtl(context: android.content.Context) =
    context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL

/** Flap tiles. Digits read left to right in every language, so the order is flipped when the layout is mirrored. */
@Composable
internal fun Tiles(digits: String, baseHeight: Dp, accent: WidgetAccent, gap: Dp = 3.dp) {
    val st = LocalStyle.current
    val ordered = if (isRtl(LocalContext.current)) digits.toList().asReversed() else digits.toList()
    // Number size scales the tiles, but never past about 60% of the widget width so a long number still fits.
    val maxWidth = LocalSize.current.width * 0.6f
    val widthPer = ordered.size.coerceAtLeast(1) * 0.66f
    val height = minOf(baseHeight * st.numberSize.scale, (maxWidth - gap * ordered.size) / widthPer).coerceAtLeast(baseHeight * 0.5f)
    Row {
        ordered.forEachIndexed { i, ch ->
            if (i > 0) Box(GlanceModifier.width(gap)) {}
            Box(
                GlanceModifier.width(height * 0.66f).height(height).background(TileBg).cornerRadius(height * 0.11f),
                contentAlignment = Alignment.Center,
            ) { Text(ch.toString(), style = TextStyle(color = digitColor(accent), fontSize = (height.value * 0.62f).sp, fontWeight = FontWeight.Bold, fontFamily = st.font.family)) }
        }
    }
}

/** Small uppercase label in the accent colour ("UPCOMING", "DUE IN"). */
@Composable
internal fun Label(text: String, a: WidgetAccent, size: Int = 10) =
    Text(text.uppercase(), maxLines = 1, style = TextStyle(color = accent(a), fontSize = size.sp, fontWeight = FontWeight.Bold, fontFamily = LocalStyle.current.font.family))

@Composable
internal fun Title(text: String, size: Int = 14, lines: Int = 1) =
    Text(text, maxLines = lines, style = TextStyle(color = Ink, fontSize = size.sp, fontWeight = FontWeight.Bold, fontFamily = LocalStyle.current.font.family))

@Composable
internal fun Sub(text: String, size: Int = 11) =
    Text(text, maxLines = 1, style = TextStyle(color = Muted, fontSize = size.sp, fontFamily = LocalStyle.current.font.family))

@Composable
internal fun Bar(progress: Float, a: WidgetAccent, modifier: GlanceModifier = GlanceModifier) {
    if (!LocalStyle.current.showProgress) return
    LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = modifier, color = accent(a), backgroundColor = Divider)
}

/** A date line that follows the studio's "Date" switch. */
@Composable
internal fun DateSub(text: String, size: Int = 11) { if (LocalStyle.current.showDate) Sub(text, size) }

/** Rounded pill with text, used for "10 months", "365". */
@Composable
internal fun Chip(text: String) {
    Box(GlanceModifier.background(ChipBg).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
        Text(text, maxLines = 1, style = TextStyle(color = ChipInk, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}
