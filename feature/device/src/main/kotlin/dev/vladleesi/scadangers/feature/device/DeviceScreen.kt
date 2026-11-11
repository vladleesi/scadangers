package dev.vladleesi.scadangers.feature.device

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.common.device.DeviceProfile
import dev.vladleesi.scadangers.core.designsystem.component.CardContent
import dev.vladleesi.scadangers.core.designsystem.component.CompactInfoTable
import dev.vladleesi.scadangers.core.designsystem.component.InfoRow
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.vladleesi.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.vladleesi.scadangers.core.designsystem.component.SectionCard
import dev.vladleesi.scadangers.core.designsystem.component.readableWidth
import dev.vladleesi.scadangers.core.security.device.DeviceSettings

const val DEVICE_LIST_TAG = "device_assessment_list"

/** Observable device posture and its limitations; all Android work belongs to the app adapter. */
@Composable
fun DeviceScreen(
    profile: DeviceProfile,
    modifier: Modifier = Modifier,
    state: DeviceUiState = DeviceUiState(),
    onAssess: () -> Unit = {},
    onCancel: () -> Unit = {},
    onSettings: (DeviceSettings) -> Unit = {}
) {
    ScreenLazyColumn(modifier.testTag(DEVICE_LIST_TAG)) {
        item(key = "reported", contentType = "facts") {
            SectionCard(
                stringResource(R.string.device_reported_heading),
                Modifier.readableWidth()
            ) {
                DeviceInfoTable(state.assessment?.observations?.profile ?: profile, detailed = true)
            }
        }
        item(key = "overview", contentType = "overview") {
            SectionCard(
                stringResource(R.string.device_assessment_heading),
                Modifier.readableWidth()
            ) {
                CardContent {
                    Text(state.summary)
                    state.error?.let { Text(it) }
                    if (state.busy) {
                        Text(stringResource(R.string.device_assessing))
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.device_cancel))
                        }
                    } else {
                        TextButton(onClick = onAssess) {
                            Text(stringResource(R.string.device_assess))
                        }
                    }
                }
            }
        }
        item(key = "limitations", contentType = "limitations") {
            SectionCard(stringResource(R.string.device_limitations), Modifier.readableWidth()) {
                CardContent {
                    Text(state.assessment?.limitations ?: stringResource(R.string.device_scope))
                    state.assessment?.let {
                        Text(
                            stringResource(
                                R.string.device_catalog,
                                it.catalogVersion,
                                it.catalogReviewedOn
                            )
                        )
                    }
                }
            }
        }
        item(key = "findings_heading", contentType = "heading") {
            Text(stringResource(R.string.device_findings), Modifier.readableWidth())
        }
        items(state.findings, key = { it.finding.id }, contentType = { "device_finding" }) { row ->
            DeviceFindingCard(row, onSettings)
        }
    }
}

@Composable
private fun DeviceFindingCard(row: DeviceFindingUi, onSettings: (DeviceSettings) -> Unit) {
    SectionCard(row.finding.title, Modifier.readableWidth()) {
        CompactInfoTable(row.details)
        CardContent {
            Text(stringResource(R.string.device_evidence, row.finding.evidence))
            Text(stringResource(R.string.device_reference, row.finding.officialReference))
            Text(stringResource(R.string.device_remediation, row.finding.remediation))
            row.finding.settings?.let { action ->
                TextButton(onClick = { onSettings(action) }) {
                    Text(stringResource(R.string.device_open_settings, action.name))
                }
            }
        }
    }
}

/**
 * System-reported device facts as a compact table. The summary omits the manufacturer row.
 * Values are shown as reported; this is not an assessment.
 */
@Composable
fun DeviceInfoTable(
    profile: DeviceProfile,
    modifier: Modifier = Modifier,
    detailed: Boolean = false
) {
    val notReported = stringResource(R.string.device_value_not_reported)
    val androidVersion = profile.androidRelease
        ?.let { stringResource(R.string.device_android_version_value, it, profile.apiLevel) }
        ?: stringResource(R.string.device_api_level_value, profile.apiLevel)
    val rows = buildList {
        if (detailed) {
            add(
                InfoRow(
                    "manufacturer",
                    stringResource(R.string.device_label_manufacturer),
                    profile.manufacturer ?: notReported
                )
            )
        }
        add(
            InfoRow(
                "model",
                stringResource(R.string.device_label_model),
                profile.model ?: notReported
            )
        )
        add(
            InfoRow(
                "android",
                stringResource(R.string.device_label_android_version),
                androidVersion
            )
        )
        add(
            InfoRow(
                key = "patch",
                label = stringResource(R.string.device_label_security_patch),
                value = profile.securityPatchLevel ?: notReported,
                technical = profile.securityPatchLevel != null
            )
        )
    }
    CompactInfoTable(rows = rows, modifier = modifier)
}

private const val PREVIEW_API_LEVEL = 36

@PreviewLightDark
@Composable
private fun DeviceScreenPreview() {
    ScadangersPreviewSurface {
        DeviceScreen(
            DeviceProfile.fromReportedValues(
                "Example maker",
                "Example model",
                "16",
                PREVIEW_API_LEVEL,
                "2026-01-01"
            )
        )
    }
}
