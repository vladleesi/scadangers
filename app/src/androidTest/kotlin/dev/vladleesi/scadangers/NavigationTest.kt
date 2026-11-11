package dev.vladleesi.scadangers

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.feature.dashboard.OVERVIEW_LIST_TAG
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setApp(size: DpSize? = null) {
        compose.setContent {
            if (size == null) {
                App()
            } else {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) { App() }
            }
        }
    }

    @Composable
    private fun App() = ScadangersTheme { ScadangersApp(appVersion = "test") }

    private fun navItem(label: String) = compose.onNode(
        hasText(label) and
            hasAnyAncestor(hasTestTag(NAVIGATION_BAR_TAG) or hasTestTag(NAVIGATION_RAIL_TAG))
    )

    @Test
    fun everyPrimaryDestinationIsReachable() {
        setApp()
        val destinations = listOf(
            "Apps" to "Installed app inventory",
            "Network" to "Network monitoring isn't available",
            "Nearby" to "Nearby scanning isn't available",
            "Alerts" to "No alerts to show",
            "Overview" to "Protection is not active"
        )
        destinations.forEach { (label, content) ->
            navItem(label).performClick()
            navItem(label).assertIsSelected()
            compose.onNodeWithText(content).assertIsDisplayed()
        }
    }

    @Test
    fun settingsOpensFromTopBarActionAndNavigatesBack() {
        setApp()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Data collection").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").assertDoesNotExist()

        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.onNodeWithText("Protection is not active").assertIsDisplayed()
    }

    @Test
    fun deviceDetailsKeepOverviewSelected() {
        setApp()
        compose.onNodeWithTag(OVERVIEW_LIST_TAG).performScrollToNode(hasText("Details"))
        compose.onNodeWithText("Details").performClick()
        compose.onNodeWithText("Reported by the system").assertIsDisplayed()
        navItem("Overview").assertIsSelected()
    }

    @Test
    fun destinationSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { App() }
        navItem("Network").performClick()
        compose.onNodeWithText("Network monitoring isn't available").assertExists()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Network monitoring isn't available").assertExists()
    }

    @Test
    fun compactWindowUsesBottomNavigationBar() {
        setApp(DpSize(400.dp, 800.dp))
        compose.onNodeWithTag(NAVIGATION_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(NAVIGATION_RAIL_TAG).assertDoesNotExist()
    }

    @Test
    fun wideWindowUsesNavigationRail() {
        setApp(DpSize(1000.dp, 700.dp))
        compose.onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
        compose.onNodeWithTag(NAVIGATION_BAR_TAG).assertDoesNotExist()
        navItem("Alerts").performClick()
        compose.onNodeWithText("No alerts to show").assertIsDisplayed()
    }
}
