package dev.vladleesi.scadangers.access

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.vladleesi.scadangers.core.security.capability.AccessAction
import dev.vladleesi.scadangers.core.security.capability.Capability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Registered once by the Activity; result delivery survives activity/process recreation. */
internal class SystemAccessLauncher(
    private val activity: ComponentActivity,
    private val model: AccessViewModel
) {
    private val runtimePermissions = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val settingsPermissions = results.filter { (permission, granted) ->
            !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }.keys
        model.permissionResult(results, settingsPermissions)
    }
    private val systemActivity = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Result codes are not authorization. Read the OS again, including VPN preparation.
        model.refresh()
    }

    fun observe() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.refresh()
                model.systemRequests.collect { request -> launch(request) }
            }
        }
    }

    private suspend fun launch(request: SystemAccessRequest) {
        try {
            when (request.action) {
                AccessAction.RuntimePermission -> runtimePermissions.launch(
                    request.permissions.toTypedArray()
                )

                AccessAction.AppSettings -> systemActivity.launch(
                    settingsIntent(request.capability)
                )

                AccessAction.LocationSettings -> systemActivity.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )

                AccessAction.VpnConsent -> {
                    val consent = withContext(Dispatchers.IO) { VpnService.prepare(activity) }
                    if (consent == null) model.refresh() else systemActivity.launch(consent)
                }

                AccessAction.None -> model.refresh()
            }
        } catch (_: ActivityNotFoundException) {
            model.systemScreenUnavailable()
        } catch (_: SecurityException) {
            model.systemScreenUnavailable()
        }
    }

    private fun settingsIntent(capability: Capability): Intent =
        if (capability == Capability.Notifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${activity.packageName}".toUri()
            )
        }
}
