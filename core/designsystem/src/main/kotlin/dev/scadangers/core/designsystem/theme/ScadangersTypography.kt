@file:Suppress("MagicNumber") // Token values are the named constants.

package dev.scadangers.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens.
 *
 * The platform sans-serif (Roboto on most devices) is used instead of a bundled font such as
 * Inter: it is licensed with the OS, covers every script the device supports, follows the
 * user's font-size and bold-text settings, and adds no APK size or font loading to startup.
 * Only three weights are used: Regular (400), Medium (500) and SemiBold (600). Sizes are in
 * `sp`, so every style scales with the system font size.
 */
object ScadangersTypography {
    private val Sans = FontFamily.Default

    /** Material roles used by components. */
    val material = Typography(
        headlineMedium = style(26, 32, FontWeight.SemiBold, letterSpacing = -0.4),
        headlineSmall = style(22, 28, FontWeight.SemiBold, letterSpacing = -0.3),
        titleLarge = style(20, 26, FontWeight.Medium, letterSpacing = -0.2),
        titleMedium = style(16, 22, FontWeight.SemiBold),
        titleSmall = style(14, 20, FontWeight.Medium),
        bodyLarge = style(16, 24, FontWeight.Normal),
        bodyMedium = style(14, 20, FontWeight.Normal),
        bodySmall = style(12, 16, FontWeight.Normal),
        labelLarge = style(14, 20, FontWeight.Medium),
        labelMedium = style(12, 16, FontWeight.Medium),
        labelSmall = style(11, 16, FontWeight.Medium, letterSpacing = 0.3)
    )

    /** Large metric values. Tabular figures keep changing numbers from shifting. */
    val metric: TextStyle = style(24, 30, FontWeight.SemiBold, letterSpacing = -0.3)
        .copy(fontFeatureSettings = "tnum")

    /** Technical identifiers such as patch levels, package names and addresses. */
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )

    private fun style(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0) =
        TextStyle(
            fontFamily = Sans,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.sp
        )
}
