package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.R
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersShapes
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme

/** Severity tag. The name is always written out; color only reinforces it. */
@Composable
fun SeverityLabel(severity: Severity, modifier: Modifier = Modifier) {
    val colors = ScadangersTheme.colors.severityColors(severity)
    val label = severity.label()
    val description = stringResource(R.string.severity_description, label)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = colors.onContainer,
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .background(colors.container, ScadangersShapes.small)
            .padding(horizontal = ScadangersSpacing.sm, vertical = ScadangersSpacing.xxs)
    )
}

@PreviewLightDark
@Composable
private fun SeverityLabelPreview() {
    ScadangersPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.sm)) {
            Severity.entries.forEach { SeverityLabel(it) }
        }
    }
}
