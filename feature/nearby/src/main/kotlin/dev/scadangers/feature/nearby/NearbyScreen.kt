package dev.scadangers.feature.nearby

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.component.EmptyState
import dev.scadangers.core.designsystem.component.ScadangersCard
import dev.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.scadangers.core.designsystem.component.readableWidth
import dev.scadangers.core.designsystem.theme.ScadangersIcons

/** Nearby destination. Discovery is not implemented; the app supplies optional access controls. */
@Composable
fun NearbyScreen(modifier: Modifier = Modifier, accessContent: @Composable () -> Unit = {}) {
    ScreenLazyColumn(modifier) {
        item(key = "unavailable") {
            ScadangersCard(Modifier.readableWidth()) {
                EmptyState(
                    title = stringResource(R.string.nearby_unavailable_title),
                    body = stringResource(R.string.nearby_unavailable_body),
                    icon = ScadangersIcons.Nearby
                )
            }
        }
        item(key = "access") { accessContent() }
    }
}

@PreviewLightDark
@Composable
private fun NearbyScreenPreview() {
    ScadangersPreviewSurface { NearbyScreen() }
}
