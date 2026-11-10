package dev.scadangers.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import dev.scadangers.core.designsystem.component.FindingUiModel
import dev.scadangers.core.designsystem.component.MetricValue
import dev.scadangers.core.designsystem.component.SecurityStatus

/** Security modules summarized on the overview. */
internal enum class SecurityModule(@param:StringRes val label: Int) {
    AppAnalysis(R.string.overview_module_app_analysis),
    NetworkProtection(R.string.overview_module_network_protection),
    NearbyScanning(R.string.overview_module_nearby),
    DeviceChecks(R.string.overview_module_device_checks)
}

@Immutable
internal data class ModuleUiModel(val module: SecurityModule, val status: SecurityStatus)

/**
 * Everything the overview renders.
 *
 * @property lastSuccessfulScan formatted completion time, or `null` if no scan has succeeded.
 * @property actionItems open findings from the last successful scan, most urgent first.
 * @property recentEvents events reported by active monitoring modules, newest first.
 */
@Immutable
internal data class OverviewUiState(
    val protectionStatus: SecurityStatus,
    val modules: List<ModuleUiModel>,
    val lastSuccessfulScan: String?,
    val quickScanAvailable: Boolean,
    val actionItems: List<FindingUiModel>,
    val recentEvents: List<FindingUiModel>
) {
    /** True only when a scan has completed, so an empty action list is meaningful. */
    val assessed: Boolean get() = lastSuccessfulScan != null

    /** True only when at least one module is verified running. */
    val monitoring: Boolean get() = modules.any { it.status == SecurityStatus.Active }

    /** Open action items, or [MetricValue.Unavailable] when nothing has been assessed. */
    val actionItemsMetric: MetricValue
        get() = if (assessed) {
            MetricValue.Count(
                actionItems.size.toLong()
            )
        } else {
            MetricValue.Unavailable
        }

    /** Detected events, or [MetricValue.Unavailable] when nothing is monitored. */
    val eventsMetric: MetricValue
        get() = if (monitoring) {
            MetricValue.Count(
                recentEvents.size.toLong()
            )
        } else {
            MetricValue.Unavailable
        }

    companion object {
        /**
         * No full-device assessment or continuous monitoring is implemented.
         * Installed-app review is available separately in Apps.
         */
        val NoDeviceAssessment = OverviewUiState(
            protectionStatus = SecurityStatus.Unavailable,
            modules = SecurityModule.entries.map {
                ModuleUiModel(
                    it,
                    if (it ==
                        SecurityModule.AppAnalysis
                    ) {
                        SecurityStatus.Inactive
                    } else {
                        SecurityStatus.Unavailable
                    }
                )
            },
            lastSuccessfulScan = null,
            quickScanAvailable = false,
            actionItems = emptyList(),
            recentEvents = emptyList()
        )
    }
}
