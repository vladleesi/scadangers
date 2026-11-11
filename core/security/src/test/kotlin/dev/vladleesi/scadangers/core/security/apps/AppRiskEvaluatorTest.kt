package dev.vladleesi.scadangers.core.security.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRiskEvaluatorTest {
    private val evaluator = AppRiskEvaluator()

    @Test
    fun systemAppsReceiveSameOldTargetAndCapabilityChecks() {
        val result = evaluator.evaluate(
            app().copy(
                system = true,
                targetSdk = 28,
                capabilities = setOf("android.permission.BIND_ACCESSIBILITY_SERVICE")
            ),
            null,
            NOW
        )
        assertTrue(result.findings.any { it.id == "old_target" })
        assertTrue(
            result.findings.any {
                it.id == "special_access" &&
                    "does not prove" in it.explanation
            }
        )
    }

    @Test
    fun alternativeAndUnknownInstallersAreNotMalwareVerdicts() {
        val alternative = evaluator.evaluate(
            app().copy(installer = InstallSource("example.store")),
            null,
            NOW
        )
        val unknown = evaluator.evaluate(app().copy(installer = InstallSource()), null, NOW)
        assertTrue(
            alternative.findings.any {
                it.id == "source_review" &&
                    "legitimate" in it.explanation
            }
        )
        assertTrue(
            unknown.findings.any {
                it.id == "source_unknown" &&
                    it.level == ReviewLevel.Information
            }
        )
    }

    @Test
    fun permissionCombinationsRequireObservedGrants() {
        val permissions =
            listOf(
                AppPermission("android.permission.INTERNET", true),
                AppPermission("android.permission.READ_SMS", null),
                AppPermission("android.permission.RECORD_AUDIO", true)
            )
        assertFalse(
            evaluator.evaluate(app().copy(permissions = permissions), null, NOW).findings.any {
                it.id ==
                    "sensitive_network"
            }
        )
        assertTrue(
            evaluator.evaluate(
                app().copy(
                    permissions = permissions.map {
                        it.copy(granted = true)
                    }
                ),
                null,
                NOW
            )
                .findings.any { it.id == "sensitive_network" }
        )
    }

    @Test
    fun coarseAndFineLocationCountAsOneSensitiveCategory() {
        val permissions = listOf(
            AppPermission("android.permission.INTERNET", true),
            AppPermission("android.permission.ACCESS_COARSE_LOCATION", true),
            AppPermission("android.permission.ACCESS_FINE_LOCATION", true)
        )
        assertFalse(
            evaluator.evaluate(app().copy(permissions = permissions), null, NOW)
                .findings.any { it.id == "sensitive_network" }
        )
    }

    @Test
    fun signingChangeRequiresKnownCurrentFingerprintsAndSameInstall() {
        val previous = evaluator.evaluate(app(), null, NOW)
        val new = app().copy(signing = SigningIdentity(setOf("new")))
        assertTrue(evaluator.evaluate(new, previous, NOW).observedSignerChange)
        assertFalse(
            evaluator.evaluate(
                new.copy(stamp = STAMP.copy(firstInstallTime = NOW)),
                previous,
                NOW
            ).observedSignerChange
        )
        assertFalse(
            evaluator.evaluate(
                new.copy(signing = SigningIdentity()),
                previous,
                NOW
            ).observedSignerChange
        )
    }

    @Test
    fun verifiedRotationIsExplainedAndObservedChangeSurvivesUnchangedRefresh() {
        val rotated = app().copy(
            signing = SigningIdentity(setOf("new"), setOf("fingerprint", "new"), true)
        )
        val previous = evaluator.evaluate(app(), null, NOW)
        val result = evaluator.evaluate(rotated, previous, NOW)
        assertEquals(
            ReviewLevel.Information,
            result.findings.first {
                it.id == "signing_change"
            }.level
        )
        assertTrue(evaluator.evaluate(rotated, result, NOW).observedSignerChange)
    }

    @Test
    fun unknownInstallTimestampDoesNotProveSigningContinuity() {
        val unknownInstall = app().copy(stamp = STAMP.copy(firstInstallTime = 0))
        val previous = evaluator.evaluate(unknownInstall, null, NOW)
        val changed = unknownInstall.copy(signing = SigningIdentity(setOf("new")))
        assertFalse(evaluator.evaluate(changed, previous, NOW).observedSignerChange)
    }

    @Test
    fun unrelatedLineageDoesNotAuthenticateAnObservedSignerChange() {
        val previous = evaluator.evaluate(app(), null, NOW)
        val changed = app().copy(
            signing = SigningIdentity(setOf("new"), setOf("unrelated", "new"), true)
        )
        val result = evaluator.evaluate(changed, previous, NOW)
        assertEquals(ReviewLevel.Review, result.findings.first { it.id == "signing_change" }.level)
        val refreshed = evaluator.evaluate(changed, result, NOW)
        assertEquals(
            ReviewLevel.Review,
            refreshed.findings.first {
                it.id == "signing_change"
            }.level
        )
        assertEquals(setOf("fingerprint"), refreshed.signerBaseline)
        val unavailable = evaluator.evaluate(
            changed.copy(signing = SigningIdentity()),
            refreshed,
            NOW
        )
        assertTrue(unavailable.observedSignerChange)
    }

    @Test
    fun legacySignatureArrayIsNotAProofOfRotation() {
        val legacy = app().copy(signing = SigningIdentity(setOf("a", "b"), multipleSigners = true))
        assertFalse(
            evaluator.evaluate(legacy, null, NOW).findings.any {
                it.id == "signing_change"
            }
        )
    }

    @Test
    fun recentSignalsExpireAndFutureTimesAreNotRecent() {
        assertTrue(evaluator.evaluate(app(), null, NOW).findings.any { it.id == "recent_install" })
        assertFalse(
            evaluator.evaluate(app(), null, NOW + 8L * 24 * 60 * 60 * 1000).findings.any {
                it.id ==
                    "recent_install"
            }
        )
        assertFalse(
            evaluator.evaluate(
                app().copy(stamp = STAMP.copy(firstInstallTime = NOW + 1)),
                null,
                NOW
            )
                .findings.any { it.id == "recent_install" }
        )
    }

    @Test
    fun allAppsRequireFurtherAnalysisAndMissingMetadataIsExplicit() {
        val result = evaluator.evaluate(app().copy(unavailable = setOf("signing")), null, NOW)
        assertTrue(result.findings.any { it.id == "incomplete" })
        assertTrue(result.findings.any { it.id == "analysis" && "no CVE" in it.explanation })
    }
}
