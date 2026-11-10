package dev.scadangers.core.designsystem

import androidx.compose.ui.unit.dp
import dev.scadangers.core.designsystem.adaptive.WindowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowWidthClassTest {
    @Test
    fun classifiesMaterialBreakpoints() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(0.dp))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(599.9.dp))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(600.dp))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(839.9.dp))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(840.dp))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(2000.dp))
    }

    @Test
    fun wideWindowsUseWiderGutters() {
        assertEquals(16.dp, WindowWidthClass.Compact.gutter)
        assertEquals(24.dp, WindowWidthClass.Medium.gutter)
        assertEquals(24.dp, WindowWidthClass.Expanded.gutter)
    }
}
