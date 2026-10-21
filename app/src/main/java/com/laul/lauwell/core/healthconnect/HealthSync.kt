package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/** How many days back the store is kept: the longest chart range (3 months = 13 weeks). */
const val SyncWindowDays = 91L

/**
 * Copies Health Connect's daily summaries into the [store], incrementally.
 *
 * Per metric: the first sync (or one after the changes token expired) takes a token, then
 * refreshes the whole window of [SyncWindowDays]. Later syncs ask Health Connect what changed
 * since the token and refresh only the days those records fall on, or the whole window when
 * records were deleted (a deletion has no day). Edits older than the window are ignored.
 *
 * Only metrics whose permissions are granted are read; one sync runs at a time.
 */
class HealthSync(
    private val source: HealthDataSource,
    private val store: SummaryStore,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend fun sync(metrics: List<MetricDefinition>): SyncResult = mutex.withLock {
        val synced = metrics.filter { it.healthConnectTypes.isNotEmpty() }
        if (synced.isEmpty()) return SyncResult()
        if (source.availability() != HealthConnectAvailability.Available) return SyncResult(unavailable = true)
        val granted = source.grantedPermissions()
        val (allowed, refused) = synced.partition { granted.containsAll(readPermissions(listOf(it))) }
        val failed = mutableSetOf<MetricId>()
        allowed.forEach { metric ->
            try {
                syncOne(metric)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Permission revoked mid-sync, rate limit, Health Connect busy: the next sync retries.
                failed += metric.id
            }
        }
        SyncResult(
            synced = allowed.map { it.id }.toSet() - failed,
            notGranted = refused.map { it.id }.toSet(),
            failed = failed,
        )
    }

    private suspend fun syncOne(metric: MetricDefinition) {
        val today = LocalDate.now(clock)
        val window = today.minusDays(SyncWindowDays - 1)..today
        val token = store.syncState(metric.id)?.changesToken
        val changes = token?.let { source.changes(metric, it) }

        val (days, nextToken) = when (changes) {
            null, HealthChanges.TokenExpired -> window to source.changesToken(metric) // token first: nothing written meanwhile is lost
            is HealthChanges.Changed -> {
                val changedDays = changes.days.filter { it in window }
                when {
                    changes.hadDeletions -> window
                    changedDays.isEmpty() -> null
                    else -> changedDays.min()..changedDays.max()
                } to changes.nextToken
            }
        }
        if (days != null) store.replace(metric.id, days, source.dailySummaries(metric, days))
        store.saveSyncState(metric.id, SyncState(nextToken, clock.instant()))
    }
}

/**
 * What a [HealthSync.sync] did, per metric. Metrics without Health Connect types are in none of
 * the sets. [unavailable]: Health Connect can't be used on this phone (or needs an update), so
 * nothing was read.
 */
data class SyncResult(
    val unavailable: Boolean = false,
    val synced: Set<MetricId> = emptySet(),
    val notGranted: Set<MetricId> = emptySet(),
    val failed: Set<MetricId> = emptySet(),
)
