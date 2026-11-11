package dev.vladleesi.scadangers.core.security.device

import dev.vladleesi.scadangers.core.common.device.DeviceProfile

enum class DeviceSeverity { Informational, Low, Medium, High, Critical }
enum class DeviceConfidence { High, Medium, Low, Unknown }
enum class DeviceCategory { Platform, LockScreen, Encryption, Settings, Network, Protection }
enum class FindingStatus {
    ObservedRisk,
    PotentialRisk,
    ReportedProtection,
    Information,
    Unknown,
    NotApplicable
}
enum class SignalState { Enabled, Disabled, Unknown, Unsupported }
enum class EncryptionState { Active, PerUser, DefaultKey, Inactive, Unsupported, Unknown }
enum class DeviceSettings { Security, Updates, Network, Developer, DeviceInfo }

/** A platform observation, including the reason when a value cannot be read. */
data class DeviceSignal(val state: SignalState, val evidence: String)

data class EncryptionSignal(val state: EncryptionState, val evidence: String)

/** Current calling user/profile only. Network observations refer to the app's default network. */
data class DeviceObservations(
    val profile: DeviceProfile,
    val capturedAt: Long,
    val secureLock: DeviceSignal,
    val encryption: EncryptionSignal,
    val adb: DeviceSignal,
    val network: NetworkObservation,
    val settings: Map<DeviceSettings, DeviceSignal>
)

data class NetworkObservation(
    val connected: DeviceSignal,
    val privateDns: DeviceSignal,
    val captivePortal: DeviceSignal,
    val vpn: DeviceSignal,
    val proxy: DeviceSignal
)

/** Severity describes impact; confidence describes evidence strength, independently of status. */
data class DeviceFinding(
    val id: String,
    val category: DeviceCategory,
    val title: String,
    val status: FindingStatus,
    val severity: DeviceSeverity,
    val confidence: DeviceConfidence,
    val evidence: String,
    val affectedComponent: String,
    val detectedAt: Long,
    val officialReference: String,
    val remediation: String,
    val settings: DeviceSettings? = null,
    val automaticRemediationAvailable: Boolean = false
)

data class DeviceAssessment(
    val observations: DeviceObservations,
    val findings: List<DeviceFinding>,
    val catalogVersion: String,
    val catalogReviewedOn: String,
    val limitations: String
)

/** Collection implementations must move all platform IPC off Main and propagate cancellation. */
fun interface DeviceObservationSource {
    suspend fun read(): DeviceObservations
}
