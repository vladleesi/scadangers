@file:Suppress("MagicNumber") // Breakpoints are the named constants.

package dev.vladleesi.scadangers.core.designsystem.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing

/**
 * Material window width size classes. Layouts adapt to the window's available width, never
 * to device type, so phones in landscape, split screen, tablets and unfolded foldables all
 * take the layout their space supports.
 */
enum class WindowWidthClass {
    /** Under 600 dp: phones in portrait, narrow split-screen panes, folded foldables. */
    Compact,

    /** 600–839 dp: large phones in landscape, small tablets, unfolded foldables in portrait. */
    Medium,

    /** 840 dp and wider: tablets and unfolded foldables in landscape, desktop windows. */
    Expanded
    ;

    /** Horizontal screen margin for this width. */
    val gutter: Dp
        get() = if (this ==
            Compact
        ) {
            ScadangersSpacing.gutterCompact
        } else {
            ScadangersSpacing.gutterWide
        }

    companion object {
        private val MediumMinWidth = 600.dp
        private val ExpandedMinWidth = 840.dp

        /** Classifies the window's available width. */
        fun fromWidth(windowWidth: Dp): WindowWidthClass = when {
            windowWidth < MediumMinWidth -> Compact
            windowWidth < ExpandedMinWidth -> Medium
            else -> Expanded
        }
    }
}

/** The current window width class, provided once by the app's root layout. */
val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.Compact }

/**
 * Measures the space given to [content] and provides its [WindowWidthClass] through
 * [LocalWindowWidthClass]. Place it once at the root of a window that fills the screen.
 */
@Composable
fun ProvideWindowWidthClass(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(
            LocalWindowWidthClass provides WindowWidthClass.fromWidth(maxWidth)
        ) {
            content()
        }
    }
}
