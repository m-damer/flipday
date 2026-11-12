package app.flipday.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.flipday.R

// Bundled fonts (SIL Open Font License, see docs/licenses): IBM Plex Sans, IBM Plex Sans Arabic, Space Grotesk for the flap digits.
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, w: FontWeight) = Font(res, weight = w, style = FontStyle.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

val Plex: FontFamily = FontFamily(Weights.map { variable(R.font.ibm_plex_sans, it) })
val PlexArabic: FontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal), Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold), Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)
val Numerals: FontFamily = FontFamily(Weights.map { variable(R.font.space_grotesk, it) })

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
