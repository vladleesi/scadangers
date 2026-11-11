package dev.vladleesi.scadangers.core.security.device

enum class BulletinScope { Aosp, VendorComponent }

/** Manually reviewed facts from the linked ASB, not executable intelligence or exploit tests. */
data class BulletinIssue(
    val cve: String,
    val component: String,
    val severity: DeviceSeverity,
    val patchLevel: PatchDate,
    val apiLevels: IntRange,
    val officialReference: String,
    val scope: BulletinScope = BulletinScope.Aosp
)

/** Bundled, deliberately incomplete catalog. Updates require a reviewed app release. */
object AndroidBulletins {
    const val VERSION = "2026-11-11.1"
    const val REVIEWED_ON = "2026-11-11"
    const val INDEX = "https://source.android.com/docs/security/bulletin"
    const val NOVEMBER_2026 = "$INDEX/2026/2026-11-01"
    const val SEPTEMBER_2023 = "$INDEX/2023-09-01"
    const val SEPTEMBER_2017 = "$INDEX/2017-09-01"

    // Numeric ranges are the reviewed Android release-to-API mappings, not algorithm thresholds.
    @Suppress("MagicNumber")
    val issues: List<BulletinIssue> = listOf(
        issue(
            "CVE-2017-0781",
            "System / Bluetooth",
            DeviceSeverity.Critical,
            "2017-09-01",
            24..26,
            SEPTEMBER_2017
        ),
        issue(
            "CVE-2023-35674",
            "Framework",
            DeviceSeverity.High,
            "2023-09-01",
            30..33,
            SEPTEMBER_2023
        ),
        issue(
            "CVE-2023-35658",
            "System",
            DeviceSeverity.Critical,
            "2023-09-01",
            30..33,
            SEPTEMBER_2023
        ),
        issue(
            "CVE-2026-58868",
            "Framework",
            DeviceSeverity.Critical,
            "2026-11-01",
            34..37,
            NOVEMBER_2026
        ),
        issue(
            "CVE-2026-58882",
            "System",
            DeviceSeverity.Critical,
            "2026-11-01",
            34..37,
            NOVEMBER_2026
        ),
        issue(
            "CVE-2026-20519",
            "MediaTek modem",
            DeviceSeverity.Critical,
            "2026-11-05",
            24..37,
            NOVEMBER_2026
        ).copy(scope = BulletinScope.VendorComponent)
    )

    // Mirrors the six independently sourced columns in a bulletin row.
    @Suppress("LongParameterList")
    private fun issue(
        cve: String,
        component: String,
        severity: DeviceSeverity,
        patch: String,
        apiLevels: IntRange,
        reference: String
    ): BulletinIssue = BulletinIssue(
        cve,
        component,
        severity,
        requireNotNull(PatchDate.parse(patch)),
        apiLevels,
        reference
    )
}
