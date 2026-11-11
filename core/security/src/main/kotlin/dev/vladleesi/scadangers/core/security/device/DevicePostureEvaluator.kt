package dev.vladleesi.scadangers.core.security.device

/** Local, deterministic evaluation. No assessment means no claim that the device is safe. */
class DevicePostureEvaluator {
    fun evaluate(observations: DeviceObservations): DeviceAssessment {
        val findings = PatchEvaluator.evaluate(observations) +
            protectionFindings(observations) + networkFindings(observations)
        return DeviceAssessment(
            observations,
            findings,
            AndroidBulletins.VERSION,
            AndroidBulletins.REVIEWED_ON,
            "Point-in-time, current user/profile assessment using public Android APIs. " +
                "OS-reported values are not remote attestation. No root, malware, bootloader, " +
                "Verified Boot or hardware-key protection verdict is inferred. " +
                "The bundled catalog contains 6 selected ASB entries from September 2017, " +
                "September 2023 and November 2026; it is not a complete CVE inventory. " +
                "APIs 27–29 have no AOSP entries in this catalog. " +
                "Missing matches do not establish safety. OEM backports, " +
                "kernel/chipset versions, " +
                "vendor fixes and Google Play system updates cannot be verified here. " +
                "No live bulletin or manufacturer support lookup runs. Network state can change " +
                "during collection. Wi-Fi encryption, app DNS, traffic interception and " +
                "other users/profiles are not assessed. Remediation requires user action. " +
                "On older Android versions, Updates opens device information; " +
                "follow the manufacturer update menu."
        )
    }
}

internal fun DeviceObservations.finding(
    id: String,
    category: DeviceCategory,
    title: String,
    evidence: String,
    reference: String,
    status: FindingStatus = FindingStatus.Unknown,
    severity: DeviceSeverity = DeviceSeverity.Informational,
    confidence: DeviceConfidence = DeviceConfidence.Unknown,
    component: String = title,
    remediation: String = "Review the system settings and manufacturer documentation.",
    settings: DeviceSettings? = DeviceSettings.Security
): DeviceFinding = DeviceFinding(
    id, category, title, status, severity, confidence, evidence, component, capturedAt,
    reference, remediation, settings
)

private fun protectionFindings(observations: DeviceObservations): List<DeviceFinding> {
    val lock = observations.secureLock
    return listOf(
        observations.finding(
            "device.lock", DeviceCategory.LockScreen, "Secure screen lock", lock.evidence,
            "https://developer.android.com/reference/android/app/KeyguardManager#isDeviceSecure()",
            status = lock.state.protectionStatus(),
            severity = if (lock.state ==
                SignalState.Disabled
            ) {
                DeviceSeverity.High
            } else {
                DeviceSeverity.Informational
            },
            confidence = lock.state.confidence(),
            remediation = "Set a strong PIN or password in Security settings. This " +
                "check does not measure credential strength."
        ),
        encryptionFinding(observations),
        adbFinding(observations),
        observations.finding(
            "device.settings",
            DeviceCategory.Settings,
            "Security settings availability",
            observations.settings.entries.joinToString("; ") { "${it.key}: ${it.value.evidence}" },
            "https://developer.android.com/reference/android/provider/Settings",
            status = FindingStatus.Information,
            confidence = DeviceConfidence.High,
            remediation = "Open the relevant settings below. OEM or administrator " +
                "restrictions may prevent opening or changing settings."
        ),
        observations.finding(
            "device.integrity",
            DeviceCategory.Protection,
            "Play Integrity / Play Protect: not evaluated",
            "No Google service is required or queried. Play Integrity can supply " +
                "device integrity " +
                "and optional Play Protect verdicts, but requires service " +
                "availability, enrollment " +
                "and server-verified responses. Package presence cannot verify protection.",
            "https://developer.android.com/google/play/integrity/verdicts",
            remediation = "On supported devices, review Play Protect in Google Play. " +
                "An unavailable or unevaluated Google verdict is not evidence of compromise.",
            settings = null
        )
    )
}

private fun encryptionFinding(observations: DeviceObservations): DeviceFinding {
    val state = observations.encryption.state
    val status = when (state) {
        EncryptionState.Active, EncryptionState.PerUser -> FindingStatus.ReportedProtection
        EncryptionState.Inactive, EncryptionState.DefaultKey -> FindingStatus.ObservedRisk
        else -> FindingStatus.Unknown
    }
    return observations.finding(
        "device.encryption", DeviceCategory.Encryption, "Storage encryption indicator",
        observations.encryption.evidence,
        "https://developer.android.com/reference/android/app/admin/DevicePolicyManager#getStorageEncryptionStatus()",
        status = status,
        severity = when (state) {
            EncryptionState.Inactive -> DeviceSeverity.High
            EncryptionState.DefaultKey -> DeviceSeverity.Medium
            else -> DeviceSeverity.Informational
        },
        confidence = if (status ==
            FindingStatus.Unknown
        ) {
            DeviceConfidence.Unknown
        } else {
            DeviceConfidence.High
        },
        remediation = "Review encryption and screen lock in Security settings. This reports the " +
            "platform storage indicator, not encryption of external storage or " +
            "hardware-backed key attestation."
    )
}

internal fun SignalState.protectionStatus(): FindingStatus = when (this) {
    SignalState.Enabled -> FindingStatus.ReportedProtection
    SignalState.Disabled -> FindingStatus.ObservedRisk
    else -> FindingStatus.Unknown
}

internal fun SignalState.confidence(): DeviceConfidence = when (this) {
    SignalState.Enabled, SignalState.Disabled -> DeviceConfidence.High
    else -> DeviceConfidence.Unknown
}

private fun adbFinding(observations: DeviceObservations): DeviceFinding = observations.finding(
    "device.adb", DeviceCategory.Settings, "USB debugging", observations.adb.evidence,
    "https://developer.android.com/reference/android/provider/Settings.Global#ADB_ENABLED",
    status = when (observations.adb.state) {
        SignalState.Enabled -> FindingStatus.ObservedRisk
        SignalState.Disabled -> FindingStatus.Information
        else -> FindingStatus.Unknown
    },
    severity = if (observations.adb.state ==
        SignalState.Enabled
    ) {
        DeviceSeverity.Low
    } else {
        DeviceSeverity.Informational
    },
    confidence = observations.adb.state.confidence(),
    remediation = "Disable USB debugging when not needed. Enabled debugging " +
        "does not imply an authorized host, root or compromise.",
    settings = DeviceSettings.Developer
)
