package dev.scadangers.core.security.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityPolicyTest {
    @Test
    fun bluetoothTransitionsFromLocationToScanPermissionAt31() {
        (24..37).forEach { api ->
            val requirements = CapabilityPolicy.requirements(Capability.BluetoothScanning, api)
            val expected = when {
                api >= 31 -> Permissions.BLUETOOTH_SCAN
                api >= 29 -> Permissions.FINE_LOCATION
                else -> Permissions.COARSE_LOCATION
            }
            assertEquals("API $api", setOf(expected), requirements.runtimePermissions)
            assertEquals(api < 31, requirements.locationServicesRequired)
            assertEquals(
                if (api <
                    31
                ) {
                    setOf(Permissions.BLUETOOTH, Permissions.BLUETOOTH_ADMIN)
                } else {
                    emptySet<String>()
                },
                requirements.manifestPermissions
            )
        }
    }

    @Test
    fun wifiScansAlwaysRequireLocationRegardlessOfNearbyWifiGrant() {
        (24..37).forEach { api ->
            val requirements = CapabilityPolicy.requirements(Capability.WifiScanning, api)
            assertEquals(
                setOf(if (api >= 29) Permissions.FINE_LOCATION else Permissions.COARSE_LOCATION),
                requirements.runtimePermissions
            )
            assertFalse(Permissions.NEARBY_WIFI in requirements.runtimePermissions)
            assertEquals(api >= 28, requirements.locationServicesRequired)
            assertEquals(
                setOf(Permissions.WIFI_STATE, Permissions.CHANGE_WIFI_STATE),
                requirements.manifestPermissions
            )
            assertEquals(
                api >= 31,
                Permissions.COARSE_LOCATION in requirements.requestPermissions(api) && api >= 29
            )
        }
    }

    @Test
    fun nearbyWifiAndNotificationsChangeAt33() {
        (24..37).forEach { api ->
            val wifi = CapabilityPolicy.requirements(Capability.NearbyWifi, api)
            assertEquals(api >= 33, Permissions.NEARBY_WIFI in wifi.runtimePermissions)
            val notifications = CapabilityPolicy.requirements(Capability.Notifications, api)
            assertEquals(
                if (api >=
                    33
                ) {
                    setOf(Permissions.NOTIFICATIONS)
                } else {
                    emptySet<String>()
                },
                notifications.runtimePermissions
            )
            assertEquals(
                if (api <
                    33
                ) {
                    SpecialAccess.NotificationSettings
                } else {
                    null
                },
                notifications.specialAccess
            )
        }
    }

    @Test
    fun localNetworkUsesDedicatedPermissionOnlyAt37() {
        (24..37).forEach { api ->
            val requirements = CapabilityPolicy.requirements(Capability.LocalNetwork, api)
            assertEquals(
                if (api >=
                    37
                ) {
                    setOf(Permissions.LOCAL_NETWORK)
                } else {
                    emptySet<String>()
                },
                requirements.runtimePermissions
            )
            assertEquals(setOf(Permissions.INTERNET), requirements.manifestPermissions)
        }
    }

    @Test
    fun packageVisibilityIsLimitedFrom30AndCannotBeRequested() {
        (24..37).forEach { api ->
            val apps = CapabilityPolicy.requirements(Capability.InstalledApps, api)
            assertTrue(apps.runtimePermissions.isEmpty())
            assertEquals(null, apps.specialAccess)
            assertEquals(api >= 30, apps.limitedVisibility)
            assertEquals(
                AccessRequirements(),
                CapabilityPolicy.requirements(Capability.DeviceInformation, api)
            )
            val vpn = CapabilityPolicy.requirements(Capability.VpnAuthorization, api)
            assertTrue(vpn.runtimePermissions.isEmpty())
            assertEquals(SpecialAccess.VpnConsent, vpn.specialAccess)
        }
    }
}
