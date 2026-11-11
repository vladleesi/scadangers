package dev.vladleesi.scadangers.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vladleesi.scadangers.core.security.device.DeviceObservationSource
import dev.vladleesi.scadangers.core.security.device.DevicePostureEvaluator
import dev.vladleesi.scadangers.feature.device.DeviceUiState
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Created when Device opens; assessments stay in memory and never run at application startup. */
@HiltViewModel
internal class DeviceViewModel @Inject constructor(
    private val source: DeviceObservationSource,
    @DeviceEvaluationDispatcher private val evaluationDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeviceUiState())
    val state = mutableState.asStateFlow()
    private var assessmentJob: Job? = null

    fun assess() {
        if (assessmentJob?.isActive == true) return
        assessmentJob = viewModelScope.launch {
            mutableState.value =
                DeviceUiState(
                    busy = true,
                    summary = "Assessment in progress; earlier observations are no longer current."
                )
            try {
                val result = withTimeout(DEADLINE_MILLIS) {
                    val observations = source.read()
                    withContext(evaluationDispatcher) {
                        DevicePostureEvaluator().evaluate(observations).presentation()
                    }
                }
                mutableState.value = result
            } catch (_: TimeoutCancellationException) {
                fail("Assessment deadline exceeded. No current result is available; retry.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                fail("Android restricted the assessment. No current result is available.")
            } catch (_: IllegalStateException) {
                fail("Platform service unavailable. No current result is available; retry.")
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun cancel() {
        if (assessmentJob?.isActive != true) return
        assessmentJob?.cancel()
        fail("Assessment cancelled. No current result is available.")
    }

    fun settingsUnavailable() =
        fail("This system settings screen is unavailable or restricted on this device.")

    private fun fail(message: String) {
        mutableState.update {
            it.copy(
                error = message,
                summary = if (it.assessment ==
                    null
                ) {
                    "No current assessment is available."
                } else {
                    it.summary
                }
            )
        }
    }

    private companion object {
        const val DEADLINE_MILLIS = 10_000L
    }
}
