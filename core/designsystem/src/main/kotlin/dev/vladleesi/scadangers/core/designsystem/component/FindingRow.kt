package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.R
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * One finding in a list: date, title, severity and category, with a severity-colored leading
 * bar. Use as a lazy-list item with `key = finding.id`.
 */
@Composable
fun FindingRow(finding: FindingUiModel, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val barColor = ScadangersTheme.colors.severityColors(finding.severity).content
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ScadangersDimensions.listRowMinHeight)
            .clickable(
                onClickLabel = stringResource(R.string.finding_open),
                role = Role.Button,
                onClick = { onClick(finding.id) }
            )
            .drawBehind {
                drawRect(
                    barColor,
                    size = Size(ScadangersDimensions.severityBar.toPx(), size.height)
                )
            }
            .padding(
                start = ScadangersSpacing.lg,
                end = ScadangersSpacing.md,
                top = ScadangersSpacing.md
            )
            .padding(bottom = ScadangersSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.xs)
        ) {
            Text(
                text = finding.detectedAt,
                style = MaterialTheme.typography.bodySmall,
                color = ScadangersTheme.colors.textSecondary
            )
            Text(text = finding.title, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                SeverityLabel(finding.severity)
                Text(
                    text = finding.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScadangersTheme.colors.textSecondary
                )
            }
        }
        Icon(
            imageVector = ScadangersIcons.ChevronRight,
            contentDescription = null,
            tint = ScadangersTheme.colors.textTertiary,
            modifier = Modifier.size(ScadangersIcons.Size.medium)
        )
    }
}

@PreviewLightDark
@Composable
private fun FindingRowPreview() {
    ScadangersPreviewSurface {
        ScadangersCard {
            Severity.entries.forEachIndexed { index, severity ->
                if (index > 0) HorizontalDivider(color = ScadangersTheme.colors.border)
                FindingRow(
                    finding = FindingUiModel(
                        id = "preview-$index",
                        title = "Example finding title for preview only",
                        severity = severity,
                        category = "Example category",
                        detectedAt = "Example date"
                    ),
                    onClick = {}
                )
            }
        }
    }
}
