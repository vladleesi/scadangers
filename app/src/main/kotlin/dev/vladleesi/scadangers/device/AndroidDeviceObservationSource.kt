package dev.vladleesi.scadangers.device

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vladleesi.scadangers.core.security.device.DeviceObservationSource
import dev.vladleesi.scadangers.core.security.device.DeviceObservations
import dev.vladleesi.scadangers.core.security.device.DeviceSettings
import dev.vladleesi.scadangers.core.security.device.DeviceSignal
import dev.vladleesi.scadangers.core.security.device.EncryptionSignal
import dev.vladleesi.scadangers.core.security.device.EncryptionState
import dev.vladleesi.scadangers.core.security.device.SignalState
import dev.vladleesi.scadangers.readDeviceProfile
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Public, read-only APIs. Binder/settings queries run only in foreground assessment work on IO. */
internal class AndroidDeviceObservationSource @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceObservationSource {
    override suspend fun read(): DeviceObservations = withContext(Dispatchers.IO) {
        val profile = readDeviceProfile()
        currentCoroutineContext().ensureActive()
        val lock = secureLock()
        val encryption = encryption()
        currentCoroutineContext().ensureActive()
        val adb = readAdb()
        val network = readNetwork(context)
        currentCoroutineContext().ensureActive()
        val settings = DeviceSettings.entries.associateWith { action ->
            observeDeviceSignal("${action.name} settings resolves") {
                context.packageManager.resolveActivity(deviceSettingsIntent(action), 0) != null
            }
        }
        currentCoroutineContext().ensureActive()
        DeviceObservations(
            profile,
            System.currentTimeMillis(),
            lock,
            encryption,
            adb,
            network,
            settings
        )
    }

    private fun secureLock(): DeviceSignal {
        if (!context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_SECURE_LOCK_SCREEN
            )
        ) {
            return DeviceSignal(
                SignalState.Unsupported,
                "Secure lock screen feature unavailable on this device."
            )
        }
        return observeDeviceSignal("KeyguardManager.isDeviceSecure") {
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure
        }
    }

    private fun encryption(): EncryptionSignal = try {
        val reported = context.getSystemService(
            DevicePolicyManager::class.java
        )?.storageEncryptionStatus
        val state = when (reported) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> EncryptionState.Active
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> EncryptionState.PerUser
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> EncryptionState.DefaultKey
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> EncryptionState.Inactive
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> EncryptionState.Unsupported
            else -> EncryptionState.Unknown
        }
        EncryptionSignal(
            state,
            "DevicePolicyManager storage encryption: $state " +
                "(reported code ${reported ?: "unavailable"})"
        )
    } catch (_: SecurityException) {
        EncryptionSignal(
            EncryptionState.Unknown,
            "Storage encryption indicator restricted by Android."
        )
    } catch (_: UnsupportedOperationException) {
        EncryptionSignal(
            EncryptionState.Unsupported,
            "Storage encryption indicator unavailable on this device."
        )
    } catch (_: IllegalStateException) {
        EncryptionSignal(EncryptionState.Unknown, "Storage encryption service unavailable.")
    }

    private fun readAdb(): DeviceSignal =
        observeDeviceSignal("Settings.Global.ADB_ENABLED (USB debugging)") {
            when (Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED)) {
                0 -> false
                1 -> true
                else -> null
            }
        }
}

internal fun observeDeviceSignal(label: String, read: () -> Boolean?): DeviceSignal = try {
    when (read()) {
        true -> DeviceSignal(SignalState.Enabled, "$label: yes")
        false -> DeviceSignal(SignalState.Disabled, "$label: no")
        null -> DeviceSignal(SignalState.Unknown, "$label: not reported")
    }
} catch (_: SecurityException) {
    DeviceSignal(SignalState.Unknown, "$label: access restricted")
} catch (_: Settings.SettingNotFoundException) {
    DeviceSignal(SignalState.Unknown, "$label: setting not reported")
} catch (_: UnsupportedOperationException) {
    DeviceSignal(SignalState.Unsupported, "$label: API unavailable")
} catch (_: IllegalStateException) {
    DeviceSignal(SignalState.Unknown, "$label: platform service unavailable")
}
