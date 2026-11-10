package dev.scadangers.access

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.scadangers.core.security.capability.AccessHistory
import dev.scadangers.core.security.capability.AccessPreferences
import dev.scadangers.core.security.capability.AccessStore
import dev.scadangers.core.security.capability.Capability
import dev.scadangers.core.security.capability.OnboardingStep
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// One application-scoped instance, first accessed from a ViewModel coroutine after activity creation.
private val Context.accessDataStore: DataStore<Preferences> by preferencesDataStore(name = "access")

@Singleton
internal class AndroidAccessStore @Inject constructor(
    @ApplicationContext private val context: Context
) : AccessStore {
    override suspend fun read(): AccessPreferences = decode(context.accessDataStore.data.first())

    override suspend fun update(
        transform: (AccessPreferences) -> AccessPreferences
    ): AccessPreferences {
        val preferences = context.accessDataStore.edit { stored ->
            val updated = transform(decode(stored))
            stored[STEP] = updated.onboardingStep.name
            stored[REQUESTED] = updated.history.requestedPermissions
            stored[SETTINGS] = updated.history.settingsPermissions
            stored[GRANTED] = updated.history.previouslyGranted.map { it.name }.toSet()
            stored[VPN_REQUESTED] = updated.history.vpnRequested.toString()
        }
        return decode(preferences)
    }

    private fun decode(preferences: Preferences): AccessPreferences = AccessPreferences(
        onboardingStep =
            OnboardingStep.entries.firstOrNull { it.name == preferences[STEP] }
                ?: OnboardingStep.Welcome,
        history = AccessHistory(
            requestedPermissions = preferences[REQUESTED].orEmpty(),
            settingsPermissions = preferences[SETTINGS].orEmpty(),
            previouslyGranted = Capability.entries.filter {
                it.name in
                    preferences[GRANTED].orEmpty()
            }.toSet(),
            vpnRequested = preferences[VPN_REQUESTED] == "true"
        )
    )

    private companion object {
        val STEP = stringPreferencesKey("onboarding_step")
        val REQUESTED = stringSetPreferencesKey("requested_permissions")
        val SETTINGS = stringSetPreferencesKey("settings_permissions")
        val GRANTED = stringSetPreferencesKey("previously_granted")
        val VPN_REQUESTED = stringPreferencesKey("vpn_requested")
    }
}
