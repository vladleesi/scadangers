package dev.vladleesi.scadangers.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dev.vladleesi.scadangers.core.security.device.DeviceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun deviceSettingsIntent(action: DeviceSettings): Intent = Intent(
    when (action) {
        DeviceSettings.Security -> Settings.ACTION_SECURITY_SETTINGS

        // The public update action was added in Android 16 QPR2 (API 36.1).
        // Device information is the supported entry point on older versions.
        DeviceSettings.Updates -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.BAKLAVA) {
            Settings.ACTION_SYSTEM_UPDATE_SETTINGS
        } else {
            Settings.ACTION_DEVICE_INFO_SETTINGS
        }

        DeviceSettings.Network -> Settings.ACTION_WIRELESS_SETTINGS

        DeviceSettings.Developer -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS

        DeviceSettings.DeviceInfo -> Settings.ACTION_DEVICE_INFO_SETTINGS
    }
)

/** Only a user click launches settings; Android remains responsible for any configuration change. */
internal suspend fun openDeviceSettings(context: Context, action: DeviceSettings): Boolean {
    val intent = deviceSettingsIntent(action)
    val available = withContext(Dispatchers.IO) {
        observeDeviceSignal("Settings available") {
            context.packageManager.resolveActivity(intent, 0) != null
        }
    }
    if (available.state !=
        dev.vladleesi.scadangers.core.security.device.SignalState.Enabled
    ) {
        return false
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
