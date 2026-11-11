package dev.vladleesi.scadangers.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dev.vladleesi.scadangers.core.security.device.DeviceSignal
import dev.vladleesi.scadangers.core.security.device.NetworkObservation
import dev.vladleesi.scadangers.core.security.device.SignalState

/** No SSID, IP address, DNS server name, proxy address or traffic content is retained. */
internal fun readNetwork(context: Context): NetworkObservation = try {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val network = manager?.activeNetwork
    if (manager == null) {
        unknownNetwork("Connectivity service unavailable")
    } else if (network == null) {
        unknownNetwork("No default network at assessment time").copy(
            connected = DeviceSignal(SignalState.Disabled, "Default network: none")
        )
    } else {
        val capabilities = manager.getNetworkCapabilities(network)
        val links = manager.getLinkProperties(network)
        val snapshot = NetworkObservation(
            connected = DeviceSignal(SignalState.Enabled, "Default network: present"),
            privateDns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                observeDeviceSignal("LinkProperties.isPrivateDnsActive") {
                    links?.isPrivateDnsActive
                }
            } else {
                DeviceSignal(SignalState.Unsupported, "Private DNS indicator requires API 28")
            },
            captivePortal = observeDeviceSignal("Captive portal capability") {
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            },
            vpn = observeDeviceSignal("VPN transport on app default network") {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            },
            proxy = observeDeviceSignal("HTTP proxy configured on app default network") {
                links?.let { it.httpProxy != null }
            }
        )
        if (manager.activeNetwork ==
            network
        ) {
            snapshot
        } else {
            unknownNetwork("Default network changed during collection; reassess")
        }
    }
} catch (_: SecurityException) {
    unknownNetwork("Network state access restricted")
} catch (_: IllegalStateException) {
    unknownNetwork("Network service unavailable")
} catch (_: UnsupportedOperationException) {
    unknownNetwork("Network state API unavailable")
}

private fun unknownNetwork(reason: String): NetworkObservation {
    val unknown = DeviceSignal(SignalState.Unknown, reason)
    return NetworkObservation(unknown, unknown, unknown, unknown, unknown)
}
