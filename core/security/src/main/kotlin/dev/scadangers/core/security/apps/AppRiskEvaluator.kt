package dev.scadangers.core.security.apps

/** Local, versioned heuristics. Installer names describe attribution, not authenticated trust. */
class AppRiskEvaluator {
    fun evaluate(app: AppInventory, previous: AssessedApp?, now: Long): AssessedApp {
        val comparison = compareSigners(app, previous)
        val findings = buildList {
            addAll(metadataFindings(app, now))
            addAll(permissionFindings(app))
            signingFinding(app.signing, comparison)?.let { add(it) }
            if (app.unavailable.isNotEmpty()) {
                add(
                    AppFinding(
                        "incomplete",
                        ReviewLevel.Review,
                        "Metadata is incomplete",
                        "Unavailable: ${app.unavailable.sorted().joinToString()}. No clean assessment is implied."
                    )
                )
            }
            add(
                AppFinding(
                    "analysis",
                    ReviewLevel.Information,
                    "Vulnerability analysis not performed",
                    "Package metadata cannot establish bundled-library or APK vulnerabilities. " +
                        "Further analysis is needed; no CVE or malware detection was performed."
                )
            )
        }
        return AssessedApp(app, findings, comparison.changed, comparison.baseline)
    }

    private fun compareSigners(app: AppInventory, previous: AssessedApp?): SignerComparison {
        val sameInstall = app.stamp.firstInstallTime > 0 &&
            previous?.inventory?.stamp?.firstInstallTime == app.stamp.firstInstallTime
        val oldSigners = previous?.inventory?.signing?.current.orEmpty()
        val observedBefore = sameInstall && previous.observedSignerChange
        val changed = observedBefore || (
            sameInstall && oldSigners.isNotEmpty() &&
                app.signing.current.isNotEmpty() && oldSigners != app.signing.current
            )
        val baseline = if (observedBefore) previous.signerBaseline else oldSigners
        return SignerComparison(changed, if (changed) baseline else emptySet())
    }

    private fun signingFinding(
        signing: SigningIdentity,
        comparison: SignerComparison
    ): AppFinding? {
        val hasRotation = signing.historyVerified && signing.history.size > 1
        if (!comparison.changed && !hasRotation) return null
        val provenRotation = hasRotation && (
            !comparison.changed ||
                (
                    comparison.baseline.isNotEmpty() &&
                        signing.history.containsAll(comparison.baseline)
                    )
            )
        return AppFinding(
            "signing_change",
            if (provenRotation) ReviewLevel.Information else ReviewLevel.Review,
            "Signing certificate changed",
            if (provenRotation) {
                "Android reports a verified signing rotation history. Rotation can be legitimate."
            } else {
                "Saved observations of this installation show a signer change. " +
                    "Continuity cannot be established from the available signing history."
            }
        )
    }

    private data class SignerComparison(val changed: Boolean, val baseline: Set<String>)

    private fun metadataFindings(app: AppInventory, now: Long): List<AppFinding> = buildList {
        // An explicit policy threshold, not a claim that newer targets are safe.
        if (app.targetSdk < OLD_TARGET_SDK) {
            add(
                AppFinding(
                    "old_target",
                    ReviewLevel.Review,
                    "Old Android target",
                    "Targets API ${app.targetSdk}, below this policy's " +
                        "API $OLD_TARGET_SDK threshold. " +
                        "Older targets may retain older platform behavior. System apps are assessed too."
                )
            )
        }
        val installer = app.installer.installingPackage
        if (!app.installer.observable || installer == null) {
            add(
                AppFinding(
                    "source_unknown",
                    ReviewLevel.Information,
                    "Installation source unavailable",
                    "Android did not provide installer attribution. This does not establish sideloading or harm."
                )
            )
        } else if (installer !in ExpectedInstallers.packageNames) {
            add(
                AppFinding(
                    "source_review",
                    ReviewLevel.Review,
                    "Review installation source",
                    "Installer attribution is $installer, " +
                        "outside the built-in expected store list. " +
                        "Alternative stores and sideloading can be legitimate; attribution is not a trust verdict."
                )
            )
        }
        if (recent(app.stamp.firstInstallTime, now)) {
            add(
                AppFinding(
                    "recent_install",
                    ReviewLevel.Information,
                    "Recently installed",
                    "Installed within seven days."
                )
            )
        }
        if (app.stamp.lastUpdateTime > app.stamp.firstInstallTime &&
            recent(app.stamp.lastUpdateTime, now)
        ) {
            add(
                AppFinding(
                    "recent_update",
                    ReviewLevel.Information,
                    "Recently updated",
                    "Updated within seven days."
                )
            )
        }
    }

    private fun permissionFindings(app: AppInventory): List<AppFinding> = buildList {
        val granted = app.permissions.filter { it.granted == true }.map { it.name }.toSet()
        val sensitiveCategories = SENSITIVE_PERMISSIONS.count { category ->
            category.any { it in granted }
        }
        if ("android.permission.INTERNET" in granted &&
            sensitiveCategories >= SENSITIVE_CATEGORY_THRESHOLD
        ) {
            add(
                AppFinding(
                    "sensitive_network",
                    ReviewLevel.Review,
                    "Sensitive access with network capability",
                    "Observed grants combine Internet with at least two of these categories: " +
                        "contacts, SMS, microphone and location. " +
                        "This combination may be necessary for the app; it does not prove data collection."
                )
            )
        }
        val special =
            app.permissions.map { it.name }.filter { it in SPECIAL_PERMISSIONS } + app.capabilities
        if (special.isNotEmpty()) {
            add(
                AppFinding(
                    "special_access",
                    ReviewLevel.Review,
                    "Powerful capabilities declared",
                    "Declared: ${special.sorted().joinToString()}. " +
                        "Declaration does not prove that special access " +
                        "is enabled. Review Android settings; other apps' AppOps " +
                        "and service activation are not assessed."
                )
            )
        }
    }

    private fun recent(timestamp: Long, now: Long): Boolean =
        timestamp > 0 && timestamp <= now && now - timestamp <= RECENT_MILLIS

    private companion object {
        const val OLD_TARGET_SDK = 29
        const val RECENT_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val SENSITIVE_CATEGORY_THRESHOLD = 2
        val SENSITIVE_PERMISSIONS = listOf(
            setOf("android.permission.READ_CONTACTS"),
            setOf(
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.SEND_SMS"
            ),
            setOf("android.permission.RECORD_AUDIO"),
            setOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION"
            )
        )
        val SPECIAL_PERMISSIONS = setOf(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SETTINGS",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.REQUEST_DELETE_PACKAGES",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM"
        )
    }
}
