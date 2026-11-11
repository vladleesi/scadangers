package dev.vladleesi.scadangers.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.vladleesi.scadangers.core.designsystem.component.CardContent
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersCard
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatus
import dev.vladleesi.scadangers.core.designsystem.component.SecurityStatusIndicator
import dev.vladleesi.scadangers.core.designsystem.component.statusColors
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Overall protection. "Protection is active" appears only for [SecurityStatus.Active], which
 * requires a verified running module; every other state reads as not active.
 */
@Composable
internal fun ProtectionCard(
    state: OverviewUiState,
    onQuickScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.protectionStatus == SecurityStatus.Active
    val headline = if (active) {
        R.string.overview_protection_active
    } else {
        R.string.overview_protection_not_active
    }
    ScadangersCard(modifier) {
        CardContent(verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)) {
            ProtectionHeader(state.protectionStatus)
            Text(
                text = stringResource(headline),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            if (state.modules.all { it.status == SecurityStatus.Unavailable }) {
                Text(
                    text = stringResource(R.string.overview_protection_no_modules),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScadangersTheme.colors.textSecondary
                )
            }
            HorizontalDivider(color = ScadangersTheme.colors.border)
            LastScanRow(state.lastSuccessfulScan)
            QuickScanAction(available = state.quickScanAvailable, onQuickScan = onQuickScan)
        }
    }
}

@Composable
private fun ProtectionHeader(status: SecurityStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.sm)
    ) {
        Icon(
            imageVector = ScadangersIcons.Shield,
            contentDescription = null,
            tint = ScadangersTheme.colors.statusColors(status).content,
            modifier = Modifier.size(ScadangersIcons.Size.medium)
        )
        Text(
            text = stringResource(R.string.overview_protection_heading),
            style = MaterialTheme.typography.labelLarge,
            color = ScadangersTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        if (status == SecurityStatus.Active) {
            SecurityStatusIndicator(status)
        } else {
            SecurityStatusIndicator(
                status = status,
                label = stringResource(R.string.overview_protection_not_active_label)
            )
        }
    }
}

/** @param lastSuccessfulScan formatted completion time, or `null` when no scan succeeded. */
@Composable
private fun LastScanRow(lastSuccessfulScan: String?) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.overview_last_scan),
            style = MaterialTheme.typography.bodyMedium,
            color = ScadangersTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = lastSuccessfulScan ?: stringResource(R.string.overview_last_scan_never),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun QuickScanAction(available: Boolean, onQuickScan: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)
    ) {
        Button(onClick = onQuickScan, enabled = available) {
            Icon(
                imageVector = ScadangersIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(ScadangersIcons.Size.small)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.overview_quick_scan))
        }
        if (!available) {
            Text(
                text = stringResource(R.string.overview_quick_scan_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = ScadangersTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
