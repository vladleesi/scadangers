package dev.scadangers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.scadangers.access.AccessRoot
import dev.scadangers.access.AccessViewModel
import dev.scadangers.access.SystemAccessLauncher
import dev.scadangers.core.designsystem.theme.ScadangersTheme
import dev.scadangers.scanner.AppsRoot
import dev.scadangers.scanner.AppsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val accessModel: AccessViewModel by viewModels()

    private val appsModel: AppsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SystemAccessLauncher(this, accessModel).observe()
        setContent {
            ScadangersTheme(reducedMotion = rememberReducedMotion()) {
                val accessState by accessModel.state.collectAsStateWithLifecycle()
                AccessRoot(accessState, accessModel) { AppsRoot(appsModel) }
            }
        }
    }
}
