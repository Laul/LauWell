package com.laul.lauwell.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.core.auth.AuthRepository
import com.laul.lauwell.core.auth.FirebaseAuthRepository
import com.laul.lauwell.core.catalog.EverythingOnToggleRepository
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.healthconnect.HealthConnectDataSource
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.settings.DataStoreDisplayPreferences
import com.laul.lauwell.core.settings.DataStorePinnedMetrics
import com.laul.lauwell.core.settings.DisplayPreferences
import com.laul.lauwell.core.settings.PinnedMetrics

/**
 * Manual dependency injection: the one place that builds the app's process-wide singletons
 * (see PLAN.md → Decisions → Dependency injection).
 *
 * Created once in [com.laul.lauwell.LauWellApplication.onCreate]. Everything is `by lazy`, so a
 * dependency is only built the first time something asks for it. Classes never reach in here
 * themselves: they receive what they need through their constructor, and ViewModels get it via
 * [appViewModel].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val credentialManager: CredentialManager by lazy { CredentialManager.create(appContext) }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(firebaseAuth, credentialManager, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }

    /** What the user switched off. MVP: nothing, see [EverythingOnToggleRepository]. */
    val toggleRepository: ToggleRepository = EverythingOnToggleRepository

    /**
     * The app's one Preferences DataStore (`settings.preferences_pb`): display preferences and the
     * pinned set now, the Settings toggles later. DataStore allows a single instance per file, hence here.
     */
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") }
    }

    val displayPreferences: DisplayPreferences by lazy { DataStoreDisplayPreferences(settingsDataStore) }

    val pinnedMetrics: PinnedMetrics by lazy { DataStorePinnedMetrics(settingsDataStore) }

    val healthDataSource: HealthDataSource by lazy { HealthConnectDataSource(appContext) }
}
