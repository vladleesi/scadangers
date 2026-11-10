package dev.scadangers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import dev.scadangers.core.designsystem.theme.ScadangersTheme
import dev.scadangers.feature.apps.AppRowUi
import dev.scadangers.feature.apps.AppsScreen
import dev.scadangers.feature.apps.AppsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InstalledAppsUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun hundredsOfAppsRemainScrollableAtLargeTextScale() {
        val rows = (1..350).map {
            AppRowUi("synthetic.$it", "Synthetic app $it", "Test fixture")
        }
        var opened: String? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                ScadangersTheme {
                    AppsScreen(AppsUiState(loaded = true, apps = rows), onOpen = { opened = it })
                }
            }
        }
        compose.onNodeWithTag("installed-apps").performScrollToNode(hasText("Synthetic app 350"))
        compose.onNodeWithText("Synthetic app 350").performClick()
        assertEquals("synthetic.350", opened)
    }

    @Test
    fun searchFiltersAndScanCancellationEmitUserEvents() {
        var full: Boolean? = null
        var cancelled = false
        compose.setContent {
            var state by remember { mutableStateOf(AppsUiState(loaded = true)) }
            ScadangersTheme {
                AppsScreen(
                    state,
                    onScan = {
                        full = it
                        state = state.copy(busy = true)
                    },
                    onCancel = {
                        cancelled = true
                        state = state.copy(busy = false)
                    },
                    onFilters = { state = state.copy(filters = it) }
                )
            }
        }
        compose.onNodeWithText("Full app scan").performClick()
        assertEquals(true, full)
        compose.onNodeWithText("Cancel scan").performClick()
        assertEquals(true, cancelled)
        compose.onNodeWithTag(
            "installed-apps"
        ).performScrollToNode(hasText("Search name or package"))
        compose.onNodeWithText("Search name or package").performTextInput("synthetic")
        compose.onNodeWithText("synthetic").assertIsDisplayed()
        compose.onNodeWithTag("installed-apps").performScrollToNode(hasText("App type: All apps"))
        compose.onNodeWithText("App type: All apps").performClick()
        compose.onNodeWithText("System apps").performClick()
        compose.onNodeWithText("App type: System apps").assertIsDisplayed()
    }
}
