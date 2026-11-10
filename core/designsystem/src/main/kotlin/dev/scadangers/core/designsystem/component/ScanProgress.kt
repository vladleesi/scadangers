package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.R
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersShapes
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme
import kotlin.math.roundToInt

private const val PERCENT = 100

/**
 * Progress of a running scan.
 *
 * @param progress completed fraction in 0..1, or `null` when the total amount of work is not
 * known yet. Values outside the range are clamped.
 * @param detail current step, for example "Checking installed apps". Never include sensitive
 * payloads such as URLs or file contents.
 * @param onCancel shows a cancel action when not `null`. Scans should support cancellation.
 */
@Composable
fun ScanProgress(
    title: String,
    progress: Float?,
    detail: String,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null
) {
    val fraction = progress?.coerceIn(0f, 1f)
    ScadangersCard(modifier) {
        CardContent(verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (fraction != null) {
                        "${(fraction * PERCENT).roundToInt()}%"
                    } else {
                        stringResource(R.string.scan_progress_in_progress)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = ScadangersTheme.colors.textSecondary
                )
            }
            ScanProgressTrack(fraction)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = ScadangersTheme.colors.textSecondary
            )
            if (onCancel != null) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.scan_progress_cancel))
                }
            }
        }
    }
}

@Composable
private fun ScanProgressTrack(fraction: Float?) {
    val colors = ScadangersTheme.colors
    val trackModifier = Modifier.fillMaxWidth().height(ScadangersDimensions.progressTrack)
    when {
        fraction != null -> LinearProgressIndicator(
            progress = { fraction },
            modifier = trackModifier,
            color = colors.accent,
            trackColor = colors.surfaceSubtle,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {}
        )

        ScadangersTheme.reducedMotion -> {
            // Static track instead of a continuous sweep; still announced as indeterminate.
            val inProgress = stringResource(R.string.scan_progress_in_progress)
            Box(
                trackModifier
                    .progressSemantics()
                    .semantics { stateDescription = inProgress }
                    .background(colors.accentContainer, ScadangersShapes.pill)
            )
        }

        else -> LinearProgressIndicator(
            modifier = trackModifier,
            color = colors.accent,
            trackColor = colors.surfaceSubtle,
            strokeCap = StrokeCap.Round
        )
    }
}

private const val PREVIEW_PROGRESS = 0.42f

@PreviewLightDark
@Composable
private fun ScanProgressPreview() {
    ScadangersPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)) {
            ScanProgress(
                title = "Example scan",
                progress = PREVIEW_PROGRESS,
                detail = "Example step",
                onCancel = {}
            )
            ScanProgress(title = "Example scan", progress = null, detail = "Preparing")
        }
    }
}
