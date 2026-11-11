package dev.vladleesi.scadangers

import android.app.KeyguardManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladleesi.scadangers.core.security.device.DevicePostureEvaluator
import dev.vladleesi.scadangers.core.security.device.DeviceSettings
import dev.vladleesi.scadangers.core.security.device.FindingStatus
import dev.vladleesi.scadangers.core.security.device.SignalState
import dev.vladleesi.scadangers.device.AndroidDeviceObservationSource
import dev.vladleesi.scadangers.device.deviceSettingsIntent
import dev.vladleesi.scadangers.device.observeDeviceSignal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Run on API 24/28/37 to exercise the Private DNS boundary and on OEM hardware before release. */
class DevicePostureIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun publicDeviceApisProduceAnAssessmentWithoutGoogleServicesOrDangerousPermission() =
        runBlocking {
            val snapshot = AndroidDeviceObservationSource(context).read()
            assertEquals(Build.VERSION.SDK_INT, snapshot.profile.apiLevel)
            val result = DevicePostureEvaluator().evaluate(snapshot)
            assertEquals(
                FindingStatus.Unknown,
                result.findings.single {
                    it.id == "device.integrity"
                }.status
            )
            assertTrue(result.findings.isNotEmpty())
            assertFalse(result.findings.any { it.automaticRemediationAvailable })
            if (snapshot.secureLock.state == SignalState.Enabled ||
                snapshot.secureLock.state == SignalState.Disabled
            ) {
                val expected = context.getSystemService(KeyguardManager::class.java).isDeviceSecure
                assertEquals(expected, snapshot.secureLock.state == SignalState.Enabled)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
                snapshot.network.connected.state == SignalState.Enabled
            ) {
                assertEquals(SignalState.Unsupported, snapshot.network.privateDns.state)
            }
        }

    @Test
    fun restrictedReadIsExplicitlyUnknownAndSettingsIntentsOnlyOpenSystemUi() {
        val restricted = observeDeviceSignal("synthetic restriction") { throw SecurityException() }
        assertEquals(SignalState.Unknown, restricted.state)
        DeviceSettings.entries.forEach { action ->
            val intent = deviceSettingsIntent(action)
            assertTrue(intent.action.orEmpty().startsWith("android.settings."))
            assertEquals(null, intent.data)
            assertEquals(null, intent.extras)
        }
    }
}
