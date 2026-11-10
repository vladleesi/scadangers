package dev.scadangers.core.security.apps

import kotlinx.serialization.Serializable

/** Metadata for one package in the calling Android user/profile. No APK contents are read. */
@Serializable
@Suppress("LongParameterList") // A platform snapshot has independently observable fields.
data class AppInventory(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val stamp: PackageStamp,
    val targetSdk: Int,
    val system: Boolean,
    val updatedSystem: Boolean,
    val installer: InstallSource,
    val signing: SigningIdentity,
    val permissions: List<AppPermission>,
    val capabilities: Set<String>,
    val apkPaths: List<String>,
    val splitNames: List<String>,
    val unavailable: Set<String> = emptySet()
)

/** Cheap identity used to invalidate cached immutable metadata, including reinstallations. */
@Serializable
data class PackageStamp(val versionCode: Long, val firstInstallTime: Long, val lastUpdateTime: Long)

@Serializable
data class InstallSource(
    val installingPackage: String? = null,
    val initiatingPackage: String? = null,
    val originatingPackage: String? = null,
    val packageSource: Int? = null,
    val observable: Boolean = true
)

/** SHA-256 fingerprints. Legacy GET_SIGNATURES cannot establish a rotation history. */
@Serializable
data class SigningIdentity(
    val current: Set<String> = emptySet(),
    val history: Set<String> = emptySet(),
    val historyVerified: Boolean = false,
    val multipleSigners: Boolean = false
)

@Serializable
data class AppPermission(val name: String, val granted: Boolean?)

@Serializable
enum class ReviewLevel { Information, Review }

@Serializable
data class AppFinding(
    val id: String,
    val level: ReviewLevel,
    val title: String,
    val explanation: String
)

/** Findings are review signals, never malware verdicts or an assertion of safety. */
@Serializable
data class AssessedApp(
    val inventory: AppInventory,
    val findings: List<AppFinding>,
    val observedSignerChange: Boolean = false,
    val signerBaseline: Set<String> = emptySet()
) {
    val level: ReviewLevel get() = findings.maxOfOrNull { it.level } ?: ReviewLevel.Information
}

@Serializable
data class ScanSnapshot(
    val completedAt: Long,
    val apiLevel: Int,
    val visibilityLimited: Boolean,
    val apps: List<AssessedApp>,
    val inaccessible: Map<String, String>
)

/** Built-in installer attribution list; package names alone do not authenticate a store. */
object ExpectedInstallers {
    val packageNames: Set<String> =
        setOf("com.android.vending", "com.amazon.venezia", "com.sec.android.app.samsungapps")
}
