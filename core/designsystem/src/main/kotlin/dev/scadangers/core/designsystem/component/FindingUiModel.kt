package dev.scadangers.core.designsystem.component

import androidx.compose.runtime.Immutable

/**
 * Display model for one finding or detected event. Build it from real assessment results
 * only; strings are already localized and formatted.
 *
 * @property id stable identifier, used as the lazy-list key.
 * @property detectedAt formatted detection time, for example "Nov 10, 2026".
 * @property category short, localized finding type such as "Insecure configuration".
 */
@Immutable
data class FindingUiModel(
    val id: String,
    val title: String,
    val severity: Severity,
    val category: String,
    val detectedAt: String
)
