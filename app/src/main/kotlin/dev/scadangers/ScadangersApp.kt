package dev.scadangers

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.scadangers.access.AccessUiState
import dev.scadangers.access.CapabilityCards
import dev.scadangers.core.common.device.DeviceProfile
import dev.scadangers.core.designsystem.adaptive.LocalWindowWidthClass
import dev.scadangers.core.designsystem.adaptive.ProvideWindowWidthClass
import dev.scadangers.core.designsystem.adaptive.WindowWidthClass
import dev.scadangers.core.designsystem.component.readableWidth
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersIcons
import dev.scadangers.core.designsystem.theme.ScadangersTheme
import dev.scadangers.core.security.capability.Capability
import dev.scadangers.core.security.capability.OnboardingStep
import dev.scadangers.feature.alerts.AlertsScreen
import dev.scadangers.feature.apps.AppsScreen
import dev.scadangers.feature.dashboard.OverviewScreen
import dev.scadangers.feature.device.DeviceInfoTable
import dev.scadangers.feature.device.DeviceScreen
import dev.scadangers.feature.device.R as DeviceR
import dev.scadangers.feature.nearby.NearbyScreen
import dev.scadangers.feature.network.NetworkProtectionCard
import dev.scadangers.feature.network.NetworkScreen
import dev.scadangers.feature.settings.R as SettingsR
import dev.scadangers.feature.settings.SettingsScreen

private const val TRANSITION_MILLIS = 150

/**
 * Root UI: adaptive navigation (bottom bar on compact windows, rail otherwise), a top app bar
 * with the settings action, and the navigation graph.
 */
@Composable
internal fun ScadangersApp(
    appVersion: String,
    deviceProfile: DeviceProfile = remember { readDeviceProfile() },
    accessState: AccessUiState = AccessUiState(onboardingStep = OnboardingStep.Complete),
    onExplainAccess: (Capability) -> Unit = {},
    onRefreshAccess: () -> Unit = {},
    appsContent: @Composable () -> Unit = { AppsScreen() }
) {
    ProvideWindowWidthClass {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val selected = TopLevelDestination.of(destination)
        val useRail = LocalWindowWidthClass.current != WindowWidthClass.Compact
        val onSelect = { target: TopLevelDestination ->
            navController.navigateToTopLevel(target.route)
        }

        // Single background owner for the window; bars and scaffold stay transparent or surface.
        Row(Modifier.fillMaxSize().background(ScadangersTheme.colors.background)) {
            if (useRail) ScadangersNavigationRail(selected, onSelect)
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentWindowInsets = if (useRail) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
                } else {
                    ScaffoldDefaults.contentWindowInsets
                },
                topBar = {
                    ScadangersTopBar(
                        title = stringResource(titleFor(destination, selected)),
                        showBack = destination != null && !destination.isTopLevel(),
                        showSettings = destination?.hasRoute(Settings::class) != true,
                        windowInsets = if (useRail) {
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.End
                            )
                        } else {
                            TopAppBarDefaults.windowInsets
                        },
                        onBack = { navController.navigateUp() },
                        onSettings = {
                            navController.navigate(Settings) { launchSingleTop = true }
                        }
                    )
                },
                bottomBar = { if (!useRail) ScadangersNavigationBar(selected, onSelect) }
            ) { padding ->
                ScadangersNavHost(
                    navController = navController,
                    appVersion = appVersion,
                    deviceProfile = deviceProfile,
                    accessState = accessState,
                    onExplainAccess = onExplainAccess,
                    onRefreshAccess = onRefreshAccess,
                    appsContent = appsContent,
                    modifier = Modifier.padding(padding).consumeWindowInsets(padding)
                )
            }
        }
    }
}

@Composable
private fun ScadangersNavHost(
    navController: NavHostController,
    appVersion: String,
    deviceProfile: DeviceProfile,
    accessState: AccessUiState,
    onExplainAccess: (Capability) -> Unit,
    onRefreshAccess: () -> Unit,
    appsContent: @Composable () -> Unit,
    modifier: Modifier
) {
    val reducedMotion = ScadangersTheme.reducedMotion
    NavHost(
        navController = navController,
        startDestination = Overview,
        modifier = modifier,
        enterTransition = {
            if (reducedMotion) EnterTransition.None else fadeIn(tween(TRANSITION_MILLIS))
        },
        exitTransition = {
            if (reducedMotion) ExitTransition.None else fadeOut(tween(TRANSITION_MILLIS))
        }
    ) {
        composable<Overview> {
            OverviewScreen(
                networkProtection = { cardModifier -> NetworkProtectionCard(cardModifier) },
                deviceSummary = { DeviceInfoTable(deviceProfile) },
                onOpenDevice = { navController.navigate(Device) { launchSingleTop = true } }
            )
        }
        capabilityDestinations(accessState, onExplainAccess, appsContent)
        composable<Device> { DeviceScreen(deviceProfile) }
        composable<Settings> {
            SettingsScreen(appVersion) {
                Column(Modifier.readableWidth()) {
                    TextButton(onClick = onRefreshAccess) {
                        Text(stringResource(R.string.access_refresh))
                    }
                    CapabilityCards(Capability.entries, accessState, onExplainAccess)
                }
            }
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.capabilityDestinations(
    accessState: AccessUiState,
    onExplainAccess: (Capability) -> Unit,
    appsContent: @Composable () -> Unit
) {
    composable<Apps> { appsContent() }
    composable<Network> {
        NetworkScreen {
            CapabilityCards(
                listOf(Capability.LocalNetwork, Capability.VpnAuthorization),
                accessState,
                onExplainAccess
            )
        }
    }
    composable<Nearby> {
        NearbyScreen {
            CapabilityCards(
                listOf(
                    Capability.BluetoothScanning,
                    Capability.WifiScanning,
                    Capability.NearbyWifi
                ),
                accessState,
                onExplainAccess
            )
        }
    }
    composable<Alerts> {
        AlertsScreen {
            CapabilityCards(listOf(Capability.Notifications), accessState, onExplainAccess)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScadangersTopBar(
    title: String,
    showBack: Boolean,
    showSettings: Boolean,
    windowInsets: WindowInsets,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = ScadangersTheme.colors
    Column {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            ScadangersIcons.Back,
                            contentDescription = stringResource(R.string.action_navigate_up)
                        )
                    }
                }
            },
            actions = {
                if (showSettings) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            ScadangersIcons.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                }
            },
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = colors.textPrimary,
                navigationIconContentColor = colors.textSecondary,
                actionIconContentColor = colors.textSecondary
            )
        )
        HorizontalDivider(thickness = ScadangersDimensions.border, color = colors.border)
    }
}

private fun titleFor(destination: NavDestination?, selected: TopLevelDestination?): Int = when {
    destination?.hasRoute(Settings::class) == true -> SettingsR.string.settings_title
    destination?.hasRoute(Device::class) == true -> DeviceR.string.device_title
    selected != null -> selected.label
    else -> R.string.app_name
}

private fun NavDestination.isTopLevel(): Boolean = TopLevelDestination.entries.any {
    hasRoute(it.route::class)
}

private fun NavHostController.navigateToTopLevel(route: Route) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
