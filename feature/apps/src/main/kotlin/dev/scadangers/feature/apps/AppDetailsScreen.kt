package dev.scadangers.feature.apps

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.scadangers.core.designsystem.component.CardContent
import dev.scadangers.core.designsystem.component.ScadangersCard
import dev.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.scadangers.core.designsystem.component.readableWidth
import dev.scadangers.core.security.apps.AppFinding
import dev.scadangers.core.security.apps.ReviewLevel

@Composable
fun AppDetailsScreen(
    details: AppDetailsUi,
    error: String?,
    onBack: () -> Unit,
    onSettings: (AppSettingsAction) -> Unit
) {
    ScreenLazyColumn {
        item(key = "header", contentType = "header") {
            AppDetailHeader(details, error, onBack)
        }
        items(details.metadata, key = { "metadata:${it.label}" }, contentType = { "line" }) {
            Text("${it.label}\n${it.description}", Modifier.readableWidth())
        }
        item(key = "findings-header", contentType = "heading") {
            Text(
                stringResource(R.string.inventory_findings),
                Modifier.readableWidth(),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(details.findings, key = {
            "finding:${it.id}"
        }, contentType = { "finding" }) { finding ->
            AppFindingCard(finding)
        }
        item(key = "permission-header", contentType = "heading") {
            Text(
                stringResource(R.string.inventory_permissions),
                Modifier.readableWidth(),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(details.permissions, key = { "permission:${it.label}" }, contentType = { "line" }) {
            Text("${it.label}\n${it.description}", Modifier.readableWidth())
        }
        item(key = "apk-header", contentType = "heading") {
            Text(
                stringResource(R.string.inventory_apks),
                Modifier.readableWidth(),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(details.apks, key = {
            "apk:$it"
        }, contentType = { "line" }) { Text(it, Modifier.readableWidth()) }
        item(key = "settings", contentType = "controls") {
            CardContent(Modifier.readableWidth()) {
                Text(
                    stringResource(R.string.inventory_system_settings),
                    style = MaterialTheme.typography.titleMedium
                )
                AppSettingsAction.entries.forEach { action ->
                    TextButton(onClick = { onSettings(action) }) { Text(settingsLabel(action)) }
                }
            }
        }
    }
}

@Composable
private fun AppFindingCard(finding: AppFinding) {
    ScadangersCard(Modifier.readableWidth()) {
        CardContent {
            Text(finding.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    if (finding.level >= ReviewLevel.Review) {
                        R.string.inventory_review_signal
                    } else {
                        R.string.inventory_information
                    }
                )
            )
            Text(finding.explanation)
        }
    }
}

@Composable
private fun AppDetailHeader(details: AppDetailsUi, error: String?, onBack: () -> Unit) {
    CardContent(Modifier.readableWidth()) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.inventory_back)) }
        Text(details.label, style = MaterialTheme.typography.headlineSmall)
        Text(details.packageName)
        Text(stringResource(R.string.inventory_details_scope))
        error?.let { Text(it) }
    }
}

@Composable
private fun settingsLabel(action: AppSettingsAction): String = stringResource(
    when (action) {
        AppSettingsAction.Details -> R.string.inventory_app_settings
        AppSettingsAction.Overlay -> R.string.inventory_overlay
        AppSettingsAction.InstallSources -> R.string.inventory_install_sources
        AppSettingsAction.WriteSettings -> R.string.inventory_write_settings
        AppSettingsAction.Usage -> R.string.inventory_usage
        AppSettingsAction.Accessibility -> R.string.inventory_accessibility
        AppSettingsAction.Notifications -> R.string.inventory_notifications
        AppSettingsAction.Administrators -> R.string.inventory_administrators
        AppSettingsAction.AllFiles -> R.string.inventory_all_files
    }
)
