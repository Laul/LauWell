package com.laul.lauwell.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Onboarding steps already shown on this phone. The Health Connect grant screen opens once after
 * sign-in (design: sign-in → Health Connect grant → Today); answering it either way marks it seen,
 * and Settings can reopen it. Not health data.
 */
interface Onboarding {
    val healthConnectPromptSeen: Flow<Boolean>
    suspend fun markHealthConnectPromptSeen()
}

/** [Onboarding] in the app's Preferences DataStore. */
class DataStoreOnboarding(private val dataStore: DataStore<Preferences>) : Onboarding {

    override val healthConnectPromptSeen: Flow<Boolean> =
        dataStore.data.map { it[HealthConnectPromptSeenKey] ?: false }

    override suspend fun markHealthConnectPromptSeen() {
        dataStore.edit { it[HealthConnectPromptSeenKey] = true }
    }

    private companion object {
        val HealthConnectPromptSeenKey = booleanPreferencesKey("health_connect_prompt_seen")
    }
}
