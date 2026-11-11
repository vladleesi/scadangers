package dev.vladleesi.scadangers.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import dev.vladleesi.scadangers.core.designsystem.adaptive.LocalWindowWidthClass
import dev.vladleesi.scadangers.core.designsystem.adaptive.WindowWidthClass
import dev.vladleesi.scadangers.core.designsystem.component.CardContent
import dev.vladleesi.scadangers.core.designsystem.component.EmptyState
import dev.vladleesi.scadangers.core.designsystem.component.FindingRow
import dev.vladleesi.scadangers.core.designsystem.component.FindingUiModel
import dev.vladleesi.scadangers.core.designsystem.component.MetricsCard
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.vladleesi.scadangers.core.designsystem.component.SectionCard
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatusIndicator
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme

/** Test tag of the overview's scrollable content. */
const val OVERVIEW_LIST_TAG = "overview_list"

/** Maximum findings or events previewed on the overview before "view all" navigation. */
private const val MAX_PREVIEW_ROWS = 5

/**
 * Overview destination. Summaries owned by other features are passed in as slots so this
 * module does not depend on them.
 *
 * @param networkProtection the network feature's protection control.
 * @param deviceSummary the device feature's summary table.
 * @param onOpenDevice opens device details.
 */
@Composable
fun OverviewScreen(
    networkProtection: @Composable (Modifier) -> Unit,
    deviceSummary: @Composable () -> Unit,
    onOpenDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverviewContent(
        state = OverviewUiState.NoDeviceAssessment,
        networkProtection = networkProtection,
        deviceSummary = deviceSummary,
        onOpenDevice = onOpenDevice,
        onQuickScan = {},
        onOpenFinding = {},
        modifier = modifier
    )
}

@Composable
internal fun OverviewContent(
    state: OverviewUiState,
    networkProtection: @Composable (Modifier) -> Unit,
    deviceSummary: @Composable () -> Unit,
    onOpenDevice: () -> Unit,
    onQuickScan: () -> Unit,
    onOpenFinding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthClass = LocalWindowWidthClass.current
    val columns = if (widthClass == WindowWidthClass.Compact) 1 else 2
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier.widthIn(
                max = ScadangersDimensions.maxContentWidth
            ).fillMaxSize().testTag(OVERVIEW_LIST_TAG),
            contentPadding = PaddingValues(
                horizontal = widthClass.gutter,
                vertical = ScadangersSpacing.lg
            ),
            horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg),
            verticalItemSpacing = ScadangersSpacing.lg
        ) {
            item(key = "protection", span = StaggeredGridItemSpan.FullLine) {
                ProtectionCard(state, onQuickScan)
            }
            item(key = "metrics", span = StaggeredGridItemSpan.FullLine) { MetricsRow(state) }
            item(key = "action_items") { ActionItemsCard(state, onOpenFinding) }
            item(key = "network") { networkProtection(Modifier.fillMaxWidth()) }
            item(key = "events") { RecentEventsCard(state, onOpenFinding) }
            item(key = "modules") { ModulesCard(state.modules) }
            item(key = "device") {
                SectionCard(
                    title = stringResource(R.string.overview_device_heading),
                    action = {
                        TextButton(onClick = onOpenDevice) {
                            Text(stringResource(R.string.overview_device_details))
                        }
                    }
                ) { deviceSummary() }
            }
        }
    }
}

@Composable
private fun MetricsRow(state: OverviewUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg)) {
        MetricsCard(
            label = stringResource(R.string.overview_metric_action_items),
            value = state.actionItemsMetric,
            supportingText = if (state.assessed) {
                null
            } else {
                stringResource(
                    R.string.overview_metric_not_assessed
                )
            },
            modifier = Modifier.weight(1f)
        )
        MetricsCard(
            label = stringResource(R.string.overview_metric_events),
            value = state.eventsMetric,
            supportingText = if (state.monitoring) {
                null
            } else {
                stringResource(
                    R.string.overview_metric_not_monitored
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionItemsCard(state: OverviewUiState, onOpenFinding: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.overview_action_items_heading)) {
        FindingList(
            findings = state.actionItems,
            emptyTitle = stringResource(R.string.overview_action_items_empty_title),
            emptyBody = stringResource(
                if (state.assessed) {
                    R.string.overview_action_items_none_found
                } else {
                    R.string.overview_action_items_not_assessed
                }
            ),
            onOpenFinding = onOpenFinding
        )
    }
}

@Composable
private fun RecentEventsCard(state: OverviewUiState, onOpenFinding: (String) -> Unit) {
    SectionCard(title = stringResource(R.string.overview_events_heading)) {
        FindingList(
            findings = state.recentEvents,
            emptyTitle = stringResource(R.string.overview_events_empty_title),
            emptyBody = stringResource(
                if (state.monitoring) {
                    R.string.overview_events_none_detected
                } else {
                    R.string.overview_events_not_monitored
                }
            ),
            onOpenFinding = onOpenFinding
        )
    }
}

/** Short, bounded list inside one grid item; full lists belong on a lazy destination. */
@Composable
private fun FindingList(
    findings: List<FindingUiModel>,
    emptyTitle: String,
    emptyBody: String,
    onOpenFinding: (String) -> Unit
) {
    if (findings.isEmpty()) {
        EmptyState(title = emptyTitle, body = emptyBody)
        return
    }
    Column {
        findings.take(MAX_PREVIEW_ROWS).forEachIndexed { index, finding ->
            if (index > 0) HorizontalDivider(color = ScadangersTheme.colors.border)
            FindingRow(finding = finding, onClick = onOpenFinding)
        }
    }
}

@Composable
private fun ModulesCard(modules: List<ModuleUiModel>) {
    SectionCard(title = stringResource(R.string.overview_modules_heading)) {
        modules.forEachIndexed { index, module ->
            if (index > 0) HorizontalDivider(color = ScadangersTheme.colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = ScadangersSpacing.lg, vertical = ScadangersSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)
            ) {
                Text(
                    text = stringResource(module.module.label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                SecurityStatusIndicator(module.status)
            }
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun OverviewPreview() {
    ScadangersPreviewSurface {
        OverviewContent(
            state = OverviewUiState.NoDeviceAssessment,
            networkProtection = { modifier ->
                SectionCard(title = "Network protection (preview slot)", modifier = modifier) {
                    CardContent { Text("Provided by the network feature") }
                }
            },
            deviceSummary = { CardContent { Text("Provided by the device feature") } },
            onOpenDevice = {},
            onQuickScan = {},
            onOpenFinding = {}
        )
    }
}
