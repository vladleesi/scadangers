package dev.vladleesi.scadangers.core.security.capability

import org.junit.Assert.assertEquals
import org.junit.Test

internal fun snapshot(apiLevel: Int = 37) = PlatformAccessSnapshot(
    apiLevel = apiLevel,
    grantedPermissions = setOf(
        Permissions.INTERNET,
        Permissions.WIFI_STATE,
        Permissions.CHANGE_WIFI_STATE,
        Permissions.BLUETOOTH,
        Permissions.BLUETOOTH_ADMIN
    ),
    hardware = Hardware.entries.toSet(),
    notificationsEnabled = true,
    locationEnabled = true,
    vpnPrepared = false
)

class CapabilityStateMachineTest {
    @Test
    fun initialCheckDoesNotConfuseNoRationaleWithPermanentDenial() {
        val state = evaluate(Capability.Notifications)
        assertEquals(AccessStatus.NotRequested, state.status)
        assertEquals(AccessAction.RuntimePermission, state.action)
    }

    @Test
    fun deniedPermissionCanBeRequestedAgainAndPermanentDenialOffersSettings() {
        val history = AccessHistory(requestedPermissions = setOf(Permissions.NOTIFICATIONS))
        val denied = evaluate(Capability.Notifications, history = history)
        assertEquals(AccessStatus.Denied, denied.status)
        assertEquals(AccessAction.RuntimePermission, denied.action)
        val blocked =
            evaluate(
                Capability.Notifications,
                history = history.copy(settingsPermissions = history.requestedPermissions)
            )
        assertEquals(AccessAction.AppSettings, blocked.action)
    }

    @Test
    fun previouslyGrantedPermissionIsRevokedAndCanBeRequestedAfterAutoReset() {
        val history = AccessHistory(previouslyGranted = setOf(Capability.BluetoothScanning))
        val state = evaluate(Capability.BluetoothScanning, history = history)
        assertEquals(AccessStatus.Revoked, state.status)
        assertEquals(AccessAction.RuntimePermission, state.action)
    }

    @Test
    fun approximateLocationCannotAuthorizeWifiScanning() {
        val platform = snapshot().copy(
            grantedPermissions =
                snapshot().grantedPermissions + Permissions.NEARBY_WIFI +
                    Permissions.COARSE_LOCATION
        )
        assertEquals(AccessStatus.Granted, evaluate(Capability.NearbyWifi, platform).status)
        assertEquals(AccessStatus.NotRequested, evaluate(Capability.WifiScanning, platform).status)
    }

    @Test
    fun hardwareIsRequiredDespiteGrantedPermissions() {
        val platform = snapshot().copy(
            hardware = emptySet(),
            grantedPermissions =
                snapshot().grantedPermissions + Permissions.BLUETOOTH_SCAN +
                    Permissions.FINE_LOCATION
        )
        listOf(
            Capability.BluetoothScanning,
            Capability.WifiScanning,
            Capability.NearbyWifi
        ).forEach {
            val state = evaluate(it, platform)
            assertEquals(AccessStatus.Unsupported, state.status)
            assertEquals(AccessAction.None, state.action)
        }
        assertEquals(AccessStatus.Granted, evaluate(Capability.DeviceInformation, platform).status)
        assertEquals(
            AccessStatus.Granted,
            evaluate(Capability.LocalNetwork, platform.copy(apiLevel = 36)).status
        )
    }

    @Test
    fun locationServicesAreAnIndependentSettingsRequirement() {
        val platform = snapshot().copy(
            locationEnabled = false,
            grantedPermissions =
                snapshot().grantedPermissions + Permissions.FINE_LOCATION
        )
        val state = evaluate(Capability.WifiScanning, platform)
        assertEquals(AccessAction.LocationSettings, state.action)
        assertEquals(AccessReason.LocationDisabled, state.reason)
    }

    @Test
    fun notificationSettingsAreCheckedOnAllSupportedVersions() {
        (24..37).forEach { api ->
            val platform = snapshot(api).copy(
                notificationsEnabled = false,
                grantedPermissions =
                    snapshot(api).grantedPermissions + Permissions.NOTIFICATIONS
            )
            val state =
                evaluate(
                    Capability.Notifications,
                    platform,
                    AccessHistory(previouslyGranted = setOf(Capability.Notifications))
                )
            assertEquals(AccessStatus.Revoked, state.status)
            assertEquals(AccessAction.AppSettings, state.action)
        }
    }

    @Test
    fun vpnRequiresRealPlatformConsentAndHonorsProfileRestrictions() {
        assertEquals(AccessAction.VpnConsent, evaluate(Capability.VpnAuthorization).action)
        assertEquals(
            AccessStatus.Denied,
            evaluate(
                Capability.VpnAuthorization,
                history = AccessHistory(vpnRequested = true)
            ).status
        )
        assertEquals(
            AccessStatus.Granted,
            evaluate(Capability.VpnAuthorization, snapshot().copy(vpnPrepared = true)).status
        )
        val restricted =
            evaluate(
                Capability.VpnAuthorization,
                snapshot().copy(vpnPrepared = true, vpnRestricted = true)
            )
        assertEquals(AccessStatus.Restricted, restricted.status)
        assertEquals(AccessAction.None, restricted.action)
        assertEquals(
            AccessStatus.Revoked,
            evaluate(
                Capability.VpnAuthorization,
                history = AccessHistory(previouslyGranted = setOf(Capability.VpnAuthorization))
            ).status
        )
    }

    @Test
    fun missingNormalPermissionNeverReportsAccessGranted() {
        val state =
            evaluate(Capability.LocalNetwork, snapshot(36).copy(grantedPermissions = emptySet()))
        assertEquals(AccessStatus.Restricted, state.status)
        assertEquals(AccessReason.MissingManifestPermission, state.reason)
    }

    private fun evaluate(
        capability: Capability,
        platform: PlatformAccessSnapshot = snapshot(),
        history: AccessHistory = AccessHistory()
    ) = CapabilityStateMachine.evaluate(capability, platform, history)
}
