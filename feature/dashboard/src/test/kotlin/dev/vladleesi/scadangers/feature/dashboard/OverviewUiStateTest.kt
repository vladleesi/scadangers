package dev.vladleesi.scadangers.feature.dashboard

import dev.vladleesi.scadangers.core.designsystem.component.FindingUiModel
import dev.vladleesi.scadangers.core.designsystem.component.MetricValue
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatus
import dev.vladleesi.scadangers.core.designsystem.component.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewUiStateTest {
    private val state = OverviewUiState.NoDeviceAssessment

    @Test
    fun appReviewAvailabilityNeverReportsContinuousProtection() {
        assertEquals(SecurityStatus.Unavailable, state.protectionStatus)
        assertTrue(
            state.modules.filter {
                it.module != SecurityModule.AppAnalysis && it.module != SecurityModule.DeviceChecks
            }.all {
                it.status ==
                    SecurityStatus.Unavailable
            }
        )
        assertEquals(SecurityModule.entries.toSet(), state.modules.map { it.module }.toSet())
        assertFalse(state.monitoring)
        assertFalse(state.quickScanAvailable)
    }

    @Test
    fun overviewDoesNotInventFullDeviceAssessmentOrFindings() {
        assertNull(state.lastSuccessfulScan)
        assertFalse(state.assessed)
        assertTrue(state.actionItems.isEmpty())
        assertTrue(state.recentEvents.isEmpty())
    }

    @Test
    fun unassessedMetricsAreUnavailableNotZero() {
        assertEquals(MetricValue.Unavailable, state.actionItemsMetric)
        assertEquals(MetricValue.Unavailable, state.eventsMetric)
    }

    @Test
    fun metricsCountOnlyAfterRealAssessmentAndMonitoring() {
        val finding = FindingUiModel("id", "Title", Severity.Low, "Category", "Date")
        val assessed = state.copy(
            lastSuccessfulScan = "Today",
            modules = listOf(
                ModuleUiModel(SecurityModule.NetworkProtection, SecurityStatus.Active)
            ),
            actionItems = listOf(finding)
        )

        assertEquals(MetricValue.Count(1), assessed.actionItemsMetric)
        assertEquals(MetricValue.Count(0), assessed.eventsMetric)
    }

    @Test
    fun inactiveModulesDoNotCountAsMonitoring() {
        val inactive = state.copy(
            modules = listOf(
                ModuleUiModel(SecurityModule.NetworkProtection, SecurityStatus.Inactive)
            )
        )

        assertFalse(inactive.monitoring)
        assertEquals(MetricValue.Unavailable, inactive.eventsMetric)
    }
}
