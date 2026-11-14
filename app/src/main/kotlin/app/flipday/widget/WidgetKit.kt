package app.flipday.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
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

// Glance cannot read the Compose theme, so the design tokens (board CD-04) are repeated here. Only the widget code may do this.
internal fun dn(day: Long, night: Long) = DayNight(Color(day), Color(night))

internal val TileBg = ColorProvider(Color(0xFF050506))
internal val Card = dn(0xFFFFFFFF, 0xFF242427)          // surface
internal val Ink = dn(0xFF1A1A1C, 0xFFF2EFE6)           // onSurface
internal val Muted = dn(0xFF625E54, 0xFFA39F94)         // onSurfaceVariant
internal val Divider = dn(0xFFDAD6CB, 0xFF37373B)       // outlineVariant
internal val ChipBg = dn(0xFFCFDCD4, 0xFF23403B)        // secondaryContainer
internal val ChipInk = dn(0xFF1A1A1C, 0xFFF2EFE6)       // onSecondaryContainer

/** Accent for labels, bars and highlights. Teal uses the design's primary pair; studio accents use their own pair. */
internal fun accent(a: WidgetAccent) = dn(a.bgDay, a.bgNight)
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
internal fun Tiles(digits: String, height: Dp, accent: WidgetAccent, gap: Dp = 3.dp) {
    val ordered = if (isRtl(LocalContext.current)) digits.toList().asReversed() else digits.toList()
    Row {
        ordered.forEachIndexed { i, ch ->
            if (i > 0) Box(GlanceModifier.width(gap)) {}
            Box(
                GlanceModifier.width(height * 0.66f).height(height).background(TileBg).cornerRadius(height * 0.11f),
                contentAlignment = Alignment.Center,
            ) { Text(ch.toString(), style = TextStyle(color = digitColor(accent), fontSize = (height.value * 0.62f).sp, fontWeight = FontWeight.Bold)) }
        }
    }
}

/** Small uppercase label in the accent colour ("UPCOMING", "DUE IN"). */
@Composable
internal fun Label(text: String, a: WidgetAccent, size: Int = 10) =
    Text(text.uppercase(), maxLines = 1, style = TextStyle(color = accent(a), fontSize = size.sp, fontWeight = FontWeight.Bold))

@Composable
internal fun Title(text: String, size: Int = 14, lines: Int = 1) =
    Text(text, maxLines = lines, style = TextStyle(color = Ink, fontSize = size.sp, fontWeight = FontWeight.Bold))

@Composable
internal fun Sub(text: String, size: Int = 11) =
    Text(text, maxLines = 1, style = TextStyle(color = Muted, fontSize = size.sp))

@Composable
internal fun Bar(progress: Float, a: WidgetAccent, modifier: GlanceModifier = GlanceModifier) =
    LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = modifier, color = accent(a), backgroundColor = Divider)

/** Rounded pill with text, used for "10 months", "365". */
@Composable
internal fun Chip(text: String) {
    Box(GlanceModifier.background(ChipBg).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
        Text(text, maxLines = 1, style = TextStyle(color = ChipInk, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}
