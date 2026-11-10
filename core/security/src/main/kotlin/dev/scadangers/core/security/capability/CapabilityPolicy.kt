package dev.scadangers.core.security.capability

/** Foreground-only contracts for targetSdk 37; no background location or monitoring consent. */
object CapabilityPolicy {
    fun requirements(capability: Capability, apiLevel: Int): AccessRequirements =
        when (capability) {
            Capability.InstalledApps -> AccessRequirements(
                limitedVisibility =
                    apiLevel >= AndroidApi.ANDROID_11
            )

            Capability.DeviceInformation -> AccessRequirements()

            Capability.Notifications -> if (apiLevel >= AndroidApi.ANDROID_13) {
                AccessRequirements(runtimePermissions = setOf(Permissions.NOTIFICATIONS))
            } else {
                AccessRequirements(specialAccess = SpecialAccess.NotificationSettings)
            }

            Capability.BluetoothScanning -> bluetooth(apiLevel)

            Capability.WifiScanning -> AccessRequirements(
                runtimePermissions = setOf(locationPermission(apiLevel)),
                manifestPermissions = setOf(Permissions.WIFI_STATE, Permissions.CHANGE_WIFI_STATE),
                hardware = Hardware.Wifi,
                locationServicesRequired = apiLevel >= AndroidApi.ANDROID_9
            )

            Capability.NearbyWifi -> AccessRequirements(
                runtimePermissions = setOf(
                    if (apiLevel >=
                        AndroidApi.ANDROID_13
                    ) {
                        Permissions.NEARBY_WIFI
                    } else {
                        locationPermission(apiLevel)
                    }
                ),
                manifestPermissions = setOf(Permissions.WIFI_STATE, Permissions.CHANGE_WIFI_STATE),
                hardware = Hardware.Wifi,
                locationServicesRequired = apiLevel < AndroidApi.ANDROID_13
            )

            Capability.LocalNetwork -> AccessRequirements(
                runtimePermissions = if (apiLevel >=
                    AndroidApi.ANDROID_17
                ) {
                    setOf(Permissions.LOCAL_NETWORK)
                } else {
                    emptySet()
                },
                manifestPermissions = setOf(Permissions.INTERNET)
            )

            Capability.VpnAuthorization -> AccessRequirements(
                specialAccess = SpecialAccess.VpnConsent
            )
        }

    private fun bluetooth(apiLevel: Int): AccessRequirements = if (apiLevel >=
        AndroidApi.ANDROID_12
    ) {
        AccessRequirements(
            runtimePermissions = setOf(Permissions.BLUETOOTH_SCAN),
            hardware = Hardware.BluetoothLe
        )
    } else {
        AccessRequirements(
            runtimePermissions = setOf(locationPermission(apiLevel)),
            manifestPermissions = setOf(Permissions.BLUETOOTH, Permissions.BLUETOOTH_ADMIN),
            hardware = Hardware.BluetoothLe,
            locationServicesRequired = true
        )
    }

    private fun locationPermission(apiLevel: Int): String = if (apiLevel >=
        AndroidApi.ANDROID_10
    ) {
        Permissions.FINE_LOCATION
    } else {
        Permissions.COARSE_LOCATION
    }
}
