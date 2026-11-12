package app.flipday.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val LocalFlap = staticCompositionLocalOf { LightFlap }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // snackbar
    small = RoundedCornerShape(10.dp),       // event plate, FAB
    medium = RoundedCornerShape(12.dp),      // buttons, chips, fields
    large = RoundedCornerShape(14.dp),       // grouped containers
    extraLarge = RoundedCornerShape(28.dp),  // dialogs
)

@Composable
fun FlipdayTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,           // off: the brand colour stays fixed (see design IA review)
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    CompositionLocalProvider(LocalFlap provides if (dark) DarkFlap else LightFlap) {
        MaterialTheme(colorScheme = scheme, typography = appTypography(arabic), shapes = AppShapes, content = content)
    }
}
