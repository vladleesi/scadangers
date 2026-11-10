package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersShapes
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Bordered content section with a heading row and an optional trailing action. Content is
 * placed edge to edge below a divider so rows and tables can draw full-width separators.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ScadangersCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ScadangersSpacing.lg, end = ScadangersSpacing.xs)
                .padding(vertical = ScadangersSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ScadangersTheme.colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = ScadangersSpacing.md)
                    .semantics { heading() }
            )
            action?.invoke()
        }
        HorizontalDivider(
            thickness = ScadangersDimensions.border,
            color = ScadangersTheme.colors.border
        )
        content()
    }
}

/** Plain bordered surface: the base container for cards. Flat, with a 1 dp border. */
@Composable
fun ScadangersCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = ScadangersShapes.medium,
        color = ScadangersTheme.colors.surface,
        contentColor = ScadangersTheme.colors.textPrimary,
        border = BorderStroke(ScadangersDimensions.border, ScadangersTheme.colors.border)
    ) {
        Column(content = content)
    }
}

/** Padding for free-form content inside a [SectionCard] or [ScadangersCard]. */
@Composable
fun CardContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(ScadangersSpacing.sm),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(ScadangersSpacing.lg),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@PreviewLightDark
@Composable
private fun SectionCardPreview() {
    ScadangersPreviewSurface {
        SectionCard(title = "Section title", action = {
            TextButton(onClick = {}) { Text("Action") }
        }) {
            CardContent {
                Box { Text("Section content", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
