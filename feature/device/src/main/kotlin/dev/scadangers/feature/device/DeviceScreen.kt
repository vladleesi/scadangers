package dev.scadangers.feature.device

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.common.device.DeviceProfile
import dev.scadangers.core.designsystem.component.CardContent
import dev.scadangers.core.designsystem.component.CompactInfoTable
import dev.scadangers.core.designsystem.component.InfoRow
import dev.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.scadangers.core.designsystem.component.SectionCard
import dev.scadangers.core.designsystem.component.SecurityStatus
import dev.scadangers.core.designsystem.component.SecurityStatusIndicator
import dev.scadangers.core.designsystem.component.readableWidth
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

/** Device details: system-reported facts and the (unavailable) device assessment. */
@Composable
fun DeviceScreen(profile: DeviceProfile, modifier: Modifier = Modifier) {
    ScreenLazyColumn(modifier) {
        item(key = "reported") {
            SectionCard(
                title = stringResource(R.string.device_reported_heading),
                modifier = Modifier.readableWidth()
            ) {
                DeviceInfoTable(profile, detailed = true)
            }
        }
        item(key = "assessment") {
            SectionCard(
                title = stringResource(R.string.device_assessment_heading),
                modifier = Modifier.readableWidth()
            ) {
                CardContent {
                    Row(
                        Modifier.semantics(mergeDescendants = true) {
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.device_assessment_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.width(ScadangersSpacing.sm).weight(1f))
                        SecurityStatusIndicator(SecurityStatus.Unavailable)
                    }
                    Text(
                        text = stringResource(R.string.device_assessment_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = ScadangersTheme.colors.textSecondary
                    )
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
