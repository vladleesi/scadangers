package dev.vladleesi.scadangers.access

import androidx.lifecycle.SavedStateHandle
import dev.vladleesi.scadangers.core.security.capability.AccessAction
import dev.vladleesi.scadangers.core.security.capability.AccessPreferences
import dev.vladleesi.scadangers.core.security.capability.AccessRepository
import dev.vladleesi.scadangers.core.security.capability.AccessStatus
import dev.vladleesi.scadangers.core.security.capability.AccessStore
import dev.vladleesi.scadangers.core.security.capability.Capability
import dev.vladleesi.scadangers.core.security.capability.Hardware
import dev.vladleesi.scadangers.core.security.capability.OnboardingStep
import dev.vladleesi.scadangers.core.security.capability.Permissions
import dev.vladleesi.scadangers.core.security.capability.PlatformAccessSnapshot
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeStore()
    private var snapshot =
        PlatformAccessSnapshot(
            37,
            setOf(Permissions.INTERNET),
            Hardware.entries.toSet(),
            true,
            true,
            false
        )

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun explanationAndOnboardingRestoreWithoutReplayingPermissionRequest() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val first = model(savedState)
        advanceUntilIdle()
        first.onboarding(OnboardingStep.Choices)
        advanceUntilIdle()
        first.selectCapability(Capability.Notifications)

        // New instances, as after process death: only durable preferences and small saved values survive.
        val restoredState =
            SavedStateHandle(
                mapOf("access_explanation" to savedState.get<String>("access_explanation"))
            )
        val restored = model(restoredState)
        advanceUntilIdle()
        assertEquals(OnboardingStep.Choices, restored.state.value.onboardingStep)
        assertEquals(Capability.Notifications, restored.state.value.explanation)
        assertEquals(emptySet<String>(), store.preferences.history.requestedPermissions)
        assertEquals(
            AccessStatus.NotRequested,
            restored.state.value.capabilities.first {
                it.capability ==
                    Capability.Notifications
            }.status
        )
    }

    @Test
    fun noSystemPromptUntilExplanationIsConfirmedAndResultsSurviveNewModel() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.selectCapability(Capability.Notifications)
        assertEquals(emptySet<String>(), store.preferences.history.requestedPermissions)
        model.confirmAccess()
        advanceUntilIdle()
        val request = model.systemRequests.first()
        assertEquals(AccessAction.RuntimePermission, request.action)
        assertEquals(setOf(Permissions.NOTIFICATIONS), request.permissions)
        assertNull(model.state.value.explanation)

        model.permissionResult(
            mapOf(Permissions.NOTIFICATIONS to false),
            setOf(Permissions.NOTIFICATIONS)
        )
        advanceUntilIdle()
        val restored = model()
        advanceUntilIdle()
        val state = restored.state.value.capabilities.first {
            it.capability ==
                Capability.Notifications
        }
        assertEquals(AccessStatus.Denied, state.status)
        assertEquals(AccessAction.AppSettings, state.action)
    }

    @Test
    fun resumeRefreshReadsNewPermissionInsteadOfRememberingGrant() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        snapshot =
            snapshot.copy(
                grantedPermissions =
                    snapshot.grantedPermissions + Permissions.NOTIFICATIONS
            )
        model.refresh()
        advanceUntilIdle()
        assertEquals(
            AccessStatus.Granted,
            model.state.value.capabilities.first {
                it.capability ==
                    Capability.Notifications
            }.status
        )
        snapshot =
            snapshot.copy(
                grantedPermissions =
                    snapshot.grantedPermissions - Permissions.NOTIFICATIONS
            )
        model.refresh()
        advanceUntilIdle()
        assertEquals(
            AccessStatus.Revoked,
            model.state.value.capabilities.first {
                it.capability ==
                    Capability.Notifications
            }.status
        )
    }

    @Test
    fun diskFailureClearsAccessAndAllowsSessionOnlyOnboardingSkip() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        store.fail = true
        model.onboarding(OnboardingStep.Complete)
        advanceUntilIdle()
        assertEquals(AccessFailure.Storage, model.state.value.failure)
        assertEquals(emptyList<Any>(), model.state.value.capabilities)
        model.continueWithoutSaving()
        assertEquals(OnboardingStep.Complete, model.state.value.onboardingStep)
        assertEquals(OnboardingStep.Welcome, store.preferences.onboardingStep)
    }

    @Test
    fun cancellationIsNeverConvertedIntoPlatformFailure() = runTest(dispatcher) {
        val model =
            AccessViewModel(
                AccessRepository({
                    throw CancellationException("test cancellation")
                }, store),
                SavedStateHandle()
            )
        advanceUntilIdle()
        assertNull(model.state.value.failure)
    }

    private fun model(savedState: SavedStateHandle = SavedStateHandle()) =
        AccessViewModel(AccessRepository({ snapshot }, store), savedState)
}

private class FakeStore : AccessStore {
    var preferences = AccessPreferences()
    var fail = false
    override suspend fun read(): AccessPreferences {
        if (fail) throw IOException("synthetic storage failure")
        return preferences
    }

    override suspend fun update(
        transform: (AccessPreferences) -> AccessPreferences
    ): AccessPreferences {
        if (fail) throw IOException("synthetic storage failure")
        preferences = transform(preferences)
        return preferences
    }
}
