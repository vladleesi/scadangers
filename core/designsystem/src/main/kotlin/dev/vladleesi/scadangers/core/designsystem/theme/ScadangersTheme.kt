package dev.vladleesi.scadangers.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = LightScadangersColors.toColorScheme()
private val DarkColorScheme = DarkScadangersColors.toColorScheme()

private val LocalScadangersColors = staticCompositionLocalOf { LightScadangersColors }
private val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Applies Scadangers tokens and the matching Material 3 theme.
 *
 * @param darkTheme selects the dark palette; follows the system setting by default.
 * @param reducedMotion true when the user turned animations off; components then replace
 * continuous motion (such as indeterminate progress) with static presentations.
 */
@Composable
fun ScadangersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalScadangersColors provides
            if (darkTheme) DarkScadangersColors else LightScadangersColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            shapes = ScadangersShapes.material,
            typography = ScadangersTypography.material,
            content = content
        )
    }
}

/** Access to the current theme's tokens. Static tokens live in their own objects. */
object ScadangersTheme {
    val colors: ScadangersColors
        @Composable
        @ReadOnlyComposable
        get() = LocalScadangersColors.current

    val typography: ScadangersTypography
        get() = ScadangersTypography

    /** True when the system "Remove animations" setting is active. */
    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}
