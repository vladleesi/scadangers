package dev.vladleesi.scadangers.access

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.vladleesi.scadangers.BuildConfig
import dev.vladleesi.scadangers.R
import dev.vladleesi.scadangers.ScadangersApp
import dev.vladleesi.scadangers.core.designsystem.component.CardContent
import dev.vladleesi.scadangers.core.designsystem.component.ConfirmationDialog
import dev.vladleesi.scadangers.core.designsystem.component.readableWidth
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersSpacing
import dev.vladleesi.scadangers.core.designsystem.theme.ScadangersTheme
import dev.vladleesi.scadangers.core.security.capability.AccessAction
import dev.vladleesi.scadangers.core.security.capability.OnboardingStep
import dev.vladleesi.scadangers.feature.apps.AppsScreen
import dev.vladleesi.scadangers.feature.device.DeviceScreen
import dev.vladleesi.scadangers.readDeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AccessRoot(
    state: AccessUiState,
    model: AccessViewModel,
    appsContent: @Composable () -> Unit = { AppsScreen() },
    deviceContent: @Composable () -> Unit = { DeviceScreen(readDeviceProfile()) }
) {
    if (state.onboardingStep == OnboardingStep.Complete) {
        ScadangersApp(
            appVersion = BuildConfig.VERSION_NAME,
            accessState = state,
            onExplainAccess = model::selectCapability,
            onRefreshAccess = model::refresh,
            appsContent = appsContent,
            deviceContent = deviceContent
        )
    } else {
        OnboardingScreen(state, model::onboarding, model::refresh, model::continueWithoutSaving)
    }
    val explanation = state.explanation
    if (explanation != null && state.onboardingStep == OnboardingStep.Complete) {
        val access = state.capabilities.firstOrNull { it.capability == explanation }
        val copy = explanation.copyResources()
        val message = stringResource(copy.rationale) + "\n\n" +
            stringResource(access?.reasonResource() ?: R.string.access_unknown) + "\n\n" +
            stringResource(R.string.access_no_engine)
        ConfirmationDialog(
            title = stringResource(copy.title),
            message = message,
            confirmLabel = stringResource(
                when (access?.action) {
                    AccessAction.RuntimePermission -> R.string.access_request

                    AccessAction.AppSettings, AccessAction.LocationSettings ->
                        R.string.access_open_settings

                    AccessAction.VpnConsent -> R.string.access_request_vpn

                    else -> R.string.access_close
                }
            ),
            dismissLabel = stringResource(R.string.access_not_now),
            onConfirm = {
                if (access == null || access.action == AccessAction.None) {
                    model.selectCapability(null)
                } else if (!state.busy) {
                    model.confirmAccess()
                }
            },
            onDismiss = { model.selectCapability(null) }
        )
    }
}

@Composable
internal fun OnboardingScreen(
    state: AccessUiState,
    onStep: (OnboardingStep) -> Unit,
    onRetry: () -> Unit,
    onContinueWithoutSaving: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScadangersTheme.colors.background,
        contentColor = ScadangersTheme.colors.textPrimary
    ) {
        Column(
            Modifier.fillMaxSize()
                .safeDrawingPadding().verticalScroll(rememberScrollState()).testTag("onboarding"),
            verticalArrangement = Arrangement.spacedBy(ScadangersSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardContent(Modifier.readableWidth()) {
                BrandIcon()
                val choices = state.onboardingStep == OnboardingStep.Choices
                val heading = if (choices) {
                    R.string.onboarding_choices_title
                } else {
                    R.string.onboarding_title
                }
                Text(
                    stringResource(heading),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(stringResource(R.string.onboarding_privacy))
                Text(stringResource(R.string.onboarding_without_access))
                Text(
                    stringResource(
                        if (choices) R.string.onboarding_choices else R.string.onboarding_contextual
                    )
                )
                Text(stringResource(R.string.access_vpn_reason))
                Text(stringResource(R.string.onboarding_no_background))
                state.failure?.let { AccessError(it) }
                OnboardingActions(state, onStep, onRetry, onContinueWithoutSaving)
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    state: AccessUiState,
    onStep: (OnboardingStep) -> Unit,
    onRetry: () -> Unit,
    onContinueWithoutSaving: () -> Unit
) {
    val choices = state.onboardingStep == OnboardingStep.Choices
    if (state.onboardingStep != null) {
        Button(
            onClick = {
                onStep(if (choices) OnboardingStep.Complete else OnboardingStep.Choices)
            },
            enabled = !state.busy
        ) {
            Text(
                stringResource(
                    if (choices) R.string.onboarding_finish else R.string.onboarding_next
                )
            )
        }
        TextButton(onClick = { onStep(OnboardingStep.Complete) }, enabled = !state.busy) {
            Text(stringResource(R.string.onboarding_skip))
        }
        if (choices) {
            TextButton(onClick = {
                onStep(OnboardingStep.Welcome)
            }, enabled = !state.busy) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    } else if (state.failure == null) {
        Text(stringResource(R.string.access_loading))
    }
    if (state.failure != null) {
        TextButton(onClick = onRetry) { Text(stringResource(R.string.access_retry)) }
        TextButton(onClick = onContinueWithoutSaving) {
            Text(stringResource(R.string.onboarding_session))
        }
    }
}

@Composable
private fun BrandIcon() {
    val resources = LocalResources.current
    val iconSize = ScadangersSpacing.xxxl * 2
    val iconSizePixels = with(LocalDensity.current) { iconSize.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, resources, iconSizePixels) {
        value =
            withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(resources, R.drawable.ic_brand, bounds)
                val sizeRatio = minOf(bounds.outWidth, bounds.outHeight) / iconSizePixels
                val options = BitmapFactory.Options().apply {
                    inSampleSize = Integer.highestOneBit(sizeRatio.coerceAtLeast(1))
                }
                checkNotNull(
                    BitmapFactory.decodeResource(resources, R.drawable.ic_brand, options)
                ).asImageBitmap()
            }
    }
    Box(Modifier.size(iconSize)) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
