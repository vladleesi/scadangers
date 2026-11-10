package dev.scadangers.access

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.VpnService
import android.os.Build
import android.os.UserManager
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.scadangers.core.security.capability.AccessSnapshotSource
import dev.scadangers.core.security.capability.Capability
import dev.scadangers.core.security.capability.CapabilityPolicy
import dev.scadangers.core.security.capability.Hardware
import dev.scadangers.core.security.capability.PlatformAccessSnapshot
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only reads platform access; never enumerates apps, enables radios or starts discovery/VPN. */
internal class AndroidAccessSource @Inject constructor(
    @ApplicationContext private val context: Context
) : AccessSnapshotSource {
    override suspend fun read(): PlatformAccessSnapshot = withContext(Dispatchers.IO) {
        val apiLevel = Build.VERSION.SDK_INT
        val permissions = Capability.entries.flatMap {
            val requirements = CapabilityPolicy.requirements(it, apiLevel)
            requirements.requestPermissions(apiLevel) + requirements.manifestPermissions
        }.toSet()
        val packageManager = context.packageManager
        val locationManager = context.getSystemService(LocationManager::class.java)
        val notificationManager =
            checkNotNull(context.getSystemService(NotificationManager::class.java))
        val userManager = checkNotNull(context.getSystemService(UserManager::class.java))
        val vpnRestricted = userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_VPN)
        PlatformAccessSnapshot(
            apiLevel = apiLevel,
            grantedPermissions = permissions.filter {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }.toSet(),
            hardware = buildSet {
                if (packageManager.hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE
                    )
                ) {
                    add(Hardware.BluetoothLe)
                }
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) add(Hardware.Wifi)
            },
            notificationsEnabled = notificationManager.areNotificationsEnabled(),
            locationEnabled =
                locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager),
            vpnPrepared = !vpnRestricted && VpnService.prepare(context) == null,
            vpnRestricted = vpnRestricted
        )
    }
}
