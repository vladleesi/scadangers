package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.R
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersColors
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersShapes
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.core.designsystem.theme.StatusColors

/**
 * Presentation state of a protection feature or security module.
 *
 * Only report [Active] after the feature has verified that it is running. Use [Unavailable]
 * for anything not implemented or not supported on this device, never a positive state.
 */
enum class SecurityStatus {
    /** Verified running. The only status drawn in the positive (green) role. */
    Active,

    /** Running with a problem the user should review. */
    NeedsAttention,

    /** Failed, or a critical problem was found. */
    AtRisk,

    /** Available, but turned off. */
    Inactive,

    /** Not implemented, not supported, or not assessed. */
    Unavailable
}

/** Color role for [status]. */
fun ScadangersColors.statusColors(status: SecurityStatus): StatusColors = when (status) {
    SecurityStatus.Active -> positive
    SecurityStatus.NeedsAttention -> caution
    SecurityStatus.AtRisk -> critical
    SecurityStatus.Inactive, SecurityStatus.Unavailable -> neutral
}

/** Icon paired with [status], so the state never depends on color alone. */
fun SecurityStatus.icon(): ImageVector = when (this) {
    SecurityStatus.Active -> ScadangersIcons.CheckCircle
    SecurityStatus.NeedsAttention -> ScadangersIcons.Warning
    SecurityStatus.AtRisk -> ScadangersIcons.Error
    SecurityStatus.Inactive -> ScadangersIcons.Off
    SecurityStatus.Unavailable -> ScadangersIcons.Unavailable
}

/** Default short label for [status]. */
@Composable
@ReadOnlyComposable
fun SecurityStatus.label(): String = stringResource(
    when (this) {
        SecurityStatus.Active -> R.string.status_active
        SecurityStatus.NeedsAttention -> R.string.status_needs_attention
        SecurityStatus.AtRisk -> R.string.status_at_risk
        SecurityStatus.Inactive -> R.string.status_inactive
        SecurityStatus.Unavailable -> R.string.status_unavailable
    }
)

/**
 * Compact status label: icon plus text on a tinted container. Screen readers announce
 * "Status: <label>".
 *
 * @param label overrides the default label, for example "Not active" for overall protection.
 */
@Composable
fun SecurityStatusIndicator(
    status: SecurityStatus,
    modifier: Modifier = Modifier,
    label: String = status.label()
) {
    val colors = ScadangersTheme.colors.statusColors(status)
    val description = stringResource(R.string.status_description, label)
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .background(colors.container, ScadangersShapes.pill)
            .padding(horizontal = ScadangersSpacing.sm, vertical = ScadangersSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = status.icon(),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(ScadangersIcons.Size.small)
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.onContainer)
    }
}

@PreviewLightDark
@Composable
private fun SecurityStatusIndicatorPreview() {
    ScadangersPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.sm)) {
            SecurityStatus.entries.forEach { SecurityStatusIndicator(it) }
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun SecurityStatusIndicatorLargeTextPreview() {
    ScadangersPreviewSurface { SecurityStatusIndicator(SecurityStatus.NeedsAttention) }
}
