package dev.vladleesi.scadangers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.vladleesi.scadangers.access.AccessRoot
import dev.vladleesi.scadangers.access.AccessViewModel
import dev.vladleesi.scadangers.access.SystemAccessLauncher
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.device.DeviceRoot
import dev.vladleesi.scadangers.device.DeviceViewModel
import dev.vladleesi.scadangers.scanner.AppsRoot
import dev.vladleesi.scadangers.scanner.AppsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val accessModel: AccessViewModel by viewModels()

    private val appsModel: AppsViewModel by viewModels()
    private val deviceModel: DeviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SystemAccessLauncher(this, accessModel).observe()
        setContent {
            ScadangersTheme(reducedMotion = rememberReducedMotion()) {
                val accessState by accessModel.state.collectAsStateWithLifecycle()
                AccessRoot(
                    accessState,
                    accessModel,
                    appsContent = { AppsRoot(appsModel) },
                    deviceContent = { DeviceRoot(deviceModel, readDeviceProfile()) }
                )
            }
        }
    }
}
