package dev.vladleesi.scadangers.core.designsystem

import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatus
import dev.vladleesi.scadangers.core.designsystem.component.Severity
import dev.vladleesi.scadangers.core.designsystem.component.severityColors
import dev.vladleesi.scadangers.core.designsystem.component.statusColors
import dev.vladleesi.scadangers.core.designsystem.theme.DarkScadangersColors
import dev.vladleesi.scadangers.core.designsystem.theme.LightScadangersColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SemanticColorTest {
    private val palettes = listOf(LightScadangersColors, DarkScadangersColors)

    @Test
    fun onlyActiveStatusUsesPositiveRole() = palettes.forEach { colors ->
        SecurityStatus.entries.forEach { status ->
            val positive = colors.statusColors(status) == colors.positive
            assertEquals("$status positive", status == SecurityStatus.Active, positive)
        }
    }

    @Test
    fun unavailableAndInactiveAreNeutral() = palettes.forEach { colors ->
        assertEquals(colors.neutral, colors.statusColors(SecurityStatus.Unavailable))
        assertEquals(colors.neutral, colors.statusColors(SecurityStatus.Inactive))
    }

    @Test
    fun severitiesMapToDistinctRiskRoles() = palettes.forEach { colors ->
        assertEquals(colors.critical, colors.severityColors(Severity.Critical))
        assertEquals(colors.caution, colors.severityColors(Severity.Moderate))
        assertEquals(colors.info, colors.severityColors(Severity.Low))
        Severity.entries.forEach { assertNotEquals(colors.positive, colors.severityColors(it)) }
    }

    @Test
    fun brandAccentIsNeverAStatusColor() = palettes.forEach { colors ->
        val roles =
            listOf(colors.positive, colors.caution, colors.critical, colors.info, colors.neutral)
        roles.forEach { role ->
            assertFalse(role.content == colors.accent || role.container == colors.accentContainer)
        }
    }
}
