package dev.vladleesi.scadangers.access

import dev.vladleesi.scadangers.R
import dev.vladleesi.scadangers.core.security.capability.Capability

internal data class CapabilityCopy(val title: Int, val rationale: Int)

internal fun Capability.copyResources(): CapabilityCopy = when (this) {
    Capability.InstalledApps -> CapabilityCopy(R.string.access_apps, R.string.access_apps_reason)

    Capability.DeviceInformation -> CapabilityCopy(
        R.string.access_device,
        R.string.access_device_reason
    )

    Capability.Notifications -> CapabilityCopy(
        R.string.access_notifications,
        R.string.access_notifications_reason
    )

    Capability.BluetoothScanning -> CapabilityCopy(
        R.string.access_bluetooth,
        R.string.access_bluetooth_reason
    )

    Capability.WifiScanning -> CapabilityCopy(R.string.access_wifi, R.string.access_wifi_reason)

    Capability.NearbyWifi -> CapabilityCopy(
        R.string.access_nearby_wifi,
        R.string.access_nearby_wifi_reason
    )

    Capability.LocalNetwork -> CapabilityCopy(R.string.access_lan, R.string.access_lan_reason)

    Capability.VpnAuthorization -> CapabilityCopy(R.string.access_vpn, R.string.access_vpn_reason)
}
