package dev.scadangers.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.component.ScadangersPreviewSurface
import dev.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.scadangers.core.designsystem.component.SectionCard
import dev.scadangers.core.designsystem.component.SettingsDivider
import dev.scadangers.core.designsystem.component.SettingsItem
import dev.scadangers.core.designsystem.component.readableWidth

/**
 * Settings. The app supplies centralized access controls through a composable slot.
 *
 * @param appVersion version name of the installed build.
 */
@Composable
fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    accessContent: @Composable () -> Unit = {}
) {
    val systemDefault = stringResource(R.string.settings_system_default)
    ScreenLazyColumn(modifier) {
        item(key = "appearance") {
            SectionCard(
                stringResource(R.string.settings_appearance_heading),
                Modifier.readableWidth()
            ) {
                SettingsItem(
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(R.string.settings_theme_summary),
                    value = systemDefault
                )
                SettingsDivider()
                SettingsItem(
                    title = stringResource(R.string.settings_animations),
                    summary = stringResource(R.string.settings_animations_summary),
                    value = systemDefault
                )
            }
        }
        item(key = "privacy") {
            SectionCard(
                stringResource(R.string.settings_privacy_heading),
                Modifier.readableWidth()
            ) {
                SettingsItem(
                    title = stringResource(R.string.settings_data_collection),
                    summary = stringResource(R.string.settings_data_collection_summary),
                    value = stringResource(R.string.settings_data_collection_value)
                )
                SettingsDivider()
                SettingsItem(
                    title = stringResource(R.string.settings_backup),
                    summary = stringResource(R.string.settings_backup_summary),
                    value = stringResource(R.string.settings_backup_value)
                )
            }
        }
        item(key = "access") { accessContent() }
        item(key = "about") {
            SectionCard(stringResource(R.string.settings_about_heading), Modifier.readableWidth()) {
                SettingsItem(title = stringResource(R.string.settings_version), value = appVersion)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    ScadangersPreviewSurface { SettingsScreen(appVersion = "0.0.0-preview") }
}
