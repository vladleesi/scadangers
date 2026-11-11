package dev.vladleesi.scadangers

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.feature.dashboard.OVERVIEW_LIST_TAG
import org.junit.Rule
import org.junit.Test

/** The overview must never claim protection, findings or results that do not exist. */
class OverviewTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setOverview(fontScale: Float = 1f) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                ScadangersTheme { ScadangersApp(appVersion = "test") }
            }
        }
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(OVERVIEW_LIST_TAG).performScrollToNode(hasText(text))
    }

    @Test
    fun protectionIsNotClaimed() {
        setOverview()
        compose.onNodeWithText("Protection is not active").assertIsDisplayed()
        compose.onNodeWithContentDescription("Status: Not active").assertIsDisplayed()
        compose.onAllNodesWithText("Protection is active").assertCountEquals(0)
        compose.onAllNodesWithText(
            "Protected",
            substring = true,
            ignoreCase = false
        ).assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Status: Active").assertCountEquals(0)
    }

    @Test
    fun scanAndNetworkProtectionAreUnavailable() {
        setOverview()
        compose.onNodeWithText("Never").assertIsDisplayed()
        compose.onNodeWithText("Quick scan").assertIsNotEnabled()
        scrollTo("Protect network traffic")
        compose.onNodeWithText("Protect network traffic").assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun metricsAreUnavailableRatherThanZero() {
        setOverview()
        compose.onNode(
            hasContentDescription("Open action items, Not available", substring = true)
        ).assertExists()
        compose.onNode(
            hasContentDescription("Detected events, Not available", substring = true)
        ).assertExists()
        compose.onAllNodesWithText("0").assertCountEquals(0)
    }

    @Test
    fun emptySectionsExplainThatNothingWasAssessed() {
        setOverview()
        scrollTo(
            "No full-device assessment has run. Review installed apps in Apps; this does not establish device safety."
        )
        scrollTo("Event detection isn't available in this version, so nothing has been monitored.")
        scrollTo("Nearby device scanning")
        compose.onAllNodesWithContentDescription(
            "Status: Not available"
        ).fetchSemanticsNodes().let {
            check(it.size >= SECURITY_MODULE_COUNT) {
                "Expected unimplemented modules to be unavailable"
            }
        }
    }

    @Test
    fun largestFontScaleKeepsEverySectionReachable() {
        setOverview(fontScale = 2f)
        compose.onNodeWithText("Protection is not active").assertIsDisplayed()
        scrollTo("Details")
        compose.onNodeWithText("Details").assertIsDisplayed()
    }

    private companion object {
        const val SECURITY_MODULE_COUNT = 2
    }
}
