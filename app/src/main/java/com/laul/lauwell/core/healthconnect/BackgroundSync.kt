package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.time.Duration

/**
 * How often the background sync runs, at most (WorkManager may run it later, e.g. on low battery).
 * PLAN.md → Decisions → *Health Connect sync triggers*.
 */
val PeriodicSyncInterval: Duration = Duration.ofMinutes(20)

/**
 * Keeps the cache fresh beyond the screens' own syncs: once each time the app comes to the
 * foreground, and periodically ([HealthSyncWorker]) while Health Connect allows background reads
 * ([BackgroundPermission]). Both sync the visible metrics, and only while signed in.
 */
class BackgroundSync(
    private val sync: HealthSync,
    private val source: HealthDataSource,
    private val toggles: ToggleRepository,
    private val session: StateFlow<Session?>,
    private val schedule: SyncSchedule,
) {

    /** The app came to the foreground: sync now, then start or stop the periodic sync to match the grants. */
    suspend fun onForeground() {
        try {
            if (session.value != null) syncVisible()
            updateSchedule()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Health Connect busy or gone mid-call: the next start, or a screen's own sync, retries.
        }
    }

    /** Starts the periodic sync when signed in with background reads granted, stops it otherwise. */
    suspend fun updateSchedule() {
        if (session.value != null && backgroundAllowed()) schedule.start() else schedule.stop()
    }

    /** One run of the periodic sync. Reads nothing once signed out or background reads were taken back. */
    suspend fun runPeriodic() {
        if (session.value != null && backgroundAllowed()) syncVisible()
    }

    private suspend fun syncVisible() {
        sync.sync(MetricRegistry.visible(toggles.switchedOff.first()))
    }

    private suspend fun backgroundAllowed(): Boolean =
        source.availability() == HealthConnectAvailability.Available &&
            BackgroundPermission in source.grantedPermissions()
}

/** The periodic sync's on/off switch. Implementation: [WorkManagerSyncSchedule]. */
interface SyncSchedule {

    /** Runs [BackgroundSync.runPeriodic] every [PeriodicSyncInterval]; already scheduled → kept as is. */
    fun start()

    fun stop()
}
