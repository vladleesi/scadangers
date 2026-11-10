@file:Suppress("MagicNumber") // Token values are the named constants.

package dev.scadangers.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconography: one filled 24 dp set from Material Icons (Apache License 2.0), declared in code
 * so icons need no resource parsing and are built only on first use. Icons inherit their tint
 * from the caller; status icons always appear with a text label.
 */
object ScadangersIcons {
    /** Icon sizes. */
    object Size {
        val small = 16.dp
        val medium = 20.dp
        val large = 24.dp
        val state = 32.dp
    }

    val Overview: ImageVector by lazy {
        icon(
            "overview",
            "M3,13h8V3H3v10zM3,21h8v-6H3v6zM13,21h8V11h-8v10zM13,3v6h8V3h-8z"
        )
    }

    val Apps: ImageVector by lazy {
        icon(
            "apps",
            "M4,8h4V4H4v4zM10,20h4v-4h-4v4zM4,20h4v-4H4v4zM4,14h4v-4H4v4zM10,14h4v-4" +
                "h-4v4zM16,4v4h4V4h-4zM10,8h4V4h-4v4zM16,14h4v-4h-4v4zM16,20h4v-4h-4v4z"
        )
    }

    val Network: ImageVector by lazy {
        icon(
            "network",
            "M1,9l2,2c4.97,-4.97 13.03,-4.97 18,0l2,-2C16.93,2.93 7.08,2.93 1,9zM9,17" +
                "l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0zM5,13l2,2" +
                "c2.76,-2.76 7.24,-2.76 10,0l2,-2C15.14,9.14 8.87,9.14 5,13z"
        )
    }

    val Nearby: ImageVector by lazy {
        icon(
            "nearby",
            "M12,11c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM18,13" +
                "c0,-3.31 -2.69,-6 -6,-6s-6,2.69 -6,6c0,2.22 1.21,4.15 3,5.19l1,-1.74" +
                "c-1.19,-0.7 -2,-1.97 -2,-3.45 0,-2.21 1.79,-4 4,-4s4,1.79 4,4" +
                "c0,1.48 -0.81,2.75 -2,3.45l1,1.74c1.79,-1.04 3,-2.97 3,-5.19zM12,3" +
                "C6.48,3 2,7.48 2,13c0,3.7 2.01,6.92 4.99,8.65l1,-1.73" +
                "C5.61,18.53 4,15.96 4,13c0,-4.42 3.58,-8 8,-8s8,3.58 8,8" +
                "c0,2.96 -1.61,5.53 -4,6.92l1,1.73" +
                "c2.99,-1.73 5,-4.95 5,-8.65 0,-5.52 -4.48,-10 -10,-10z"
        )
    }

    val Alerts: ImageVector by lazy {
        icon(
            "alerts",
            "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5" +
                "c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5" +
                "s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"
        )
    }

    val Settings: ImageVector by lazy {
        icon(
            "settings",
            "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94" +
                "c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58" +
                "c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32" +
                "c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96" +
                "c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81" +
                "c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41" +
                "L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33" +
                "c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48" +
                "l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58" +
                "c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32" +
                "c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94" +
                "l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84" +
                "c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54" +
                "c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22" +
                "l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6" +
                "c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6" +
                "S13.98,15.6 12,15.6z"
        )
    }

    val Back: ImageVector by lazy {
        icon(
            "back",
            "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
            "chevron_right",
            "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z"
        )
    }

    val Shield: ImageVector by lazy {
        icon(
            "shield",
            "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4z"
        )
    }

    val Device: ImageVector by lazy {
        icon(
            "device",
            "M17,1.01L7,1c-1.1,0 -1.99,0.9 -1.99,2v18c0,1.1 0.89,2 1.99,2h10" +
                "c1.1,0 2,-0.9 2,-2V3c0,-1.1 -0.9,-1.99 -2,-1.99zM17,19H7V5h10v14z"
        )
    }

    val Search: ImageVector by lazy {
        icon(
            "search",
            "M15.5,14h-0.79l-0.28,-0.27" +
                "C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3" +
                "S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79" +
                "l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5" +
                "S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"
        )
    }

    val CheckCircle: ImageVector by lazy {
        icon(
            "check_circle",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" +
                "M10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z"
        )
    }

    val Warning: ImageVector by lazy {
        icon(
            "warning",
            "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z"
        )
    }

    val Error: ImageVector by lazy {
        icon(
            "error",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" +
                "M13,17h-2v-2h2v2zM13,13h-2V7h2v6z"
        )
    }

    val Info: ImageVector by lazy {
        icon(
            "info",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" +
                "M13,17h-2v-6h2v6zM13,9h-2V7h2v2z"
        )
    }

    val Unavailable: ImageVector by lazy {
        icon(
            "unavailable",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" +
                "M17,13H7v-2h10v2z"
        )
    }

    val Off: ImageVector by lazy {
        icon(
            "off",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" +
                "M4,12c0,-4.42 3.58,-8 8,-8c1.85,0 3.55,0.63 4.9,1.69L5.69,16.9" +
                "C4.63,15.55 4,13.85 4,12zM12,20c-1.85,0 -3.55,-0.63 -4.9,-1.69L18.31,7.1" +
                "C19.37,8.45 20,10.15 20,12c0,4.42 -3.58,8 -8,8z"
        )
    }

    val Lock: ImageVector by lazy {
        icon(
            "lock",
            "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10" +
                "c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17" +
                "c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6" +
                "c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z"
        )
    }

    val Inbox: ImageVector by lazy {
        icon(
            "inbox",
            "M19,3H4.99c-1.11,0 -1.98,0.89 -1.98,2L3,19c0,1.1 0.88,2 1.99,2H19" +
                "c1.1,0 2,-0.9 2,-2V5c0,-1.11 -0.9,-2 -2,-2zM19,15h-4c0,1.66 -1.35,3 -3,3" +
                "s-3,-1.34 -3,-3H4.99V5H19v10z"
        )
    }

    private fun icon(name: String, pathData: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()
}
