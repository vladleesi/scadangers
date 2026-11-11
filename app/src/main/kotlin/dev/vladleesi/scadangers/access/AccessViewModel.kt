package dev.vladleesi.scadangers.access

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vladleesi.scadangers.core.security.capability.AccessAction
import dev.vladleesi.scadangers.core.security.capability.AccessRepository
import dev.vladleesi.scadangers.core.security.capability.Capability
import dev.vladleesi.scadangers.core.security.capability.CapabilityState
import dev.vladleesi.scadangers.core.security.capability.OnboardingStep
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class AccessUiState(
    val onboardingStep: OnboardingStep? = null,
    val capabilities: List<CapabilityState> = emptyList(),
    val explanation: Capability? = null,
    val busy: Boolean = false,
    val failure: AccessFailure? = null
)

internal enum class AccessFailure { Storage, Platform, SystemScreenUnavailable }

internal data class SystemAccessRequest(
    val capability: Capability,
    val action: AccessAction,
    val permissions: Set<String>
)

/** Owns events and restoration; no permission decision lives in a composable. */
@HiltViewModel
internal class AccessViewModel @Inject constructor(
    private val repository: AccessRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        AccessUiState(
            explanation = Capability.entries.firstOrNull {
                it.name ==
                    savedState.get<String>(EXPLANATION)
            }
        )
    )
    val state = mutableState.asStateFlow()
    private val requests = Channel<SystemAccessRequest>(Channel.BUFFERED)
    val systemRequests = requests.receiveAsFlow()
    private var refreshJob: Job? = null
    private val operations = Mutex()
    private var continueForSession = false

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { operations.withLock { handleFailure { evaluate() } } }
    }

    fun selectCapability(capability: Capability?) {
        savedState[EXPLANATION] = capability?.name
        mutableState.update { it.copy(explanation = capability) }
    }

    fun confirmAccess() = action {
        val capability = mutableState.value.explanation ?: return@action
        // Re-evaluate immediately before requesting, including after a restored explanation.
        val evaluation = repository.refresh()
        val capabilityState = evaluation.capabilities.first { it.capability == capability }
        val permissions = capabilityState.requirements.requestPermissions(
            evaluation.snapshot.apiLevel
        )
        if (capabilityState.action != AccessAction.None) {
            repository.recordRequest(
                capability,
                if (capabilityState.action ==
                    AccessAction.RuntimePermission
                ) {
                    permissions
                } else {
                    emptySet()
                }
            )
            selectCapability(null)
            requests.send(SystemAccessRequest(capability, capabilityState.action, permissions))
        } else {
            selectCapability(null)
        }
        evaluate()
    }

    fun permissionResult(results: Map<String, Boolean>, settingsPermissions: Set<String>) = action {
        repository.recordResult(results, settingsPermissions)
        evaluate()
    }

    fun onboarding(step: OnboardingStep) = action {
        repository.saveOnboarding(step)
        evaluate()
    }

    fun continueWithoutSaving() {
        continueForSession = true
        mutableState.update { it.copy(onboardingStep = OnboardingStep.Complete) }
    }

    fun systemScreenUnavailable() {
        mutableState.update { it.copy(failure = AccessFailure.SystemScreenUnavailable) }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            operations.withLock {
                mutableState.update { it.copy(busy = true) }
                try {
                    handleFailure(block)
                } finally {
                    mutableState.update { it.copy(busy = false) }
                }
            }
        }
    }

    private suspend fun evaluate() {
        val evaluation = repository.refresh()
        mutableState.update {
            it.copy(
                onboardingStep = if (continueForSession) {
                    OnboardingStep.Complete
                } else {
                    evaluation.preferences.onboardingStep
                },
                capabilities = evaluation.capabilities,
                failure = null
            )
        }
    }

    private suspend fun handleFailure(block: suspend () -> Unit) {
        val failure = try {
            block()
            null
        } catch (_: IOException) {
            AccessFailure.Storage
        } catch (_: SecurityException) {
            AccessFailure.Platform
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            AccessFailure.Platform
        }
        if (failure != null) {
            // A failed refresh must not leave a stale Granted badge on screen.
            mutableState.update { it.copy(capabilities = emptyList(), failure = failure) }
        }
    }

    private companion object {
        const val EXPLANATION = "access_explanation"
    }
}
