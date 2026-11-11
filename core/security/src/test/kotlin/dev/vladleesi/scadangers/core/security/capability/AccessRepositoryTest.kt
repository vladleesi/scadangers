package dev.vladleesi.scadangers.core.security.capability

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AccessRepositoryTest {
    @Test
    fun settingsReentryRefreshesGrantRevocationAndRegrant() = runTest {
        var platform = snapshot()
        val store = MemoryAccessStore()
        val repository = AccessRepository({ platform }, store)
        repository.recordRequest(Capability.Notifications, setOf(Permissions.NOTIFICATIONS))
        repository.recordResult(
            mapOf(Permissions.NOTIFICATIONS to false),
            setOf(Permissions.NOTIFICATIONS)
        )
        assertEquals(AccessAction.AppSettings, repository.refresh().notification().action)

        platform =
            platform.copy(
                grantedPermissions =
                    platform.grantedPermissions + Permissions.NOTIFICATIONS
            )
        assertEquals(AccessStatus.Granted, repository.refresh().notification().status)
        assertFalse(Permissions.NOTIFICATIONS in store.preferences.history.settingsPermissions)

        platform =
            platform.copy(
                grantedPermissions =
                    platform.grantedPermissions - Permissions.NOTIFICATIONS
            )
        assertEquals(AccessStatus.Revoked, repository.refresh().notification().status)
        assertEquals(AccessAction.RuntimePermission, repository.refresh().notification().action)

        platform =
            platform.copy(
                grantedPermissions =
                    platform.grantedPermissions + Permissions.NOTIFICATIONS
            )
        assertEquals(AccessStatus.Granted, repository.refresh().notification().status)
    }

    @Test
    fun freshRepositoryRestoresOnboardingButRechecksGrants() = runTest {
        val store = MemoryAccessStore()
        val first = AccessRepository({ snapshot() }, store)
        first.saveOnboarding(OnboardingStep.Choices)
        first.recordRequest(Capability.VpnAuthorization, emptySet())
        val restored = AccessRepository({ snapshot() }, store).refresh()
        assertEquals(OnboardingStep.Choices, restored.preferences.onboardingStep)
        assertEquals(
            AccessStatus.Denied,
            restored.capabilities.first {
                it.capability ==
                    Capability.VpnAuthorization
            }.status
        )
        first.saveOnboarding(OnboardingStep.Complete)
        assertEquals(
            OnboardingStep.Complete,
            AccessRepository({
                snapshot()
            }, store).refresh().preferences.onboardingStep
        )
    }

    private fun AccessEvaluation.notification() = capabilities.first {
        it.capability ==
            Capability.Notifications
    }
}

private class MemoryAccessStore : AccessStore {
    var preferences = AccessPreferences()
    override suspend fun read() = preferences
    override suspend fun update(
        transform: (AccessPreferences) -> AccessPreferences
    ): AccessPreferences {
        preferences = transform(preferences)
        return preferences
    }
}
