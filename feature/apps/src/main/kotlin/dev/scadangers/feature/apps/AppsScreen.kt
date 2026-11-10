package dev.scadangers.feature.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.scadangers.core.designsystem.component.CardContent
import dev.scadangers.core.designsystem.component.ScadangersCard
import dev.scadangers.core.designsystem.component.ScreenLazyColumn
import dev.scadangers.core.designsystem.component.readableWidth

/** Stateless rendering of actual inventory; scanning and list preparation belong to the owner. */
@Composable
fun AppsScreen(
    state: AppsUiState = AppsUiState(),
    onScan: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {},
    onFilters: (AppFilters) -> Unit = {},
    onOpen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ScreenLazyColumn(modifier.testTag("installed-apps")) {
        item(key = "scan", contentType = "controls") {
            ScanControls(state, onScan, onCancel)
        }
        item(key = "filters", contentType = "controls") {
            Column(Modifier.readableWidth()) {
                OutlinedTextField(
                    value = state.filters.query,
                    onValueChange = { onFilters(state.filters.copy(query = it)) },
                    label = { Text(stringResource(R.string.inventory_search)) },
                    singleLine = true,
                    modifier = Modifier.readableWidth()
                )
                AppFilters(state.filters, onFilters)
            }
        }
        if (state.apps.isEmpty()) {
            item(key = "empty", contentType = "message") {
                Text(stringResource(R.string.inventory_empty), Modifier.readableWidth())
            }
        }
        items(state.apps, key = { "app:${it.packageName}" }, contentType = { "app" }) { app ->
            ScadangersCard(
                Modifier.readableWidth().clickable(
                    onClickLabel = stringResource(R.string.inventory_details),
                    role = Role.Button
                ) {
                    onOpen(app.packageName)
                }
            ) {
                CardContent {
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                    Text(app.packageName)
                    Text(app.summary)
                }
            }
        }
        items(state.inaccessible, key = {
            "unavailable:${it.label}"
        }, contentType = { "message" }) {
            Text("${it.label}: ${it.description}", Modifier.readableWidth())
        }
    }
}

@Composable
private fun ScanControls(state: AppsUiState, onScan: (Boolean) -> Unit, onCancel: () -> Unit) {
    ScadangersCard(Modifier.readableWidth()) {
        CardContent {
            Text(
                stringResource(R.string.inventory_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.inventory_scope))
            Text(state.lastScan ?: stringResource(R.string.inventory_never))
            if (state.summary.isNotEmpty()) Text(state.summary)
            if (state.busy) {
                Text(state.progress)
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.inventory_cancel))
                }
            } else {
                Button(onClick = { onScan(true) }, enabled = state.loaded) {
                    Text(stringResource(R.string.inventory_full_scan))
                }
                TextButton(onClick = { onScan(false) }, enabled = state.loaded) {
                    Text(stringResource(R.string.inventory_refresh))
                }
            }
            state.error?.let { Text(it) }
        }
    }
}

@Composable
private fun AppFilters(filters: AppFilters, onFilters: (AppFilters) -> Unit) {
    SelectionMenu(
        stringResource(R.string.inventory_sort),
        filters.sort,
        listOf("Name", "Review signals", "Recently installed", "Recently updated"),
        AppSort.entries
    ) { onFilters(filters.copy(sort = it)) }
    SelectionMenu(
        stringResource(R.string.inventory_type),
        filters.type,
        listOf("All apps", "User apps", "System apps"),
        AppTypeFilter.entries
    ) { onFilters(filters.copy(type = it)) }
    SelectionMenu(
        stringResource(R.string.inventory_risk),
        filters.risk,
        listOf("All signals", "Needs review"),
        AppRiskFilter.entries
    ) { onFilters(filters.copy(risk = it)) }
    SelectionMenu(
        stringResource(R.string.inventory_source),
        filters.source,
        listOf("All sources", "Expected store attribution", "Other source", "Unknown source"),
        AppSourceFilter.entries
    ) { onFilters(filters.copy(source = it)) }
}

@Composable
private fun <T : Enum<T>> SelectionMenu(
    label: String,
    selected: T,
    labels: List<String>,
    options: List<T>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) { Text("$label: ${labels[selected.ordinal]}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(labels[option.ordinal]) }, onClick = {
                    expanded = false
                    onSelect(option)
                })
            }
        }
    }
}
