package dev.scadangers.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.scadangers.R
import dev.scadangers.core.designsystem.component.CardContent
import dev.scadangers.core.designsystem.component.SectionCard
import dev.scadangers.core.designsystem.component.readableWidth
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme
import dev.scadangers.core.security.capability.AccessAction
import dev.scadangers.core.security.capability.AccessReason
import dev.scadangers.core.security.capability.AccessStatus
import dev.scadangers.core.security.capability.Capability
import dev.scadangers.core.security.capability.CapabilityState

@Composable
internal fun CapabilityCards(
    capabilities: List<Capability>,
    state: AccessUiState,
    onExplain: (Capability) -> Unit
) {
    Column(
        Modifier.readableWidth(),
        verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg)
    ) {
        state.failure?.let { AccessError(it) }
        capabilities.forEach { capability ->
            val access = state.capabilities.firstOrNull { it.capability == capability }
            val copy = capability.copyResources()
            SectionCard(
                stringResource(copy.title),
                Modifier.testTag("capability_${capability.name}")
            ) {
                CardContent {
                    Text(
                        stringResource(copy.rationale),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(access.statusResource()),
                        style = MaterialTheme.typography.labelLarge,
                        color = ScadangersTheme.colors.textSecondary
                    )
                    if (access != null && access.reason != AccessReason.None) {
                        Text(
                            stringResource(access.reasonResource()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (access != null && access.action != AccessAction.None) {
                        TextButton(onClick = { onExplain(capability) }, enabled = !state.busy) {
                            Text(stringResource(R.string.access_review))
                        }
                    }
                }
            }
        }
    }
}

internal fun CapabilityState?.statusResource(): Int = when (this?.status) {
    null, AccessStatus.Unknown -> R.string.access_unknown

    AccessStatus.NotRequested -> R.string.access_not_requested

    AccessStatus.Granted -> if (requirements.limitedVisibility) {
        R.string.access_limited
    } else {
        R.string.access_granted
    }

    AccessStatus.Denied -> R.string.access_denied

    AccessStatus.Revoked -> R.string.access_revoked

    AccessStatus.Unsupported -> R.string.access_unsupported

    AccessStatus.Restricted -> R.string.access_restricted
}

internal fun CapabilityState.reasonResource(): Int = when (reason) {
    AccessReason.None -> R.string.access_no_engine

    AccessReason.MissingPermission -> if (action ==
        AccessAction.AppSettings
    ) {
        R.string.access_settings_recovery
    } else {
        R.string.access_runtime_needed
    }

    AccessReason.MissingHardware -> R.string.access_hardware_missing

    AccessReason.NotificationsDisabled -> R.string.access_notifications_disabled

    AccessReason.LocationDisabled -> R.string.access_location_disabled

    AccessReason.VpnNotAuthorized -> R.string.access_vpn_needed

    AccessReason.DevicePolicy -> R.string.access_policy_restricted

    AccessReason.MissingManifestPermission -> R.string.access_configuration_missing

    AccessReason.PlatformUnsupported -> R.string.access_platform_unsupported
}

@Composable
internal fun AccessError(failure: AccessFailure) {
    val message = when (failure) {
        AccessFailure.Storage -> R.string.access_storage_error
        AccessFailure.Platform -> R.string.access_platform_error
        AccessFailure.SystemScreenUnavailable -> R.string.access_system_screen_error
    }
    Text(stringResource(message), color = ScadangersTheme.colors.caution.content)
}
