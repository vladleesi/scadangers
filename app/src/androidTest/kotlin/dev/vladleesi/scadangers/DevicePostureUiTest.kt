package dev.vladleesi.scadangers

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladleesi.scadangers.core.common.device.DeviceProfile
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.core.security.device.DevicePostureEvaluator
import dev.vladleesi.scadangers.core.security.device.DeviceSettings
import dev.vladleesi.scadangers.device.AndroidDeviceObservationSource
import dev.vladleesi.scadangers.device.presentation
import dev.vladleesi.scadangers.feature.device.DEVICE_LIST_TAG
import dev.vladleesi.scadangers.feature.device.DeviceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DevicePostureUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun findingsLimitationsAndSystemSettingsRemainReachableAtLargeTextScale() {
        val state = runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val snapshot = AndroidDeviceObservationSource(context).read()
            withContext(Dispatchers.Default) {
                DevicePostureEvaluator().evaluate(snapshot).presentation()
            }
        }
        var selected: DeviceSettings? = null
        val profile = requireNotNull(state.assessment).observations.profile
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                ScadangersTheme {
                    DeviceScreen(
                        profile,
                        state = state,
                        onSettings = { selected = it }
                    )
                }
            }
        }
        scrollTo("Scope and limitations")
        scrollTo("Platform security findings and device indicators")
        scrollTo("Open Updates settings")
        compose.onAllNodesWithText("Open Updates settings")[0].performClick()
        compose.runOnIdle { assertEquals(DeviceSettings.Updates, selected) }
        val lastTitle = state.findings.last().finding.title
        val lastHeading = hasText(lastTitle) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        compose.onNodeWithTag(DEVICE_LIST_TAG).performScrollToNode(lastHeading)
        compose.onNode(lastHeading).assertIsDisplayed()
    }

    @Test
    fun unassessedScreenOffersAssessmentWithoutInventingFindings() {
        var requested = false
        val syntheticProfile = DeviceProfile.fromReportedValues(
            "Example",
            "Synthetic",
            "17",
            37,
            null
        )
        compose.setContent {
            ScadangersTheme {
                DeviceScreen(syntheticProfile, onAssess = { requested = true })
            }
        }
        scrollTo("Assess device security")
        compose.onNodeWithText("Assess device security").performClick()
        compose.runOnIdle { assertEquals(true, requested) }
        compose.onNodeWithText("0 observed risks", substring = true).assertDoesNotExist()
    }

    private fun scrollTo(label: String) {
        compose.onNodeWithTag(DEVICE_LIST_TAG).performScrollToNode(hasText(label))
    }
}
