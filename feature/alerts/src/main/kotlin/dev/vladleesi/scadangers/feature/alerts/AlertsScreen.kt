package dev.vladleesi.scadangers.feature.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.component.EmptyState
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersCard
import dev.vladleesi.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.vladleesi.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.vladleesi.scadangers.core.designsystem.component.readableWidth
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersIcons

/** Alerts destination. No alert source exists yet, so the list is explicitly empty. */
@Composable
fun AlertsScreen(modifier: Modifier = Modifier, accessContent: @Composable () -> Unit = {}) {
    ScreenLazyColumn(modifier) {
        item(key = "empty") {
            ScadangersCard(Modifier.readableWidth()) {
                EmptyState(
                    title = stringResource(R.string.alerts_empty_title),
                    body = stringResource(R.string.alerts_empty_body),
                    icon = ScadangersIcons.Alerts
                )
            }
        }
        item(key = "access") { accessContent() }
    }
}

@PreviewLightDark
@Composable
private fun AlertsScreenPreview() {
    ScadangersPreviewSurface { AlertsScreen() }
}
