package app.flipday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import app.flipday.ui.theme.LocalFlap
import app.flipday.ui.theme.Numerals

/**
 * One split-flap tile per character. Digits always read left to right, also in Arabic.
 * The whole row is ONE accessibility element: screen readers announce [description], not digit by digit.
 * Digit size follows the tile height in dp, not the font scale: at 200% the surrounding text grows but a digit must never outgrow its tile.
 */
@Composable
fun FlipNumber(text: String, description: String, tileHeight: Dp = 44.dp, modifier: Modifier = Modifier) {
    val flap = LocalFlap.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.semantics { contentDescription = description }.clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            text.forEach { ch ->
                Box(
                    modifier = Modifier
                        .width(tileHeight * 0.66f).height(tileHeight)
                        .background(flap.tile, RoundedCornerShape(tileHeight / 9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ch.toString(), color = flap.digit, fontFamily = Numerals, fontWeight = FontWeight.SemiBold, fontSize = with(LocalDensity.current) { (tileHeight * 0.72f).toSp() })
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black.copy(alpha = 0.55f)))
                }
            }
        }
    }
}
