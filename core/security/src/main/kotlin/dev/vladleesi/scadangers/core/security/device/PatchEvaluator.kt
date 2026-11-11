package dev.vladleesi.scadangers.core.security.device

internal object PatchEvaluator {
    fun evaluate(observations: DeviceObservations): List<DeviceFinding> {
        val patch = PatchDate.parse(observations.profile.securityPatchLevel)
            ?.takeIf { it.epochMillis <= observations.capturedAt }
        return listOf(
            platform(observations),
            patchFinding(observations, patch),
            support(observations)
        ) +
            AndroidBulletins.issues.map { issue -> match(observations, patch, issue) }
    }

    private fun platform(observations: DeviceObservations): DeviceFinding = observations.finding(
        "device.platform",
        DeviceCategory.Platform,
        "Reported platform",
        with(observations.profile) {
            "Manufacturer: ${manufacturer ?: "unknown"}; model: ${model ?: "unknown"}; " +
                "Android: ${androidRelease ?: "unknown"}; API: $apiLevel. " +
                "These Build values identify the reported platform; they do not " +
                "authenticate the device."
        },
        "https://developer.android.com/reference/android/os/Build",
        status = FindingStatus.Information,
        confidence = DeviceConfidence.High,
        settings = DeviceSettings.DeviceInfo
    )

    private fun patchFinding(observations: DeviceObservations, patch: PatchDate?): DeviceFinding {
        val ageDays = patch?.let { (observations.capturedAt - it.epochMillis) / DAY_MILLIS }
        val old = ageDays != null && ageDays > PATCH_REVIEW_DAYS
        val explanation = if (ageDays == null) {
            "Patch date is missing, invalid or later than the device clock. " +
                "Reported: ${observations.profile.securityPatchLevel ?: "not reported"}. " +
                "Correct the clock or consult the manufacturer; no patch coverage is established."
        } else {
            "Build.VERSION.SECURITY_PATCH: ${patch.isoDate}; $ageDays days before assessment. " +
                "Age over $PATCH_REVIEW_DAYS days is a local review policy, not a " +
                "vendor support or exploit verdict. " +
                "ASB -01 and -05 levels cover different sets of fixes."
        }
        return observations.finding(
            "device.patch", DeviceCategory.Platform, "Security patch level", explanation,
            AndroidBulletins.INDEX,
            status = when {
                patch == null -> FindingStatus.Unknown
                old -> FindingStatus.ObservedRisk
                else -> FindingStatus.Information
            },
            severity = if (old) DeviceSeverity.Medium else DeviceSeverity.Informational,
            confidence = if (patch == null) DeviceConfidence.Unknown else DeviceConfidence.High,
            remediation = "Check for system and Google Play system updates. Ask the manufacturer " +
                "about your exact model, region and carrier if updates are unavailable.",
            settings = DeviceSettings.Updates
        )
    }

    private fun support(observations: DeviceObservations): DeviceFinding = observations.finding(
        "device.support",
        DeviceCategory.Platform,
        "Manufacturer security update support: unknown",
        "Android exposes no universal public API for an exact model's support end date. " +
            "Manufacturer/model, Android release and patch age cannot establish " +
            "remaining support. " +
            "This catalog does not contain model/region/carrier support schedules.",
        AndroidBulletins.INDEX,
        remediation =
            "Consult the manufacturer's official support schedule for your exact model, " +
                "region and carrier. Consider a supported replacement if security " +
                "updates have ended.",
        settings = DeviceSettings.Updates
    )

    private fun match(
        observations: DeviceObservations,
        patch: PatchDate?,
        issue: BulletinIssue
    ): DeviceFinding {
        val status = matchStatus(observations, patch, issue)
        val explanation = when (status) {
            FindingStatus.Unknown -> if (issue.scope == BulletinScope.VendorComponent) {
                "Vendor component applicability is unknown: chipset, modem firmware and affected " +
                    "product revisions are not verified. Device brand alone cannot " +
                    "establish applicability or patch coverage."
            } else {
                "Insufficient patch-date or clock information to compare with this bulletin."
            }

            FindingStatus.NotApplicable ->
                "The reported API is outside this entry's listed updated AOSP releases. " +
                    "This is not proof that unlisted older releases are unaffected."

            FindingStatus.ReportedProtection ->
                "The reported patch level meets this entry's ASB fix level. " +
                    "This is the OS patch claim, not independent verification of installed code."

            else ->
                "The reported API matches listed AOSP releases and the patch predates this fix. " +
                    "Potential applicability only; OEM backports, build configuration " +
                    "and mitigations are unverified. " +
                    "No exploit test was performed."
        }
        return observations.finding(
            issue.cve, DeviceCategory.Platform, "${issue.cve} · ${issue.component}",
            "${issue.component}; fix level ${issue.patchLevel.isoDate}; " +
                "listed API mapping ${issue.apiLevels}; " +
                "reported API ${observations.profile.apiLevel}, " +
                "patch ${observations.profile.securityPatchLevel ?: "unknown"}. $explanation",
            issue.officialReference, status = status, severity = issue.severity,
            confidence = when (status) {
                FindingStatus.Unknown -> DeviceConfidence.Unknown
                FindingStatus.PotentialRisk -> DeviceConfidence.Low
                else -> DeviceConfidence.Medium
            },
            component = issue.component,
            remediation =
                "Install manufacturer security updates. Confirm this CVE's applicability " +
                    "with the vendor for your exact firmware; do not assume the device " +
                    "is exploitable.",
            settings = DeviceSettings.Updates
        )
    }

    private fun matchStatus(
        observations: DeviceObservations,
        patch: PatchDate?,
        issue: BulletinIssue
    ): FindingStatus = when {
        issue.scope == BulletinScope.VendorComponent -> FindingStatus.Unknown

        observations.profile.apiLevel !in issue.apiLevels -> FindingStatus.NotApplicable

        patch == null || issue.patchLevel.epochMillis > observations.capturedAt ->
            FindingStatus.Unknown

        patch >= issue.patchLevel -> FindingStatus.ReportedProtection

        else -> FindingStatus.PotentialRisk
    }

    private const val DAY_MILLIS = 86_400_000L
    private const val PATCH_REVIEW_DAYS = 90
}
