package dev.vladleesi.scadangers.device

import dev.vladleesi.scadangers.core.common.device.DeviceProfile
import dev.vladleesi.scadangers.core.security.device.DeviceObservationSource
import dev.vladleesi.scadangers.core.security.device.DeviceObservations
import dev.vladleesi.scadangers.core.security.device.DeviceSignal
import dev.vladleesi.scadangers.core.security.device.EncryptionSignal
import dev.vladleesi.scadangers.core.security.device.EncryptionState
import dev.vladleesi.scadangers.core.security.device.NetworkObservation
import dev.vladleesi.scadangers.core.security.device.SignalState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun cancelledAndOverlappingAssessmentsDoNotPublishPartialResults() = runTest(dispatcher) {
        var reads = 0
        val model = model {
            reads++
            awaitCancellation()
        }
        model.assess()
        runCurrent()
        model.assess()
        runCurrent()
        org.junit.Assert.assertEquals(1, reads)
        model.cancel()
        runCurrent()
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.assessment)
        assertTrue(model.state.value.error.orEmpty().contains("cancelled"))
    }

    @Test
    fun stalledCollectionHasABoundedDeadline() = runTest(dispatcher) {
        val model = model { awaitCancellation() }
        model.assess()
        advanceUntilIdle()
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.assessment)
        assertTrue(model.state.value.error.orEmpty().contains("deadline"))
    }

    @Test
    fun failedRefreshClearsPreviousFindingsAndCanBeRetried() = runTest(dispatcher) {
        var failure = false
        val model = model {
            if (failure) throw SecurityException("synthetic restriction")
            fixture()
        }
        model.assess()
        model.state.first { it.assessment != null }
        runCurrent()
        assertTrue(model.state.value.findings.isNotEmpty())
        failure = true
        model.assess()
        runCurrent()
        assertNull(model.state.value.assessment)
        assertTrue(model.state.value.findings.isEmpty())
        assertTrue(model.state.value.error.orEmpty().contains("restricted"))
        failure = false
        model.assess()
        model.state.first { it.assessment != null }
        assertNull(model.state.value.error)
    }

    @Test
    fun navigationCancellationReleasesCollectionResources() = runTest(dispatcher) {
        val released = CompletableDeferred<Unit>()
        val source = DeviceObservationSource {
            try {
                awaitCancellation()
            } finally {
                released.complete(Unit)
            }
        }
        val model = model(source)
        model.assess()
        runCurrent()
        model.cancel()
        released.await()
        assertNull(model.state.value.assessment)
    }

    private fun model(source: DeviceObservationSource) = DeviceViewModel(source, dispatcher)

    private fun fixture(): DeviceObservations {
        val unknown = DeviceSignal(SignalState.Unknown, "synthetic unknown")
        return DeviceObservations(
            DeviceProfile.fromReportedValues("Example", "Synthetic", "17", 37, "2026-11-05"),
            1_794_355_200_000L,
            unknown,
            EncryptionSignal(EncryptionState.Unknown, "synthetic unknown"),
            unknown,
            NetworkObservation(unknown, unknown, unknown, unknown, unknown),
            emptyMap()
        )
    }
}
