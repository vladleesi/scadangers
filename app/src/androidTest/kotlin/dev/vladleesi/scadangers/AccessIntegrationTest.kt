package dev.vladleesi.scadangers

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladleesi.scadangers.access.AccessRoot
import dev.vladleesi.scadangers.access.AccessViewModel
import dev.vladleesi.scadangers.access.AndroidAccessSource
import dev.vladleesi.scadangers.access.AndroidAccessStore
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.core.security.capability.AccessHistory
import dev.vladleesi.scadangers.core.security.capability.AccessPreferences
import dev.vladleesi.scadangers.core.security.capability.AccessRepository
import dev.vladleesi.scadangers.core.security.capability.AccessStore
import dev.vladleesi.scadangers.core.security.capability.Capability
import dev.vladleesi.scadangers.core.security.capability.Hardware
import dev.vladleesi.scadangers.core.security.capability.OnboardingStep
import dev.vladleesi.scadangers.core.security.capability.Permissions
import dev.vladleesi.scadangers.core.security.capability.PlatformAccessSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccessIntegrationTest {
    @get:Rule
    val compose = createComposeRule()

    private val modelStore = ViewModelStore()
    private val store = IntegrationAccessStore()
    private lateinit var model: AccessViewModel

    @Before
    fun setup() {
        compose.runOnIdle {
            model = ViewModelProvider(
                modelStore,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val snapshot = PlatformAccessSnapshot(
                            Build.VERSION.SDK_INT,
                            setOf(
                                Permissions.INTERNET,
                                Permissions.WIFI_STATE,
                                Permissions.CHANGE_WIFI_STATE
                            ),
                            Hardware.entries.toSet(),
                            false,
                            true,
                            false
                        )
                        // This test factory supplies only AccessViewModel.
                        @Suppress("UNCHECKED_CAST")
                        return AccessViewModel(
                            AccessRepository({
                                snapshot
                            }, store),
                            SavedStateHandle()
                        ) as T
                    }
                }
            )[AccessViewModel::class.java]
        }
    }

    @After
    fun cleanup() = compose.runOnIdle { modelStore.clear() }

    @Test
    fun optionalAccessCanBeSkippedWithoutAnyPermissionRequest() {
        showApp()
        compose.onNodeWithText("Skip optional access").performScrollTo().performClick()
        compose.onNodeWithText("Protection is not active").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(OnboardingStep.Complete, store.preferences.onboardingStep)
            assertEquals(emptySet<String>(), store.preferences.history.requestedPermissions)
            assertFalse(store.preferences.history.vpnRequested)
        }
    }

    @Test
    fun contextualRationaleCanBeDismissedAndOtherScreensRemainUsable() {
        showApp()
        compose.onNodeWithText("Skip optional access").performScrollTo().performClick()
        compose.onNodeWithText("Alerts").performClick()
        compose.onNodeWithText("Review access").performScrollTo().performClick()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Apps").performClick()
        compose.onNodeWithText("Installed app inventory").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(emptySet<String>(), store.preferences.history.requestedPermissions)
        }
    }

    @Test
    fun onboardingAndRationaleRemainUsableAtLargeFontScale() {
        showApp(fontScale = 2f)
        compose.onNodeWithText("Skip optional access").performScrollTo().performClick()
        compose.onNodeWithText("Alerts").performClick()
        compose.onNodeWithText("Review access").performScrollTo().performClick()
        compose.onNodeWithText("Not now").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(emptySet<String>(), store.preferences.history.requestedPermissions)
        }
    }

    @Test
    fun datastoreRoundTripRestoresOnboardingAndDenialHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = AndroidAccessStore(context)
        val original = first.read()
        try {
            first.update {
                AccessPreferences(
                    OnboardingStep.Choices,
                    AccessHistory(
                        requestedPermissions = setOf(Permissions.FINE_LOCATION),
                        settingsPermissions = setOf(Permissions.FINE_LOCATION),
                        previouslyGranted = setOf(Capability.WifiScanning)
                    )
                )
            }
            val restored = AndroidAccessStore(context).read()
            assertEquals(OnboardingStep.Choices, restored.onboardingStep)
            assertEquals(setOf(Permissions.FINE_LOCATION), restored.history.settingsPermissions)
            assertEquals(setOf(Capability.WifiScanning), restored.history.previouslyGranted)
        } finally {
            first.update { original }
        }
    }

    @Test
    fun androidSnapshotAgreesWithActualPermissionsAndHardware() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val snapshot = AndroidAccessSource(context).read()
        assertEquals(Build.VERSION.SDK_INT, snapshot.apiLevel)
        assertEquals(
            context.checkSelfPermission(Permissions.FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
            Permissions.FINE_LOCATION in snapshot.grantedPermissions
        )
        assertEquals(
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            Hardware.BluetoothLe in snapshot.hardware
        )
        assertEquals(
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI),
            Hardware.Wifi in snapshot.hardware
        )
    }

    private fun showApp(fontScale: Float = 1f) {
        compose.setContent {
            val state by model.state.collectAsStateWithLifecycle()
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                ScadangersTheme { AccessRoot(state, model) }
            }
        }
    }
}

private class IntegrationAccessStore : AccessStore {
    var preferences = AccessPreferences()
    override suspend fun read() = preferences
    override suspend fun update(
        transform: (AccessPreferences) -> AccessPreferences
    ): AccessPreferences {
        preferences = transform(preferences)
        return preferences
    }
}
