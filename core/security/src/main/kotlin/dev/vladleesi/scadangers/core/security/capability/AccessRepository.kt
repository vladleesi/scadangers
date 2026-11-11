package dev.vladleesi.scadangers.core.security.capability

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class OnboardingStep { Welcome, Choices, Complete }

data class AccessPreferences(
    val onboardingStep: OnboardingStep = OnboardingStep.Welcome,
    val history: AccessHistory = AccessHistory()
)

/** Implementations must serialize updates and keep disk work off Main. */
interface AccessStore {
    suspend fun read(): AccessPreferences
    suspend fun update(transform: (AccessPreferences) -> AccessPreferences): AccessPreferences
}

fun interface AccessSnapshotSource {
    suspend fun read(): PlatformAccessSnapshot
}

data class AccessEvaluation(
    val preferences: AccessPreferences,
    val snapshot: PlatformAccessSnapshot,
    val capabilities: List<CapabilityState>
)

/** Serializes OS refresh, request history and onboarding; stored history cannot grant access. */
class AccessRepository(private val source: AccessSnapshotSource, private val store: AccessStore) {
    private val mutex = Mutex()

    suspend fun refresh(): AccessEvaluation = mutex.withLock {
        val preferences = store.read()
        val snapshot = source.read()
        val capabilities = Capability.entries.map {
            CapabilityStateMachine.evaluate(it, snapshot, preferences.history)
        }
        val history = preferences.history.copy(
            previouslyGranted = preferences.history.previouslyGranted + capabilities
                .filter { it.status == AccessStatus.Granted }.map { it.capability },
            settingsPermissions =
                preferences.history.settingsPermissions - snapshot.grantedPermissions
        )
        val updated = if (history ==
            preferences.history
        ) {
            preferences
        } else {
            store.update { it.copy(history = history) }
        }
        AccessEvaluation(updated, snapshot, capabilities)
    }

    suspend fun recordRequest(capability: Capability, permissions: Set<String>) = mutex.withLock {
        store.update {
            it.copy(
                history = it.history.copy(
                    requestedPermissions = it.history.requestedPermissions + permissions,
                    vpnRequested =
                        it.history.vpnRequested || capability == Capability.VpnAuthorization
                )
            )
        }
    }

    /** Settings recovery is inferred only after a completed denial, never on the first check. */
    suspend fun recordResult(results: Map<String, Boolean>, settingsPermissions: Set<String>) =
        mutex.withLock {
            store.update {
                it.copy(
                    history = it.history.copy(
                        requestedPermissions = it.history.requestedPermissions + results.keys,
                        settingsPermissions =
                            (
                                it.history.settingsPermissions -
                                    results.filterValues { granted -> granted }.keys
                                ) +
                                settingsPermissions.intersect(
                                    results.filterValues { granted ->
                                        !granted
                                    }.keys
                                )
                    )
                )
            }
        }

    suspend fun saveOnboarding(step: OnboardingStep) = mutex.withLock {
        store.update { it.copy(onboardingStep = step) }
    }
}
