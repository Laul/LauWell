package com.laul.lauwell.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Asks for an [EntrySync] run as soon as the phone is online. Implementation: [WorkManagerEntrySyncSchedule]. */
fun interface EntrySyncSchedule {
    fun request()
}

/**
 * One unique [EntrySyncWorker] at a time, run when a network is available. A request made while a run is
 * going is appended after it, so a change saved during a push is sent by the next run.
 */
class WorkManagerEntrySyncSchedule(private val workManager: WorkManager) : EntrySyncSchedule {

    override fun request() {
        val work = OneTimeWorkRequestBuilder<EntrySyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WorkName, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    private companion object {
        const val WorkName = "entry-sync"
    }
}

/**
 * One [EntrySync] run, after [prepare] (the one-time Firestore import, until Firestore is removed). Network
 * trouble and server errors (5xx) retry with backoff; a 4xx other than an expired sign-in is a bug that a
 * retry won't fix, so it stops until the next request. A failed [prepare] doesn't hold back the sync, and
 * retries with it.
 */
class EntrySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val sync: EntrySync,
    private val prepare: suspend () -> Unit,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prepared = try {
            prepare()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        sync.run()
        if (prepared) Result.success() else Result.retry()
    } catch (e: NotSignedInException) {
        Result.success()
    } catch (e: SyncHttpException) {
        if (e.status >= 500) Result.retry() else Result.failure()
    } catch (e: IOException) {
        Result.retry()
    }
}

/** Builds [EntrySyncWorker] with its dependency from the `AppContainer`; `null` for any other class. */
class EntrySyncWorkerFactory(
    private val sync: () -> EntrySync,
    private val prepare: suspend () -> Unit = {},
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        EntrySyncWorker::class.java.name -> EntrySyncWorker(appContext, workerParameters, sync(), prepare)
        else -> null
    }
}
