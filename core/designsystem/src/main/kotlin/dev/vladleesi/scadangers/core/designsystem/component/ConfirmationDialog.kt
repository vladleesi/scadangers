package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersElevation
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersShapes
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Asks the user to confirm a consequential action. Name the action in [confirmLabel]
 * ("Remove", "Turn off") rather than "OK".
 *
 * @param destructive draws the confirm action in the critical role, for irreversible or
 * protection-reducing actions.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    val colors = ScadangersTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                message,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (destructive) colors.critical.content else colors.accent
                )
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
            ) { Text(dismissLabel) }
        },
        shape = ScadangersShapes.large,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        tonalElevation = ScadangersElevation.flat
    )
}

@PreviewLightDark
@Composable
private fun ConfirmationDialogPreview() {
    ScadangersPreviewSurface {
        ConfirmationDialog(
            title = "Remove example?",
            message = "Example confirmation message describing the consequence.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {},
            onDismiss = {},
            destructive = true
        )
    }
}
