package dev.vladleesi.scadangers.device

import dev.vladleesi.scadangers.core.designsystem.component.InfoRow
import dev.vladleesi.scadangers.core.security.device.DeviceAssessment
import dev.vladleesi.scadangers.core.security.device.DeviceFinding
import dev.vladleesi.scadangers.core.security.device.FindingStatus
import dev.vladleesi.scadangers.feature.device.DeviceFindingUi
import dev.vladleesi.scadangers.feature.device.DeviceUiState
import java.text.DateFormat
import java.util.Date

internal fun DeviceAssessment.presentation(): DeviceUiState {
    val observed = findings.count { it.status == FindingStatus.ObservedRisk }
    val potential = findings.count { it.status == FindingStatus.PotentialRisk }
    val unknown = findings.count { it.status == FindingStatus.Unknown }
    val timestamp = DateFormat.getDateTimeInstance().format(Date(observations.capturedAt))
    return DeviceUiState(
        assessment = this,
        findings = findings.map { it.presentation(timestamp) },
        summary =
            "$observed observed risks · $potential potential bulletin matches · " +
                "$unknown unknown checks. " +
                "Assessed $timestamp. This is a limited posture assessment, not an " +
                "overall safety verdict."
    )
}

private fun DeviceFinding.presentation(timestamp: String): DeviceFindingUi = DeviceFindingUi(
    this,
    listOf(
        InfoRow("id", "ID", id, technical = true),
        InfoRow("category", "Category", category.name),
        InfoRow(
            "status",
            "Result",
            when (status) {
                FindingStatus.ObservedRisk -> "Observed risk"
                FindingStatus.PotentialRisk -> "Potential applicability; unverified"
                FindingStatus.ReportedProtection -> "Protection reported by Android"
                FindingStatus.Unknown -> "Unknown / insufficient information"
                FindingStatus.NotApplicable -> "Outside listed releases; not assessed"
                FindingStatus.Information -> "Information"
            }
        ),
        InfoRow("severity", "Severity", severity.name),
        InfoRow("confidence", "Confidence", confidence.name),
        InfoRow("component", "Affected component", affectedComponent),
        InfoRow("timestamp", "Detected at", timestamp),
        InfoRow("automatic", "Automatic remediation", "Unavailable; user action required")
    )
)
