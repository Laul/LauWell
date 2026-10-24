package com.laul.lauwell.core.healthconnect

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/** [SyncSchedule] on WorkManager: one unique periodic [HealthSyncWorker], kept across restarts. */
class WorkManagerSyncSchedule(private val workManager: WorkManager) : SyncSchedule {

    override fun start() {
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(PeriodicSyncInterval)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        // UPDATE, not REPLACE: a running period keeps its timing; a changed interval still applies.
        workManager.enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun stop() {
        workManager.cancelUniqueWork(WorkName)
    }

    private companion object {
        const val WorkName = "health-sync"
    }
}

/**
 * The periodic background sync. Always succeeds: a metric that fails is retried by the next run
 * (see [HealthSync]), so WorkManager has nothing to back off from.
 */
class HealthSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val backgroundSync: BackgroundSync,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        backgroundSync.runPeriodic()
        return Result.success()
    }
}

/**
 * Builds [HealthSyncWorker] with its dependency from the `AppContainer` (manual DI); `null` for any
 * other class lets WorkManager fall back to its default.
 */
class HealthSyncWorkerFactory(private val backgroundSync: () -> BackgroundSync) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        HealthSyncWorker::class.java.name -> HealthSyncWorker(appContext, workerParameters, backgroundSync())
        else -> null
    }
}
