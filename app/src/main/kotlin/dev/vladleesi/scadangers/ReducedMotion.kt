package dev.vladleesi.scadangers

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Tracks the system "Remove animations" setting. Re-read on every resume, because changing
 * the setting does not recreate the activity.
 */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reducedMotion by remember { mutableStateOf(isReducedMotionEnabled(context)) }
    LifecycleResumeEffect(context) {
        reducedMotion = isReducedMotionEnabled(context)
        onPauseOrDispose { }
    }
    return reducedMotion
}

private fun isReducedMotionEnabled(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // In-process value pushed by the system; no IPC.
        !ValueAnimator.areAnimatorsEnabled()
    } else {
        // API 24-25: a cached Settings provider read.
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) ==
            0f
    }
