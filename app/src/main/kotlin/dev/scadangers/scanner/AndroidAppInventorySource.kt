package dev.scadangers.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Resources
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.scadangers.core.security.apps.AppInventory
import dev.scadangers.core.security.apps.AppInventorySource
import dev.scadangers.core.security.apps.AppPermission
import dev.scadangers.core.security.apps.InstallSource
import dev.scadangers.core.security.apps.PackageStamp
import dev.scadangers.core.security.apps.PackageUnavailableException
import dev.scadangers.core.security.apps.SigningIdentity
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** PackageManager-only collection for the calling user; no APK opening or icon decoding. */
@Singleton
internal class AndroidAppInventorySource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val events: PackageEvents
) : AppInventorySource {
    private val packageManager get() = context.packageManager
    override val apiLevel get() = Build.VERSION.SDK_INT
    override val generation get() = events.generation

    override suspend fun packages(): Map<String, PackageStamp> = withContext(Dispatchers.IO) {
        installedPackages().associate { it.packageName to it.stamp() }
    }

    override suspend fun read(packageName: String, cached: AppInventory?): AppInventory =
        withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            try {
                val reusable = cached?.takeIf { it.unavailable.isEmpty() }
                val flags = if (reusable == null) fullFlags() else PackageManager.GET_PERMISSIONS
                val info = packageInfo(packageName, flags)
                // Even unchanged versions may have changed runtime permission grants.
                if (reusable != null && reusable.stamp == info.stamp()) {
                    reusable.copy(permissions = info.permissions())
                } else {
                    val complete = if (reusable == null) {
                        info
                    } else {
                        packageInfo(packageName, fullFlags())
                    }
                    inventory(complete)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                throw PackageUnavailableException("Package is no longer visible.")
            } catch (_: SecurityException) {
                throw PackageUnavailableException("Package metadata access denied.")
            }
        }

    private fun inventory(info: PackageInfo): AppInventory {
        val application =
            info.applicationInfo
                ?: throw PackageUnavailableException("Application metadata unavailable.")
        val unavailable = mutableSetOf<String>()
        val source = try {
            installSource(info.packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            unavailable.add("installer")
            InstallSource(observable = false)
        } catch (_: SecurityException) {
            unavailable.add("installer")
            InstallSource(observable = false)
        }
        val signing = info.signingIdentity()
        if (signing.current.isEmpty()) unavailable.add("signing certificates")
        if (info.permissions().any { it.granted == null }) unavailable.add("permission grants")
        val label = try {
            packageManager.getApplicationLabel(application).toString().take(MAX_LABEL_LENGTH)
        } catch (_: Resources.NotFoundException) {
            unavailable.add("app label")
            info.packageName
        }
        return AppInventory(
            packageName = info.packageName,
            label = label,
            versionName = info.versionName,
            stamp = info.stamp(),
            targetSdk = application.targetSdkVersion,
            system = application.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            updatedSystem = application.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            installer = source,
            signing = signing,
            permissions = info.permissions(),
            capabilities = declaredCapabilities(info),
            apkPaths = listOfNotNull(application.sourceDir) + application.splitSourceDirs.orEmpty(),
            splitNames = info.splitNames.orEmpty().toList(),
            unavailable = unavailable.toSet()
        )
    }

    private fun installSource(packageName: String): InstallSource = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.R
    ) {
        val source = packageManager.getInstallSourceInfo(packageName)
        InstallSource(
            source.installingPackageName,
            source.initiatingPackageName,
            source.originatingPackageName,
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                source.packageSource
            } else {
                null
            }
        )
    } else {
        @Suppress("DEPRECATION")
        InstallSource(installingPackage = packageManager.getInstallerPackageName(packageName))
    }

    private fun installedPackages(): List<PackageInfo> = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstalledPackages(0)
    }

    private fun packageInfo(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }

    private fun fullFlags(): Int {
        @Suppress("DEPRECATION")
        val signing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.MATCH_DISABLED_COMPONENTS or
            signing
    }

    private companion object {
        const val MAX_LABEL_LENGTH = 512
    }
}

internal fun PackageInfo.stamp(): PackageStamp {
    @Suppress("DEPRECATION")
    val code = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.P
    ) {
        longVersionCode
    } else {
        versionCode.toLong()
    }
    return PackageStamp(code, firstInstallTime, lastUpdateTime)
}

internal fun PackageInfo.permissions(): List<AppPermission> =
    requestedPermissions.orEmpty().mapIndexed {
            index,
            permission
        ->
        val flags = requestedPermissionsFlags?.getOrNull(index)
        AppPermission(
            permission,
            flags?.let {
                it and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            }
        )
    }

internal fun PackageInfo.signingIdentity(): SigningIdentity = if (Build.VERSION.SDK_INT >=
    Build.VERSION_CODES.P
) {
    val signing = signingInfo
    val multiple = signing?.hasMultipleSigners() == true
    val history = if (multiple) {
        emptyList()
    } else {
        signing?.signingCertificateHistory.orEmpty().toList()
    }
    val current = if (multiple) {
        signing.apkContentsSigners.orEmpty().toList()
    } else {
        history.takeLast(
            1
        )
    }
    SigningIdentity(
        current.fingerprints(),
        history.fingerprints(),
        signing != null && !multiple,
        multiple
    )
} else {
    @Suppress("DEPRECATION")
    val current = signatures.orEmpty().toList()
    SigningIdentity(current = current.fingerprints(), multipleSigners = current.size > 1)
}

private fun List<Signature>.fingerprints(): Set<String> = map { signature ->
    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") {
        "%02X".format(it)
    }
}.toSet()

private fun declaredCapabilities(info: PackageInfo): Set<String> = buildSet {
    info.services.orEmpty().mapNotNull {
        it.permission
    }.filter { it in SERVICE_CAPABILITIES }.forEach { add(it) }
    if (info.receivers.orEmpty().any { it.permission == "android.permission.BIND_DEVICE_ADMIN" }) {
        add("android.permission.BIND_DEVICE_ADMIN")
    }
}

private val SERVICE_CAPABILITIES = setOf(
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
    "android.permission.BIND_VPN_SERVICE"
)
