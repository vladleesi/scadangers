package dev.scadangers.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import dev.scadangers.core.designsystem.R
import dev.scadangers.core.designsystem.theme.ScadangersColors
import dev.scadangers.core.designsystem.theme.StatusColors

/** Risk of a finding, ordered from most to least urgent. */
enum class Severity {
    /** Needs immediate action to prevent exploitation or data exposure. */
    Critical,

    /** Should be addressed to maintain a strong security posture. */
    Moderate,

    /** Hardening suggestion or informational finding. */
    Low
}

/** Color role for [severity]. Severity never uses the positive role or the brand accent. */
fun ScadangersColors.severityColors(severity: Severity): StatusColors = when (severity) {
    Severity.Critical -> critical
    Severity.Moderate -> caution
    Severity.Low -> info
}

/** Localized severity name. */
@Composable
@ReadOnlyComposable
fun Severity.label(): String = stringResource(
    when (this) {
        Severity.Critical -> R.string.severity_critical
        Severity.Moderate -> R.string.severity_moderate
        Severity.Low -> R.string.severity_low
    }
)
