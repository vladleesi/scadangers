package dev.scadangers.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.scadangers.core.designsystem.adaptive.ProvideWindowWidthClass
import dev.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.scadangers.core.designsystem.theme.ScadangersTheme

/**
 * Wraps `@Preview` content in the theme and app background. For previews only; preview
 * functions must use clearly synthetic text, never data that resembles a real finding.
 */
@Composable
fun ScadangersPreviewSurface(content: @Composable () -> Unit) {
    ScadangersTheme {
        Box(Modifier.background(ScadangersTheme.colors.background).padding(ScadangersSpacing.lg)) {
            ProvideWindowWidthClass(content)
        }
    }
}
