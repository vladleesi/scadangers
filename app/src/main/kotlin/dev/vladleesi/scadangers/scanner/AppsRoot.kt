package dev.vladleesi.scadangers.scanner

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.scadangers.feature.apps.AppDetailsScreen
import dev.vladleesi.scadangers.feature.apps.AppSettingsAction
import dev.vladleesi.scadangers.feature.apps.AppsScreen

@Composable
internal fun AppsRoot(model: AppsViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(model) {
        model.resume()
        onPauseOrDispose { model.pause() }
    }
    val details = state.details
    BackHandler(enabled = details != null) { model.open(null) }
    if (details == null) {
        AppsScreen(
            state = state,
            onScan = model::scan,
            onCancel = model::cancel,
            onFilters = model::filters,
            onOpen = { model.open(it) }
        )
    } else {
        AppDetailsScreen(details, state.error, { model.open(null) }) { action ->
            if (!openAppSettings(context, details.packageName, action)) model.settingsUnavailable()
        }
    }
}

internal fun openAppSettings(
    context: Context,
    packageName: String,
    action: AppSettingsAction
): Boolean {
    val intent = settingsIntent(packageName, action) ?: return false
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

internal fun settingsIntent(packageName: String, action: AppSettingsAction): Intent? {
    val settingsAction = when (action) {
        AppSettingsAction.Details -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS

        AppSettingsAction.Overlay -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION

        AppSettingsAction.InstallSources -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
        } else {
            null
        }

        AppSettingsAction.WriteSettings -> Settings.ACTION_MANAGE_WRITE_SETTINGS

        AppSettingsAction.Usage -> Settings.ACTION_USAGE_ACCESS_SETTINGS

        AppSettingsAction.Accessibility -> Settings.ACTION_ACCESSIBILITY_SETTINGS

        AppSettingsAction.Notifications -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

        AppSettingsAction.Administrators -> Settings.ACTION_SECURITY_SETTINGS

        AppSettingsAction.AllFiles -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        } else {
            null
        }
    }
    if (settingsAction == null) return null
    val intent = Intent(settingsAction)
    if (action in PACKAGE_SETTINGS) intent.data = Uri.fromParts("package", packageName, null)
    return intent
}

private val PACKAGE_SETTINGS = setOf(
    AppSettingsAction.Details,
    AppSettingsAction.Overlay,
    AppSettingsAction.InstallSources,
    AppSettingsAction.WriteSettings,
    AppSettingsAction.AllFiles
)
