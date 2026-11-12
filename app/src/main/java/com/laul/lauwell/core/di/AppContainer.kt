package com.laul.lauwell.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.DelegatingWorkerFactory
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.google.firebase.auth.FirebaseAuth
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.core.auth.AuthRepository
import com.laul.lauwell.core.auth.FirebaseAuthRepository
import com.laul.lauwell.core.catalog.EverythingOnToggleRepository
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.data.LauWellDatabase
import com.laul.lauwell.core.data.RoomSummaryStore
import com.laul.lauwell.core.healthconnect.BackgroundSync
import com.laul.lauwell.core.healthconnect.HealthConnectDataSource
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.healthconnect.HealthSyncWorkerFactory
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.WorkManagerSyncSchedule
import com.laul.lauwell.core.data.RoomRecordRepository
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.summary.DailySummarySource
import com.laul.lauwell.core.summary.DayHistory
import com.laul.lauwell.core.settings.DataStoreDisplayPreferences
import com.laul.lauwell.core.settings.DataStoreOnboarding
import com.laul.lauwell.core.settings.DataStorePinnedMetrics
import com.laul.lauwell.core.settings.DisplayPreferences
import com.laul.lauwell.core.settings.Onboarding
import com.laul.lauwell.core.settings.PinnedMetrics
import com.laul.lauwell.core.sync.EntrySync
import com.laul.lauwell.core.sync.EntrySyncSchedule
import com.laul.lauwell.core.sync.EntrySyncWorkerFactory
import com.laul.lauwell.core.sync.HttpSyncApi
import com.laul.lauwell.core.sync.WorkManagerEntrySyncSchedule
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

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

    val onboarding: Onboarding by lazy { DataStoreOnboarding(settingsDataStore) }

    val healthDataSource: HealthDataSource by lazy { HealthConnectDataSource(appContext) }

    /**
     * The Health Connect cache and what you log. Every version change migrates (see [LauWellDatabase]);
     * only version 1, the cache alone, is dropped on upgrade.
     */
    private val database: LauWellDatabase by lazy {
        Room.databaseBuilder(appContext, LauWellDatabase::class.java, "lauwell.db")
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .build()
    }

    val summaryStore: SummaryStore by lazy { RoomSummaryStore(database.healthCacheDao()) }

    val healthSync: HealthSync by lazy { HealthSync(healthDataSource, summaryStore, Clock.systemDefaultZone()) }

    /** Writes and work that outlive a screen (a save finishing after its sheet closed). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** What you enter yourself (mood…), on the phone first, then synced to Postgres ([entrySync]). */
    val recordRepository: RecordRepository by lazy {
        RoomRecordRepository(database.recordDao(), authRepository.session, appScope, entrySyncSchedule, Clock.systemUTC())
    }

    /** Daily summaries for any metric: Health Connect's cache, or your entries for a manual one. */
    val dailySummaries: DailySummarySource by lazy { DailySummarySource(summaryStore, recordRepository) }

    /** A metric's days over the whole past its charts scroll through, loading older ones on demand. */
    val dayHistory: DayHistory by lazy {
        DayHistory(dailySummaries, summaryStore, healthDataSource, recordRepository, Clock.systemDefaultZone())
    }

    /** WorkManager's own singleton, set up with [workerFactory] (see [com.laul.lauwell.LauWellApplication]). */
    private val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    val backgroundSync: BackgroundSync by lazy {
        BackgroundSync(
            healthSync,
            healthDataSource,
            toggleRepository,
            authRepository.session,
            WorkManagerSyncSchedule(workManager),
        )
    }

    /**
     * What you log, kept in step with Postgres through the sync API. The ID token comes from Firebase,
     * which caches and refreshes it; it never leaves this call.
     */
    val entrySync: EntrySync by lazy {
        val api = HttpSyncApi(BuildConfig.SYNC_API_URL) { forceRefresh ->
            firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        }
        EntrySync(database.recordDao(), api, authRepository.session, Clock.systemUTC())
    }

    val entrySyncSchedule: EntrySyncSchedule by lazy { WorkManagerEntrySyncSchedule(workManager) }

    /** Lets WorkManager build our workers; their dependencies are only built when a worker runs. */
    val workerFactory: WorkerFactory = DelegatingWorkerFactory().apply {
        addFactory(HealthSyncWorkerFactory { backgroundSync })
        addFactory(EntrySyncWorkerFactory { entrySync })
    }
}
