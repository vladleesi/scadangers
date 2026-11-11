package dev.vladleesi.scadangers.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.vladleesi.scadangers.core.designsystem.adaptive.LocalWindowWidthClass
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersDimensions
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing

/**
 * Scrollable single-column screen body with gutters for the current window width class.
 * Give every item [Modifier.readableWidth] and a stable `key`. The app scaffold owns the
 * background.
 */
@Composable
fun ScreenLazyColumn(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    val gutter = LocalWindowWidthClass.current.gutter
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = gutter, vertical = ScadangersSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/** Fills the available width up to a comfortable reading width. */
fun Modifier.readableWidth(): Modifier =
    widthIn(max = ScadangersDimensions.maxReadableWidth).fillMaxWidth()
