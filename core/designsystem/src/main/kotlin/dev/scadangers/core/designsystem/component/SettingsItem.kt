package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.scadangers.core.designsystem.theme.ScadangersIcons
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Settings row with a title, optional summary and icon.
 *
 * @param onClick makes the row a button and shows a chevron; `null` renders a read-only row.
 * @param value short current value shown at the end, such as "System default".
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    SettingsRow(modifier.then(interaction), title, summary, icon) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = ScadangersTheme.colors.textSecondary
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = ScadangersIcons.ChevronRight,
                contentDescription = null,
                tint = ScadangersTheme.colors.textTertiary,
                modifier = Modifier.size(ScadangersIcons.Size.medium)
            )
        }
    }
}

/**
 * Settings row with a switch. The whole row toggles and is announced as one switch.
 * When [enabled] is false, explain why in [summary].
 */
@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    SettingsRow(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        title = title,
        summary = summary,
        icon = icon
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ScadangersTheme.colors.accent,
                uncheckedBorderColor = ScadangersTheme.colors.controlBorder,
                uncheckedThumbColor = ScadangersTheme.colors.controlBorder,
                uncheckedTrackColor = ScadangersTheme.colors.surface
            )
        )
    }
}

/** Divider between consecutive settings rows inside a card. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = ScadangersSpacing.lg),
        thickness = ScadangersDimensions.border,
        color = ScadangersTheme.colors.border
    )
}

@Composable
private fun SettingsRow(
    modifier: Modifier,
    title: String,
    summary: String?,
    icon: ImageVector?,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ScadangersDimensions.listRowMinHeight)
            .padding(horizontal = ScadangersSpacing.lg, vertical = ScadangersSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ScadangersTheme.colors.textSecondary,
                modifier = Modifier.size(ScadangersIcons.Size.large)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ScadangersTheme.colors.textPrimary
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScadangersTheme.colors.textSecondary
                )
            }
        }
        trailing()
    }
}

@PreviewLightDark
@Composable
private fun SettingsItemPreview() {
    ScadangersPreviewSurface {
        ScadangersCard {
            SettingsItem(title = "Navigation item", summary = "Opens another screen", onClick = {})
            SettingsDivider()
            SettingsItem(title = "Read-only item", value = "Value", icon = ScadangersIcons.Info)
            SettingsDivider()
            SettingsToggleItem(title = "Enabled toggle", checked = true, onCheckedChange = {})
            SettingsDivider()
            SettingsToggleItem(
                title = "Unavailable toggle",
                summary = "Explains why this can't be changed",
                checked = false,
                onCheckedChange = {},
                enabled = false
            )
        }
    }
}
