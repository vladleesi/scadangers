package dev.vladleesi.scadangers.core.security.device

import dev.vladleesi.scadangers.core.common.device.DeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePostureEvaluatorTest {
    private val evaluator = DevicePostureEvaluator()

    @Test
    fun patchedAndUnpatchedReportsDoNotBecomeExploitVerdicts() {
        val unpatched = evaluate("2026-10-05").issue("CVE-2026-58868")
        assertEquals(FindingStatus.PotentialRisk, unpatched.status)
        assertEquals(DeviceSeverity.Critical, unpatched.severity)
        assertEquals(DeviceConfidence.Low, unpatched.confidence)
        assertTrue(unpatched.evidence.contains("OEM backports"))
        val patched = evaluate("2026-11-01").issue("CVE-2026-58868")
        assertEquals(FindingStatus.ReportedProtection, patched.status)
        assertEquals(DeviceSeverity.Critical, patched.severity)
        assertTrue(patched.evidence.contains("not independent verification"))
    }

    @Test
    fun strictPatchParserRejectsMissingInvalidAndRolloverDates() {
        listOf(
            null,
            "",
            "unknown",
            "2026-2-01",
            "2026-02-29",
            "2026-13-01",
            "2026-01-00",
            "2026-11-01suffix"
        ).forEach {
            assertNull("Unexpected valid date: $it", PatchDate.parse(it))
            assertEquals(FindingStatus.Unknown, evaluate(it).issue("CVE-2026-58868").status)
        }
        assertNotNull(PatchDate.parse("2024-02-29"))
    }

    @Test
    fun futurePatchAndIncorrectClockCannotEstablishProtection() {
        assertEquals(FindingStatus.Unknown, evaluate("2027-01-05").issue("CVE-2026-58868").status)
        val earlyClock = fixture().copy(
            capturedAt = requireNotNull(PatchDate.parse("2026-10-01")).epochMillis
        )
        assertEquals(
            FindingStatus.Unknown,
            evaluator.evaluate(earlyClock).issue("CVE-2026-58868").status
        )
    }

    @Test
    fun vendorComponentStaysUnknownEvenForMatchingBrandOrNewPatch() {
        listOf("Google", "Samsung", "MediaTek", null).forEach { manufacturer ->
            val input = fixture("2026-11-05")
            val result = evaluator.evaluate(
                input.copy(profile = input.profile.copy(manufacturer = manufacturer))
            )
            val vendor = result.issue("CVE-2026-20519")
            assertEquals(FindingStatus.Unknown, vendor.status)
            assertEquals(DeviceSeverity.Critical, vendor.severity)
            assertEquals(DeviceConfidence.Unknown, vendor.confidence)
            assertEquals(FindingStatus.Unknown, result.issue("device.support").status)
        }
    }

    @Test
    fun listedReleaseMappingCoversEverySupportedApiWithoutPretendingOlderVersionsAreSafe() {
        for (api in 24..37) {
            val result = evaluate("2016-01-01", api)
            assertEquals(api, result.observations.profile.apiLevel)
            val current = result.issue("CVE-2026-58868")
            assertEquals(
                if (api >= 34) FindingStatus.PotentialRisk else FindingStatus.NotApplicable,
                current.status
            )
            if (api < 34) assertTrue(current.evidence.contains("not proof"))
            assertTrue(result.limitations.contains("27–29"))
        }
        assertEquals(
            FindingStatus.PotentialRisk,
            evaluate("2017-08-05", 24).issue("CVE-2017-0781").status
        )
        assertEquals(
            FindingStatus.ReportedProtection,
            evaluate("2017-09-01", 26).issue("CVE-2017-0781").status
        )
        assertEquals(
            FindingStatus.PotentialRisk,
            evaluate("2023-08-05", 31).issue("CVE-2023-35674").status
        )
    }

    @Test
    fun noGoogleServicesAreNeededAndUnevaluatedGoogleSignalsAreNotRisk() {
        val result = evaluate("2026-11-05")
        assertEquals(FindingStatus.Unknown, result.issue("device.integrity").status)
        assertEquals(DeviceSeverity.Informational, result.issue("device.integrity").severity)
        assertEquals(FindingStatus.ReportedProtection, result.issue("device.lock").status)
        assertTrue(
            result.issue("device.integrity").evidence.contains("No Google service is required")
        )
    }

    @Test
    fun missingAndRestrictedSignalsNeverBecomeCleanResults() {
        val input = fixture().copy(
            secureLock = unknown,
            encryption = EncryptionSignal(EncryptionState.Unknown, "restricted"),
            adb = unknown,
            network = NetworkObservation(unknown, unknown, unknown, unknown, unknown)
        )
        val result = evaluator.evaluate(input)
        listOf(
            "device.lock",
            "device.encryption",
            "device.adb",
            "device.network",
            "device.dns"
        ).forEach {
            assertEquals(FindingStatus.Unknown, result.issue(it).status)
        }
    }

    @Test
    fun verifiedLockAndEncryptionIndicatorsHaveHighConfidenceWhileSeverityVaries() {
        val input = fixture().copy(
            secureLock = disabled,
            encryption = EncryptionSignal(EncryptionState.DefaultKey, "default key"),
            adb = enabled
        )
        val result = evaluator.evaluate(input)
        assertEquals(FindingStatus.ObservedRisk, result.issue("device.lock").status)
        assertEquals(DeviceSeverity.High, result.issue("device.lock").severity)
        assertEquals(DeviceConfidence.High, result.issue("device.lock").confidence)
        assertEquals(DeviceSeverity.Medium, result.issue("device.encryption").severity)
        assertEquals(DeviceSeverity.Low, result.issue("device.adb").severity)
    }

    @Test
    fun privateDnsAbsenceIsLimitedSignalAndVpnAbsenceIsNotThreat() {
        val input = fixture().copy(
            network = NetworkObservation(enabled, disabled, disabled, disabled, disabled)
        )
        val result = evaluator.evaluate(input)
        assertEquals(FindingStatus.ObservedRisk, result.issue("device.dns").status)
        assertEquals(DeviceSeverity.Low, result.issue("device.dns").severity)
        assertEquals(FindingStatus.Information, result.issue("device.network").status)
        assertTrue(result.issue("device.dns").evidence.contains("different DNS"))
        val captive = evaluator.evaluate(
            input.copy(network = input.network.copy(captivePortal = enabled))
        )
        assertEquals(FindingStatus.ObservedRisk, captive.issue("device.network").status)
    }

    @Test
    fun findingsHaveCompleteRiskMetadataAndNoAutomaticFixes() {
        val result = evaluate("2026-10-05")
        assertEquals(result.findings.size, result.findings.map { it.id }.distinct().size)
        result.findings.forEach {
            assertFalse(it.id.isBlank())
            assertFalse(it.evidence.isBlank())
            assertFalse(it.affectedComponent.isBlank())
            assertEquals(result.observations.capturedAt, it.detectedAt)
            assertTrue(it.officialReference.startsWith("https://"))
            assertFalse(it.remediation.isBlank())
            assertFalse(it.automaticRemediationAvailable)
        }
    }

    private fun evaluate(patch: String?, api: Int = 37): DeviceAssessment =
        evaluator.evaluate(fixture(patch, api))

    private fun DeviceAssessment.issue(id: String): DeviceFinding = findings.single { it.id == id }

    private fun fixture(patch: String? = "2026-10-05", api: Int = 37): DeviceObservations =
        DeviceObservations(
            DeviceProfile.fromReportedValues("Example", "Synthetic device", "17", api, patch),
            requireNotNull(PatchDate.parse("2026-11-11")).epochMillis,
            enabled,
            EncryptionSignal(EncryptionState.PerUser, "per-user encryption"),
            disabled,
            NetworkObservation(enabled, enabled, disabled, disabled, disabled),
            DeviceSettings.entries.associateWith { enabled }
        )

    private companion object {
        val unknown = DeviceSignal(SignalState.Unknown, "not available")
        val enabled = DeviceSignal(SignalState.Enabled, "reported enabled")
        val disabled = DeviceSignal(SignalState.Disabled, "reported disabled")
    }
}
