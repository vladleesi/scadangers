package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

private const val LABEL_WEIGHT = 0.45f
private const val VALUE_WEIGHT = 0.55f

/**
 * One label/value pair.
 *
 * @property key stable identifier for the row.
 * @property technical renders the value in monospace, for identifiers and versions.
 */
@Immutable
data class InfoRow(
    val key: String,
    val label: String,
    val value: String,
    val technical: Boolean = false
)

/**
 * Dense label/value table for small, fixed sets of facts (up to about 20 rows; use a lazy
 * list for anything longer). Long values wrap rather than clip, and each row is read by
 * screen readers as one "label, value" item.
 */
@Composable
fun CompactInfoTable(rows: List<InfoRow>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = ScadangersSpacing.lg),
                    thickness = ScadangersDimensions.border,
                    color = ScadangersTheme.colors.border
                )
            }
            InfoTableRow(row)
        }
    }
}

@Composable
private fun InfoTableRow(row: InfoRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ScadangersSpacing.lg, vertical = ScadangersSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg)
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = ScadangersTheme.colors.textSecondary,
            modifier = Modifier.weight(LABEL_WEIGHT)
        )
        Text(
            text = row.value,
            style = if (row.technical) {
                ScadangersTheme.typography.mono
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = ScadangersTheme.colors.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(VALUE_WEIGHT)
        )
    }
}

@PreviewLightDark
@Composable
private fun CompactInfoTablePreview() {
    ScadangersPreviewSurface {
        ScadangersCard {
            CompactInfoTable(
                rows = listOf(
                    InfoRow("a", "Example label", "Example value"),
                    InfoRow("b", "Technical value", "2026-01-01", technical = true),
                    InfoRow(
                        "c",
                        "A longer label that wraps",
                        "A long value that wraps onto another line"
                    )
                )
            )
        }
    }
}
