package dev.vladleesi.scadangers

import android.os.Build
import dev.vladleesi.scadangers.core.common.device.DeviceProfile

/** Reads build properties. These are static process constants: no I/O, IPC or permission. */
internal fun readDeviceProfile(): DeviceProfile = DeviceProfile.fromReportedValues(
    manufacturer = Build.MANUFACTURER,
    model = Build.MODEL,
    androidRelease = Build.VERSION.RELEASE,
    apiLevel = Build.VERSION.SDK_INT,
    securityPatchLevel = Build.VERSION.SECURITY_PATCH
)
