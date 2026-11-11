@file:Suppress("MagicNumber") // Token values are the named constants.

package dev.vladleesi.scadangers.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One semantic color role.
 *
 * @property content icons, borders, indicator bars and text on the app surface.
 * @property container tinted background for labels and banners.
 * @property onContainer text drawn on [container].
 */
@Immutable
data class StatusColors(val content: Color, val container: Color, val onContainer: Color)

/**
 * Scadangers color tokens. Every status role has exactly one meaning; never pick a role for
 * its hue alone.
 *
 * - [accent]: brand, primary actions, selection and focus. Never a status or warning.
 * - [positive]: protection verified active, or a completed check that passed. Never a
 *   default or placeholder state.
 * - [caution]: moderate severity, or a state that needs the user's attention.
 * - [critical]: critical severity, failed protection, or an assessment that failed.
 * - [info]: low severity and informational findings.
 * - [neutral]: inactive, unavailable, not assessed, or unknown.
 */
@Immutable
data class ScadangersColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val controlBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val positive: StatusColors,
    val caution: StatusColors,
    val critical: StatusColors,
    val info: StatusColors,
    val neutral: StatusColors
)

/** Light palette: warm neutral surfaces, thin borders and a restrained deep-orange accent. */
val LightScadangersColors = ScadangersColors(
    isDark = false,
    background = Color(0xFFFAF9F7),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFF4F2F0),
    border = Color(0xFFE6E2DE),
    controlBorder = Color(0xFF8C857F),
    textPrimary = Color(0xFF1C1A19),
    textSecondary = Color(0xFF57524E),
    textTertiary = Color(0xFF6F6964),
    accent = Color(0xFFC0410B),
    onAccent = Color(0xFFFFFFFF),
    accentContainer = Color(0xFFFCEADF),
    onAccentContainer = Color(0xFF6E2605),
    positive = StatusColors(Color(0xFF157F3B), Color(0xFFE8F5EC), Color(0xFF0F4D25)),
    caution = StatusColors(Color(0xFF8F6200), Color(0xFFFDF3D8), Color(0xFF5C3F00)),
    critical = StatusColors(Color(0xFFC42323), Color(0xFFFCEBEA), Color(0xFF7A1414)),
    info = StatusColors(Color(0xFF2759C4), Color(0xFFEAF0FC), Color(0xFF173A85)),
    neutral = StatusColors(Color(0xFF66605B), Color(0xFFEFECE9), Color(0xFF3A3633))
)

/** Dark palette: warm near-black surfaces with lifted, desaturated status colors. */
val DarkScadangersColors = ScadangersColors(
    isDark = true,
    background = Color(0xFF151414),
    surface = Color(0xFF1C1B1A),
    surfaceSubtle = Color(0xFF242221),
    border = Color(0xFF33302E),
    controlBorder = Color(0xFF7D7570),
    textPrimary = Color(0xFFF3EEEA),
    textSecondary = Color(0xFFBDB5B0),
    textTertiary = Color(0xFFA39B96),
    accent = Color(0xFFFF6A2B),
    onAccent = Color(0xFF1F0C02),
    accentContainer = Color(0xFF3A1D0E),
    onAccentContainer = Color(0xFFFFD4BF),
    positive = StatusColors(Color(0xFF4FD27F), Color(0xFF11291A), Color(0xFFB7F0CB)),
    caution = StatusColors(Color(0xFFF2C14E), Color(0xFF2E2410), Color(0xFFF9E3A8)),
    critical = StatusColors(Color(0xFFFF7B72), Color(0xFF36171A), Color(0xFFFFD0CC)),
    info = StatusColors(Color(0xFF7EA6FF), Color(0xFF16233F), Color(0xFFCBDAFF)),
    neutral = StatusColors(Color(0xFFA8A09A), Color(0xFF2A2725), Color(0xFFE3DDD8))
)

/** Maps tokens onto Material roles so stock Material 3 components inherit the palette. */
internal fun ScadangersColors.toColorScheme(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        inversePrimary = accent,
        secondary = textSecondary,
        onSecondary = surface,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        tertiary = info.content,
        onTertiary = surface,
        tertiaryContainer = info.container,
        onTertiaryContainer = info.onContainer,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSubtle,
        onSurfaceVariant = textSecondary,
        surfaceTint = surface,
        inverseSurface = textPrimary,
        inverseOnSurface = surface,
        error = critical.content,
        onError = surface,
        errorContainer = critical.container,
        onErrorContainer = critical.onContainer,
        outline = controlBorder,
        outlineVariant = border,
        surfaceBright = surface,
        surfaceDim = surfaceSubtle,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceSubtle
    )
}
