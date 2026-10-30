package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.SyncWindowDays
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.RecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/**
 * A metric's days over the whole past its charts can scroll through (Notion → Decisions Tracker:
 * *Charts are built from time-stamped series…*). The chart groups them further (weeks…) itself.
 *
 * A synced metric: the local cache holds the sync window ([SyncWindowDays]) kept fresh by `HealthSync`;
 * older days are read from Health Connect when asked ([load]), a chunk at a time, and kept in the
 * cache, so a second visit shows them at once. A logged metric: your entries are all in your
 * account already, nothing to load.
 */
class DayHistory(
    private val summaries: DailySummarySource,
    private val store: SummaryStore,
    private val source: HealthDataSource,
    private val records: RecordRepository,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    /** Per metric, the oldest day read from Health Connect in this run; the sync window before any load. */
    private val loadedFrom = mutableMapOf<MetricId, LocalDate>()

    /** [metric]'s days in [days], oldest first, updated live (a sync, a load, a new entry). */
    fun days(metric: MetricDefinition, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> =
        summaries.summaries(metric, days)

    /**
     * The first day [metric] has data, or null with none: where scrolling stops. A night counts on
     * the morning it ends, as its summaries do.
     */
    suspend fun firstDay(metric: MetricDefinition): LocalDate? = try {
        if (isSynced(metric)) {
            source.oldestRecordTime(metric)?.atZone(clock.zone)?.toLocalDate()
                ?.let { if (metric.nightly) it.plusDays(1) else it }
        } else {
            (records.measurements(metric.id).first() as? AppResult.Success)?.data
                ?.minOfOrNull { it.time }?.atZone(clock.zone)?.toLocalDate()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null // Not readable (no permission, Health Connect busy): only the cached days show.
    }

    /**
     * Makes sure [metric]'s days from [from] onward are in the cache, reading from Health Connect the
     * ones older than what is already there, in one call. Nothing to do for a logged metric, or for
     * days inside the sync window. Fails (and loads nothing) without access to Health Connect.
     */
    suspend fun load(metric: MetricDefinition, from: LocalDate): AppResult<Unit> = mutex.withLock {
        if (!isSynced(metric)) return AppResult.Success(Unit)
        val oldestLoaded = loadedFrom[metric.id] ?: LocalDate.now(clock).minusDays(SyncWindowDays - 1)
        if (!from.isBefore(oldestLoaded)) return AppResult.Success(Unit)
        val chunk = from..oldestLoaded.minusDays(1)
        try {
            store.replace(metric.id, chunk, source.dailySummaries(metric, chunk))
            loadedFrom[metric.id] = from
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(e, "Couldn't read older ${metric.label.lowercase()} data")
        }
    }

    private fun isSynced(metric: MetricDefinition) = metric.healthConnectTypes.isNotEmpty()
}
