package app.flipday.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(fonts): the design uses IBM Plex Sans, IBM Plex Sans Arabic and Space Grotesk (numerals).
// Add the font files under res/font and replace the system families below. Until then the system sans is used.
val Plex: FontFamily = FontFamily.SansSerif
val PlexArabic: FontFamily = FontFamily.SansSerif
val Numerals: FontFamily = FontFamily.SansSerif

fun appTypography(arabic: Boolean): Typography {
    val f = if (arabic) PlexArabic else Plex
    val lh = if (arabic) 1.12f else 1f   // Arabic needs about 10-15% more line height
    fun s(size: Int, line: Int, w: FontWeight, fam: FontFamily = f) =
        TextStyle(fontFamily = fam, fontSize = size.sp, lineHeight = (line * lh).sp, fontWeight = w)
    return Typography(
        displayLarge = s(96, 88, FontWeight.SemiBold, Numerals), displayMedium = s(72, 68, FontWeight.SemiBold, Numerals),
        headlineLarge = s(28, 36, FontWeight.Bold), headlineSmall = s(24, 32, FontWeight.Bold),
        titleLarge = s(22, 28, FontWeight.SemiBold), titleMedium = s(16, 24, FontWeight.SemiBold),
        bodyLarge = s(16, 24, FontWeight.Normal), bodyMedium = s(14, 20, FontWeight.Normal),
        labelLarge = s(14, 20, FontWeight.SemiBold), labelMedium = s(12, 16, FontWeight.SemiBold), labelSmall = s(11, 16, FontWeight.SemiBold),
    )
}
