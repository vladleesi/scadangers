package dev.vladleesi.scadangers.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.vladleesi.scadangers.core.designsystem.theme.DarkScadangersColors
import dev.vladleesi.scadangers.core.designsystem.theme.LightScadangersColors
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersColors
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.x contrast requirements for token pairs that render text or meaningful graphics. */
class ColorContrastTest {
    private val palettes = listOf(LightScadangersColors, DarkScadangersColors)

    @Test
    fun textMeetsAaOnEverySurface() = palettes.forEach { colors ->
        val surfaces = listOf(colors.background, colors.surface, colors.surfaceSubtle)
        val text =
            listOf(colors.textPrimary, colors.textSecondary, colors.textTertiary, colors.accent)
        surfaces.forEach { surface ->
            text.forEach { assertContrast(it, surface, TEXT_AA, colors) }
        }
    }

    @Test
    fun statusRolesMeetAa() = palettes.forEach { colors ->
        colors.statusRoles().forEach { role ->
            assertContrast(role.onContainer, role.container, TEXT_AA, colors)
            assertContrast(role.content, colors.surface, TEXT_AA, colors)
            assertContrast(role.content, colors.background, TEXT_AA, colors)
            assertContrast(role.content, role.container, NON_TEXT_AA, colors)
        }
    }

    @Test
    fun accentActionsMeetAa() = palettes.forEach { colors ->
        assertContrast(colors.onAccent, colors.accent, TEXT_AA, colors)
        assertContrast(colors.onAccentContainer, colors.accentContainer, TEXT_AA, colors)
    }

    @Test
    fun controlBoundariesMeetNonTextContrast() = palettes.forEach { colors ->
        assertContrast(colors.controlBorder, colors.surface, NON_TEXT_AA, colors)
        assertContrast(colors.controlBorder, colors.background, NON_TEXT_AA, colors)
    }

    private fun ScadangersColors.statusRoles() = listOf(positive, caution, critical, info, neutral)

    private fun assertContrast(
        foreground: Color,
        background: Color,
        minimum: Double,
        colors: ScadangersColors
    ) {
        val ratio = contrast(foreground, background)
        val theme = if (colors.isDark) "dark" else "light"
        assertTrue("$theme: $foreground on $background is $ratio, needs $minimum", ratio >= minimum)
    }

    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (max(a, b) + LUMINANCE_OFFSET) / (min(a, b) + LUMINANCE_OFFSET)
    }

    private companion object {
        const val TEXT_AA = 4.5
        const val NON_TEXT_AA = 3.0
        const val LUMINANCE_OFFSET = 0.05
    }
}
