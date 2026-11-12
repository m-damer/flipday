package app.flipday.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
) {
    val dark = isSystemInDarkTheme()
    val flap = template == WidgetTemplate.FLAP
    val bg = if (flap) (if (dark) Color(0xFF171719) else Color.White) else Color(if (dark) accent.bgNight else accent.bgDay)
    val ink = if (flap) (if (dark) Color(0xFFF2EFE6) else Color(0xFF1A1A1C)) else Color.White
    val muted = if (flap) (if (dark) Color(0xFFB8B4A8) else Color(0xFF625E54)) else Color(0xFFE8F4F1)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description }.background(bg, RoundedCornerShape(24.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (showTitle) Text(title, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                digits.forEach { ch ->
                    if (flap) Box(Modifier.width(34.dp).height(52.dp).background(Color(0xFF050506), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                        Text(ch.toString(), color = Color(accent.digit), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    } else Text(ch.toString(), color = ink, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showUnit) Text(unit, color = muted, fontSize = 12.sp)
    }
}
