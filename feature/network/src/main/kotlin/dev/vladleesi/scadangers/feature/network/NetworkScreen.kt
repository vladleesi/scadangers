package dev.vladleesi.scadangers.feature.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.component.EmptyState
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersCard
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.vladleesi.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.vladleesi.scadangers.core.designsystem.component.SectionCard
import dev.vladleesi.scadangers.core.designsystem.component.SettingsToggleItem
import dev.vladleesi.scadangers.core.designsystem.component.readableWidth
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons

/** Network destination: protection control plus monitoring, both unavailable in this version. */
@Composable
fun NetworkScreen(modifier: Modifier = Modifier, accessContent: @Composable () -> Unit = {}) {
    ScreenLazyColumn(modifier) {
        item(key = "protection") { NetworkProtectionCard(Modifier.readableWidth()) }
        item(key = "monitoring") {
            ScadangersCard(Modifier.readableWidth()) {
                EmptyState(
                    title = stringResource(R.string.network_monitoring_title),
                    body = stringResource(R.string.network_monitoring_body),
                    icon = ScadangersIcons.Network
                )
            }
        }
        item(key = "access") { accessContent() }
    }
}

/**
 * Network protection on/off control. Network protection is not implemented, so the switch is
 * off and disabled with an explanation; it never claims traffic is protected.
 */
@Composable
fun NetworkProtectionCard(modifier: Modifier = Modifier) {
    SectionCard(title = stringResource(R.string.network_protection_heading), modifier = modifier) {
        SettingsToggleItem(
            title = stringResource(R.string.network_protection_toggle),
            summary = stringResource(R.string.network_protection_unavailable),
            checked = false,
            onCheckedChange = {},
            enabled = false
        )
    }
}

@PreviewLightDark
@Composable
private fun NetworkScreenPreview() {
    ScadangersPreviewSurface { NetworkScreen() }
}
