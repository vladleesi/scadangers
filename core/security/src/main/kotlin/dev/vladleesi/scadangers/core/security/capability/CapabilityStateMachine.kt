package dev.vladleesi.scadangers.core.security.capability

/** A deterministic reducer: UI receives states/actions, never performs permission checks. */
object CapabilityStateMachine {
    fun evaluate(
        capability: Capability,
        snapshot: PlatformAccessSnapshot,
        history: AccessHistory
    ): CapabilityState {
        val requirements = CapabilityPolicy.requirements(capability, snapshot.apiLevel)
        val initial = CapabilityState(capability, requirements, AccessStatus.Granted)
        return when {
            snapshot.apiLevel < AndroidApi.ANDROID_7 -> initial.copy(
                status = AccessStatus.Unsupported,
                reason = AccessReason.PlatformUnsupported
            )

            requirements.hardware != null && requirements.hardware !in snapshot.hardware ->
                initial.copy(
                    status = AccessStatus.Unsupported,
                    reason = AccessReason.MissingHardware
                )

            !snapshot.grantedPermissions.containsAll(requirements.manifestPermissions) ->
                initial.copy(
                    status = AccessStatus.Restricted,
                    reason = AccessReason.MissingManifestPermission
                )

            capability == Capability.VpnAuthorization && snapshot.vpnRestricted ->
                initial.copy(status = AccessStatus.Restricted, reason = AccessReason.DevicePolicy)

            else -> evaluateAccess(initial, snapshot, history)
        }
    }

    private fun evaluateAccess(
        initial: CapabilityState,
        snapshot: PlatformAccessSnapshot,
        history: AccessHistory
    ): CapabilityState {
        val missing = initial.requirements.runtimePermissions - snapshot.grantedPermissions
        val status = deniedStatus(initial.capability, missing, history)
        return when {
            missing.isNotEmpty() -> initial.copy(
                status = status,
                action = if (missing.any { it in history.settingsPermissions }) {
                    AccessAction.AppSettings
                } else {
                    AccessAction.RuntimePermission
                },
                reason = AccessReason.MissingPermission
            )

            initial.capability == Capability.Notifications && !snapshot.notificationsEnabled ->
                initial.copy(
                    status = if (status == AccessStatus.Revoked) status else AccessStatus.Denied,
                    action = AccessAction.AppSettings,
                    reason = AccessReason.NotificationsDisabled
                )

            initial.capability == Capability.VpnAuthorization && !snapshot.vpnPrepared ->
                initial.copy(
                    status = status,
                    action = AccessAction.VpnConsent,
                    reason = AccessReason.VpnNotAuthorized
                )

            initial.requirements.locationServicesRequired && !snapshot.locationEnabled ->
                initial.copy(
                    status = if (status == AccessStatus.Revoked) status else AccessStatus.Denied,
                    action = AccessAction.LocationSettings,
                    reason = AccessReason.LocationDisabled
                )

            else -> initial
        }
    }

    private fun deniedStatus(
        capability: Capability,
        missingPermissions: Set<String>,
        history: AccessHistory
    ): AccessStatus = when {
        capability in history.previouslyGranted -> AccessStatus.Revoked
        missingPermissions.any { it in history.requestedPermissions } -> AccessStatus.Denied
        capability == Capability.VpnAuthorization && history.vpnRequested -> AccessStatus.Denied
        else -> AccessStatus.NotRequested
    }
}
