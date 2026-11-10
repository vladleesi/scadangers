package dev.scadangers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersElevation
import dev.scadangers.core.designsystem.theme.ScadangersTheme

internal const val NAVIGATION_BAR_TAG = "navigation_bar"
internal const val NAVIGATION_RAIL_TAG = "navigation_rail"

/** Bottom navigation for compact windows. */
@Composable
internal fun ScadangersNavigationBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit
) {
    val colors = ScadangersTheme.colors
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.onAccentContainer,
        selectedTextColor = colors.textPrimary,
        indicatorColor = colors.accentContainer,
        unselectedIconColor = colors.textSecondary,
        unselectedTextColor = colors.textSecondary
    )
    Column {
        HorizontalDivider(thickness = ScadangersDimensions.border, color = colors.border)
        NavigationBar(
            modifier = Modifier.testTag(NAVIGATION_BAR_TAG),
            containerColor = colors.surface,
            tonalElevation = ScadangersElevation.flat
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(destination.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = itemColors
                )
            }
        }
    }
}

/** Side navigation for medium and expanded windows. */
@Composable
internal fun ScadangersNavigationRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit
) {
    val colors = ScadangersTheme.colors
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onAccentContainer,
        selectedTextColor = colors.textPrimary,
        indicatorColor = colors.accentContainer,
        unselectedIconColor = colors.textSecondary,
        unselectedTextColor = colors.textSecondary
    )
    Row {
        NavigationRail(
            modifier = Modifier.fillMaxHeight().testTag(NAVIGATION_RAIL_TAG),
            containerColor = colors.surface,
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(destination.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = itemColors
                )
            }
        }
        VerticalDivider(thickness = ScadangersDimensions.border, color = colors.border)
    }
}
