package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.runtime.Immutable

/** A metric's value. Unmeasured metrics are [Unavailable], never a default number. */
@Immutable
sealed interface MetricValue {
    /** A measured count. */
    data class Count(val count: Long) : MetricValue

    /** A non-numeric, already localized value such as "Never". */
    data class Text(val text: String) : MetricValue

    /** Not measured, not implemented, or not assessed. Rendered as a dash. */
    data object Unavailable : MetricValue
}
