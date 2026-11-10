package dev.scadangers

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.security.NetworkSecurityPolicy
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlatformSecurityTest {
    @Test
    fun applicationHasRestrictedNetworkAndBackupPolicy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted)
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(37, context.applicationInfo.targetSdkVersion)
        listOf(
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BIND_VPN_SERVICE"
        ).forEach { permission ->
            assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(permission))
        }
    }

    @Test
    fun composeToolingPreviewActivityIsNotPackaged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activities = packageActivities(context.packageManager, context.packageName)
        assertFalse(activities.any { it.name == "androidx.compose.ui.tooling.PreviewActivity" })
    }

    @Test
    fun launcherSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
            }
        }
    }
}

private fun packageActivities(
    packageManager: PackageManager,
    packageName: String
): List<ActivityInfo> {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    }
    return info.activities.orEmpty().toList()
}
