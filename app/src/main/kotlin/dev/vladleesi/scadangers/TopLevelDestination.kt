package dev.vladleesi.scadangers

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons
import dev.vladleesi.scadangers.feature.alerts.R as AlertsR
import dev.vladleesi.scadangers.feature.apps.R as AppsR
import dev.vladleesi.scadangers.feature.dashboard.R as DashboardR
import dev.vladleesi.scadangers.feature.nearby.R as NearbyR
import dev.vladleesi.scadangers.feature.network.R as NetworkR
import kotlin.reflect.KClass

/**
 * The five primary destinations shown in the navigation bar or rail.
 *
 * @property children secondary routes that keep this destination selected.
 */
internal enum class TopLevelDestination(
    val route: Route,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    private val children: List<KClass<out Route>> = emptyList()
) {
    OverviewDestination(
        Overview,
        DashboardR.string.overview_title,
        ScadangersIcons.Overview,
        listOf(Device::class)
    ),
    AppsDestination(Apps, AppsR.string.apps_title, ScadangersIcons.Apps),
    NetworkDestination(Network, NetworkR.string.network_title, ScadangersIcons.Network),
    NearbyDestination(Nearby, NearbyR.string.nearby_title, ScadangersIcons.Nearby),
    AlertsDestination(Alerts, AlertsR.string.alerts_title, ScadangersIcons.Alerts)
    ;

    fun isSelected(destination: NavDestination?): Boolean = destination != null &&
        (destination.hasRoute(route::class) || children.any { destination.hasRoute(it) })

    companion object {
        /** The top-level destination that owns [destination], or `null` (for example Settings). */
        fun of(destination: NavDestination?): TopLevelDestination? =
            entries.firstOrNull { it.isSelected(destination) }
    }
}
