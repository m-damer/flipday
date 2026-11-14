package app.flipday.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-app preview that mirrors the home-screen widget (same colours, same small layout).
 * Colours come from [WidgetAccent] because Glance cannot read the Compose theme; the real widget uses the same values.
 */
@Composable
fun WidgetPreview(
    title: String, digits: String, unit: String, description: String,
    template: WidgetTemplate, accent: WidgetAccent, showTitle: Boolean, showUnit: Boolean,
    modifier: Modifier = Modifier,
    progress: Float = 0.92f,
) {
    val dark = isSystemInDarkTheme()
    val flap = true
    val dim = if (flap) (if (dark) Color(0xFF37373B) else Color(0xFFDAD6CB)) else Color(0x33FFFFFF)
    val arc = if (dark) Color(accent.bgNight) else Color(accent.bgDay)
    val bg = if (flap) (if (dark) Color(0xFF242427) else Color.White) else Color(if (dark) accent.bgNight else accent.bgDay)
    val ink = if (flap) (if (dark) Color(0xFFF2EFE6) else Color(0xFF1A1A1C)) else Color.White
    val muted = if (flap) (if (dark) Color(0xFFA39F94) else Color(0xFF625E54)) else Color(0xFFE8F4F1)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description }.background(bg, RoundedCornerShape(24.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (showTitle && template != WidgetTemplate.RING) Text(title, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (template == WidgetTemplate.RING) Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 9.dp.toPx()
                val inset = w / 2
                val box = Size(size.width - w, size.height - w)
                drawArc(dim, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(w, cap = StrokeCap.Round))
                if (progress > 0f) drawArc(arc, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), box, style = Stroke(w, cap = StrokeCap.Round))
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    digits.forEach { ch ->
                        Box(Modifier.width(22.dp).height(34.dp).background(Color(0xFF050506), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(ch.toString(), color = Color(accent.digit), fontSize = with(LocalDensity.current) { 22.dp.toSp() }, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                digits.forEach { ch ->
                    if (flap) Box(Modifier.width(34.dp).height(52.dp).background(Color(0xFF050506), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                        Text(ch.toString(), color = Color(accent.digit), fontSize = with(LocalDensity.current) { 34.dp.toSp() }, fontWeight = FontWeight.Bold)
                    } else Text(ch.toString(), color = ink, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showUnit) Text(unit, color = muted, fontSize = 12.sp)
    }
}
