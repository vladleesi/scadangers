package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.R
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import java.text.NumberFormat

/**
 * Single metric with a label, a prominent value and optional supporting text.
 * Screen readers announce label, value and supporting text as one item.
 */
@Composable
fun MetricsCard(
    label: String,
    value: MetricValue,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    val displayed = metricDisplayText(value)
    val spoken = if (value is MetricValue.Unavailable) {
        stringResource(
            R.string.metric_unavailable
        )
    } else {
        displayed
    }
    val description = listOfNotNull(label, spoken, supportingText).joinToString(", ")
    ScadangersCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Column(
            modifier = Modifier.padding(ScadangersSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ScadangersTheme.colors.textSecondary
            )
            Text(
                text = displayed,
                style = ScadangersTheme.typography.metric,
                color = if (value is MetricValue.Unavailable) {
                    ScadangersTheme.colors.textTertiary
                } else {
                    ScadangersTheme.colors.textPrimary
                }
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScadangersTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun metricDisplayText(value: MetricValue): String = when (value) {
    is MetricValue.Count -> {
        val locale = LocalConfiguration.current.locales[0]
        remember(value.count, locale) {
            NumberFormat.getIntegerInstance(locale).format(value.count)
        }
    }

    is MetricValue.Text -> value.text

    MetricValue.Unavailable -> stringResource(R.string.metric_unavailable_symbol)
}

private const val PREVIEW_COUNT = 1234L

@PreviewLightDark
@Composable
private fun MetricsCardPreview() {
    ScadangersPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.md)) {
            MetricsCard(
                label = "Example count",
                value = MetricValue.Count(PREVIEW_COUNT),
                supportingText = "Preview data"
            )
            MetricsCard(
                label = "Example metric",
                value = MetricValue.Unavailable,
                supportingText = "Not assessed"
            )
        }
    }
}
