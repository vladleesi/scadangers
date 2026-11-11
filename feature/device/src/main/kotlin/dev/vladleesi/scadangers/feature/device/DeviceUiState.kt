package dev.vladleesi.scadangers.feature.device

import dev.vladleesi.scadangers.core.designsystem.component.InfoRow
import dev.vladleesi.scadangers.core.security.device.DeviceAssessment
import dev.vladleesi.scadangers.core.security.device.DeviceFinding

/** Prepared off Main by the app state holder. */
data class DeviceFindingUi(val finding: DeviceFinding, val details: List<InfoRow>)

data class DeviceUiState(
    val busy: Boolean = false,
    val assessment: DeviceAssessment? = null,
    val findings: List<DeviceFindingUi> = emptyList(),
    val summary: String = "No assessment has run. Open Device to assess observable settings.",
    val error: String? = null
)
