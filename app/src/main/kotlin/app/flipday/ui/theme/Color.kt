package app.flipday.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object TealLight {
    val primary = Color(0xFF0F766E)
    val onPrimary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFCFDCD4)
    val onSecondaryContainer = Color(0xFF1A1A1C)
    val background = Color(0xFFF3F0E8)
    val surface = Color(0xFFFFFFFF)
    val onSurface = Color(0xFF1A1A1C)
    val onSurfaceVariant = Color(0xFF625E54)
    val outline = Color(0xFF7A7569)
    val outlineVariant = Color(0xFFDAD6CB)
    val error = Color(0xFFB3261C)
    val onError = Color(0xFFFFFFFF)
    val inverseSurface = Color(0xFF1A1A1C)
    val inverseOnSurface = Color(0xFFF3F0E8)
    val inversePrimary = Color(0xFF5EE0C4)
    val flapTile = Color(0xFF050506)
    val flapDigit = Color(0xFF5EE0C4)
}
object TealDark {
    val primary = Color(0xFF4FD1B5)
    val onPrimary = Color(0xFF171719)
    val secondaryContainer = Color(0xFF23403B)
    val onSecondaryContainer = Color(0xFFF2EFE6)
    val background = Color(0xFF171719)
    val surface = Color(0xFF242427)
    val onSurface = Color(0xFFF2EFE6)
    val onSurfaceVariant = Color(0xFFA39F94)
    val outline = Color(0xFF8A857A)
    val outlineVariant = Color(0xFF37373B)
    val error = Color(0xFFF2B8B5)
    val onError = Color(0xFF601410)
    val inverseSurface = Color(0xFFF2EFE6)
    val inverseOnSurface = Color(0xFF171719)
    val inversePrimary = Color(0xFF0F766E)
    val flapTile = Color(0xFF050506)
    val flapDigit = Color(0xFF5EE0C4)
}

/** Custom roles that Material 3 does not have. */
@Immutable
data class FlapColors(val tile: Color, val digit: Color)

val LightFlap = FlapColors(TealLight.flapTile, TealLight.flapDigit)
val DarkFlap = FlapColors(TealDark.flapTile, TealDark.flapDigit)

val LightColors: ColorScheme = lightColorScheme(
    primary = TealLight.primary, onPrimary = TealLight.onPrimary,
    secondaryContainer = TealLight.secondaryContainer, onSecondaryContainer = TealLight.onSecondaryContainer,
    background = TealLight.background, onBackground = TealLight.onSurface,
    surface = TealLight.surface, onSurface = TealLight.onSurface, onSurfaceVariant = TealLight.onSurfaceVariant,
    outline = TealLight.outline, outlineVariant = TealLight.outlineVariant,
    error = TealLight.error, onError = TealLight.onError,
    inverseSurface = TealLight.inverseSurface, inverseOnSurface = TealLight.inverseOnSurface, inversePrimary = TealLight.inversePrimary,
    // Roles the design does not list are mapped to its tokens so Material's default purple never shows.
    primaryContainer = TealLight.secondaryContainer, onPrimaryContainer = TealLight.onSecondaryContainer,
    secondary = TealLight.primary, onSecondary = TealLight.onPrimary,
    tertiary = TealLight.primary, onTertiary = TealLight.onPrimary,
    tertiaryContainer = TealLight.secondaryContainer, onTertiaryContainer = TealLight.onSecondaryContainer,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    surfaceVariant = TealLight.outlineVariant, surfaceTint = TealLight.primary, scrim = Color(0xFF000000),
    surfaceBright = TealLight.surface, surfaceDim = TealLight.background,
    surfaceContainerLowest = TealLight.surface, surfaceContainerLow = TealLight.surface, surfaceContainer = TealLight.surface,
    surfaceContainerHigh = TealLight.surface, surfaceContainerHighest = Color(0xFFECE8DE),
)
val DarkColors: ColorScheme = darkColorScheme(
    primary = TealDark.primary, onPrimary = TealDark.onPrimary,
    secondaryContainer = TealDark.secondaryContainer, onSecondaryContainer = TealDark.onSecondaryContainer,
    background = TealDark.background, onBackground = TealDark.onSurface,
    surface = TealDark.surface, onSurface = TealDark.onSurface, onSurfaceVariant = TealDark.onSurfaceVariant,
    outline = TealDark.outline, outlineVariant = TealDark.outlineVariant,
    error = TealDark.error, onError = TealDark.onError,
    inverseSurface = TealDark.inverseSurface, inverseOnSurface = TealDark.inverseOnSurface, inversePrimary = TealDark.inversePrimary,
    primaryContainer = TealDark.secondaryContainer, onPrimaryContainer = TealDark.onSecondaryContainer,
    secondary = TealDark.primary, onSecondary = TealDark.onPrimary,
    tertiary = TealDark.primary, onTertiary = TealDark.onPrimary,
    tertiaryContainer = TealDark.secondaryContainer, onTertiaryContainer = TealDark.onSecondaryContainer,
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    surfaceVariant = TealDark.outlineVariant, surfaceTint = TealDark.primary, scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2D2D30), surfaceDim = TealDark.background,
    surfaceContainerLowest = TealDark.background, surfaceContainerLow = TealDark.surface, surfaceContainer = TealDark.surface,
    surfaceContainerHigh = TealDark.surface, surfaceContainerHighest = Color(0xFF2D2D30),
)
