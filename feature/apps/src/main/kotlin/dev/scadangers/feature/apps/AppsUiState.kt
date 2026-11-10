package dev.scadangers.feature.apps

import dev.scadangers.core.security.apps.AppFinding

enum class AppSort { Name, Risk, Installed, Updated }
enum class AppTypeFilter { All, User, System }
enum class AppRiskFilter { All, Review }
enum class AppSourceFilter { All, ExpectedStore, Other, Unknown }

data class AppFilters(
    val query: String = "",
    val sort: AppSort = AppSort.Name,
    val type: AppTypeFilter = AppTypeFilter.All,
    val risk: AppRiskFilter = AppRiskFilter.All,
    val source: AppSourceFilter = AppSourceFilter.All
)

data class AppRowUi(val packageName: String, val label: String, val summary: String)
data class AppDetailLine(val label: String, val description: String)
data class AppDetailsUi(
    val packageName: String,
    val label: String,
    val metadata: List<AppDetailLine>,
    val findings: List<AppFinding>,
    val permissions: List<AppDetailLine>,
    val apks: List<String>
)

data class AppsUiState(
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val progress: String = "",
    val lastScan: String? = null,
    val summary: String = "",
    val error: String? = null,
    val filters: AppFilters = AppFilters(),
    val apps: List<AppRowUi> = emptyList(),
    val details: AppDetailsUi? = null,
    val inaccessible: List<AppDetailLine> = emptyList()
)

/** Only documented settings screens; actions never grant access or change another app directly. */
enum class AppSettingsAction {
    Details,
    Overlay,
    InstallSources,
    WriteSettings,
    Usage,
    Accessibility,
    Notifications,
    Administrators,
    AllFiles
}
