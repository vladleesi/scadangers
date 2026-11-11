package dev.vladleesi.scadangers.core.security.capability

/** Access only: a grant never means that an engine is running or a device is safe. */
enum class Capability {
    InstalledApps,
    DeviceInformation,
    Notifications,
    BluetoothScanning,
    WifiScanning,
    NearbyWifi,
    LocalNetwork,
    VpnAuthorization
}

enum class Hardware { BluetoothLe, Wifi }

enum class SpecialAccess { NotificationSettings, VpnConsent }

/** Android permission names stay Android-independent so the policy can be tested on the JVM. */
object Permissions {
    const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val BLUETOOTH = "android.permission.BLUETOOTH"
    const val BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN"
    const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
    const val NEARBY_WIFI = "android.permission.NEARBY_WIFI_DEVICES"
    const val NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
    const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    const val WIFI_STATE = "android.permission.ACCESS_WIFI_STATE"
    const val CHANGE_WIFI_STATE = "android.permission.CHANGE_WIFI_STATE"
    const val INTERNET = "android.permission.INTERNET"
}

data class AccessRequirements(
    val runtimePermissions: Set<String> = emptySet(),
    val manifestPermissions: Set<String> = emptySet(),
    val hardware: Hardware? = null,
    val specialAccess: SpecialAccess? = null,
    val locationServicesRequired: Boolean = false,
    val limitedVisibility: Boolean = false
) {
    /** Android 12+ requires coarse and fine to be requested together for precise location. */
    fun requestPermissions(apiLevel: Int): Set<String> =
        if (apiLevel >= AndroidApi.ANDROID_12 && Permissions.FINE_LOCATION in runtimePermissions) {
            runtimePermissions + Permissions.COARSE_LOCATION
        } else {
            runtimePermissions
        }
}

enum class AccessStatus { Unknown, NotRequested, Granted, Denied, Revoked, Unsupported, Restricted }

enum class AccessAction { None, RuntimePermission, AppSettings, LocationSettings, VpnConsent }

enum class AccessReason {
    None,
    MissingPermission,
    MissingHardware,
    NotificationsDisabled,
    LocationDisabled,
    VpnNotAuthorized,
    DevicePolicy,
    MissingManifestPermission,
    PlatformUnsupported
}

data class CapabilityState(
    val capability: Capability,
    val requirements: AccessRequirements,
    val status: AccessStatus,
    val action: AccessAction = AccessAction.None,
    val reason: AccessReason = AccessReason.None
)

/** Fresh OS observations. Never restore these as current grants from saved state or storage. */
data class PlatformAccessSnapshot(
    val apiLevel: Int,
    val grantedPermissions: Set<String>,
    val hardware: Set<Hardware>,
    val notificationsEnabled: Boolean,
    val locationEnabled: Boolean,
    val vpnPrepared: Boolean,
    val vpnRestricted: Boolean = false
)

/** Nonsensitive history, used to explain denial/revocation; never an authorization source. */
data class AccessHistory(
    val requestedPermissions: Set<String> = emptySet(),
    val settingsPermissions: Set<String> = emptySet(),
    val previouslyGranted: Set<Capability> = emptySet(),
    val vpnRequested: Boolean = false
)
