package dev.vladleesi.scadangers.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.scadangers.core.common.device.DeviceProfile
import dev.vladleesi.scadangers.feature.device.DeviceScreen
import kotlinx.coroutines.launch

@Composable
internal fun DeviceRoot(model: DeviceViewModel, profile: DeviceProfile) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(model) {
        // Returning from settings always replaces the earlier point-in-time observations.
        model.assess()
        onPauseOrDispose { model.cancel() }
    }
    DeviceScreen(
        profile,
        state = state,
        onAssess = model::assess,
        onCancel = model::cancel
    ) { action ->
        scope.launch {
            if (!openDeviceSettings(context, action)) model.settingsUnavailable()
        }
    }
}
