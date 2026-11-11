@file:Suppress("MagicNumber") // Token values are the named constants.

package dev.vladleesi.scadangers.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4 dp spacing scale. Use these instead of inline padding values. */
object ScadangersSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Horizontal screen margin in compact windows. */
    val gutterCompact = 16.dp

    /** Horizontal screen margin in medium and expanded windows. */
    val gutterWide = 24.dp
}

/** Subtle corner radii; cards and controls stay close to rectangular. */
object ScadangersShapes {
    val small = RoundedCornerShape(4.dp)
    val medium = RoundedCornerShape(6.dp)
    val large = RoundedCornerShape(8.dp)
    val extraLarge = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(percent = 50)

    internal val material = Shapes(
        extraSmall = small,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge
    )
}

/**
 * Elevation. Content separates through 1 dp borders, not shadows; only transient overlays
 * (menus, dialogs) are raised.
 */
object ScadangersElevation {
    val flat = 0.dp
    val overlay = 6.dp
}

/** Component dimensions shared across the system. */
object ScadangersDimensions {
    /** Minimum touch target for any interactive element (Android accessibility guidance). */
    val minTouchTarget = 48.dp
    val border = 1.dp
    val severityBar = 3.dp
    val statusDot = 8.dp
    val listRowMinHeight = 56.dp
    val progressTrack = 4.dp

    /** Content stays readable on tablets and unfolded foldables instead of stretching. */
    val maxContentWidth = 1200.dp
    val maxReadableWidth = 640.dp

    /** Minimum card width for adaptive multi-column layouts. */
    val cardMinWidth = 340.dp
}
