package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.R
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersIcons
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Nothing to show. When the list is empty because no assessment has run, say so in [body];
 * an empty list must never read as "the device is safe".
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = ScadangersIcons.Inbox,
    action: (@Composable () -> Unit)? = null
) {
    StatePanel(icon, ScadangersTheme.colors.textTertiary, title, body, modifier) {
        action?.invoke()
    }
}

/**
 * Loading content. Shows a spinner, or a static icon when reduced motion is on; both are
 * announced as indeterminate progress.
 */
@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ScadangersSpacing.xl)
            .progressSemantics()
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)
    ) {
        if (ScadangersTheme.reducedMotion) {
            StateIcon(ScadangersIcons.Search, ScadangersTheme.colors.textTertiary)
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(ScadangersIcons.Size.state),
                color = ScadangersTheme.colors.accent,
                trackColor = ScadangersTheme.colors.surfaceSubtle
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ScadangersTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A failed operation. Never use this to soften a failed security assessment into a neutral
 * result: state what failed and that the outcome is unknown.
 */
@Composable
fun ErrorState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    StatePanel(
        icon = ScadangersIcons.Error,
        iconTint = ScadangersTheme.colors.critical.content,
        title = title,
        body = body,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.state_retry)) }
        }
    }
}

/**
 * A feature needs a runtime permission. Explain what it is used for before the system
 * prompt; [onGrant] should launch the platform permission request.
 */
@Composable
fun PermissionRequiredState(
    title: String,
    rationale: String,
    grantLabel: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    StatePanel(ScadangersIcons.Lock, ScadangersTheme.colors.accent, title, rationale, modifier) {
        Button(onClick = onGrant) { Text(grantLabel) }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
private fun StatePanel(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth()
            .widthIn(max = ScadangersDimensions.maxReadableWidth)
            .padding(horizontal = ScadangersSpacing.lg, vertical = ScadangersSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.sm)
    ) {
        StateIcon(icon, iconTint)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = ScadangersTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        actions()
    }
}

@Composable
private fun StateIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.padding(bottom = ScadangersSpacing.xs).size(ScadangersIcons.Size.state)
    )
}

@PreviewLightDark
@Composable
private fun StatePanelsPreview() {
    ScadangersPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)) {
            ScadangersCard {
                EmptyState(title = "Nothing here", body = "Example empty state body.")
            }
            ScadangersCard { LoadingState(label = "Loading example") }
            ScadangersCard {
                ErrorState(title = "Couldn't load", body = "Example error body.", onRetry = {})
            }
            ScadangersCard {
                PermissionRequiredState(
                    title = "Permission needed",
                    rationale = "Example rationale explaining why access is needed.",
                    grantLabel = "Allow",
                    onGrant = {},
                    secondaryLabel = "Not now",
                    onSecondary = {}
                )
            }
        }
    }
}
