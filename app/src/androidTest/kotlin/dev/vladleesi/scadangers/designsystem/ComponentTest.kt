package dev.vladleesi.scadangers.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.vladleesi.scadangers.core.designsystem.component.CompactInfoTable
import dev.vladleesi.scadangers.core.designsystem.component.ConfirmationDialog
import dev.vladleesi.scadangers.core.designsystem.component.EmptyState
import dev.vladleesi.scadangers.core.designsystem.component.ErrorState
import dev.vladleesi.scadangers.core.designsystem.component.FindingRow
import dev.vladleesi.scadangers.core.designsystem.component.FindingUiModel
import dev.vladleesi.scadangers.core.designsystem.component.InfoRow
import dev.vladleesi.scadangers.core.designsystem.component.LoadingState
import dev.vladleesi.scadangers.core.designsystem.component.MetricValue
import dev.vladleesi.scadangers.core.designsystem.component.MetricsCard
import dev.vladleesi.scadangers.core.designsystem.component.PermissionRequiredState
import dev.vladleesi.scadangers.core.designsystem.component.ScanProgress
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatus
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatusIndicator
import dev.vladleesi.scadangers.core.designsystem.component.SettingsToggleItem
import dev.vladleesi.scadangers.core.designsystem.component.Severity
import dev.vladleesi.scadangers.core.designsystem.component.SeverityLabel
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import java.text.NumberFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Semantics and interaction contracts of design-system components, using synthetic data. */
class ComponentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(reducedMotion: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { ScadangersTheme(reducedMotion = reducedMotion, content = content) }
    }

    @Test
    fun statusAndSeverityAreAnnouncedInWords() {
        show {
            SecurityStatusIndicator(SecurityStatus.NeedsAttention)
            SeverityLabel(Severity.Critical)
        }
        compose.onNodeWithContentDescription("Status: Needs attention").assertExists()
        compose.onNodeWithContentDescription("Severity: Critical").assertExists()
    }

    @Test
    fun unavailableMetricIsAnnouncedAndNeverZero() {
        show { MetricsCard(label = "Example metric", value = MetricValue.Unavailable) }
        compose.onNodeWithContentDescription("Example metric, Not available").assertExists()
    }

    @Test
    fun countMetricUsesLocaleFormatting() {
        show { MetricsCard(label = "Example count", value = MetricValue.Count(1234)) }
        val expected = NumberFormat.getIntegerInstance().format(1234L)
        compose.onNodeWithContentDescription("Example count, $expected").assertExists()
    }

    @Test
    fun findingRowReportsItsIdAndMeetsTouchTarget() {
        var opened: String? = null
        val finding =
            FindingUiModel(
                "synthetic-1",
                "Synthetic finding",
                Severity.Moderate,
                "Example",
                "Example date"
            )
        show { FindingRow(finding = finding, onClick = { opened = it }) }

        compose.onNodeWithText("Synthetic finding", substring = true)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals("synthetic-1", opened)
    }

    @Test
    fun settingsToggleRowTogglesAsOneSwitch() {
        show {
            var checked by remember { mutableStateOf(false) }
            SettingsToggleItem(title = "Example toggle", checked = checked, onCheckedChange = {
                checked =
                    it
            })
        }
        compose.onNodeWithText(
            "Example toggle"
        ).assertIsOff().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Example toggle").assertIsOn()
    }

    @Test
    fun disabledSettingsToggleIgnoresInput() {
        var changed = false
        show {
            SettingsToggleItem(title = "Unavailable toggle", checked = false, onCheckedChange = {
                changed =
                    true
            }, enabled = false)
        }
        compose.onNodeWithText(
            "Unavailable toggle"
        ).assertIsNotEnabled().assertIsOff().performClick()
        assertFalse(changed)
    }

    @Test
    fun confirmationDialogReportsChoice() {
        var result: Boolean? = null
        show {
            ConfirmationDialog(
                title = "Remove example?",
                message = "Synthetic message",
                confirmLabel = "Remove",
                dismissLabel = "Cancel",
                onConfirm = { result = true },
                onDismiss = { result = false },
                destructive = true
            )
        }
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(false, result)
        compose.onNodeWithText("Remove").performClick()
        assertEquals(true, result)
    }

    @Test
    fun determinateScanProgressIsClampedAndExposed() {
        show { ScanProgress(title = "Example scan", progress = 1.7f, detail = "Example step") }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f))).assertExists()
        compose.onNodeWithText("100%").assertExists()
    }

    @Test
    fun reducedMotionIndeterminateProgressIsStaticButStillAnnounced() {
        show(reducedMotion = true) {
            ScanProgress(title = "Example scan", progress = null, detail = "Preparing")
            LoadingState(label = "Loading example")
        }
        val indeterminate = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        compose.onNode(indeterminate and hasStateDescription("In progress")).assertExists()
        compose.onNode(indeterminate and hasText("Loading example")).assertExists()
    }

    @Test
    fun statePanelsExposeHeadingsAndActions() {
        var retried = false
        var granted = false
        show {
            Column {
                EmptyState(title = "Nothing here", body = "Synthetic body")
                ErrorState(
                    title = "Couldn't load",
                    body = "Synthetic body",
                    onRetry = { retried = true }
                )
                PermissionRequiredState(
                    title = "Access needed",
                    rationale = "Synthetic",
                    grantLabel = "Allow",
                    onGrant = { granted = true }
                )
            }
        }
        compose.onNode(hasText("Nothing here") and isHeading()).assertExists()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Allow").performClick()
        assertTrue(retried && granted)
    }

    @Test
    fun infoTableRowsReadAsLabelValuePairs() {
        show { CompactInfoTable(listOf(InfoRow("model", "Model", "Synthetic model"))) }
        compose.onNode(hasText("Model") and hasText("Synthetic model")).assertExists()
    }

    private fun hasStateDescription(description: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)
}
