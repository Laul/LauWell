package com.laul.lauwell.core.data.repository

import com.laul.lauwell.core.data.db.MeasurementDao
import com.laul.lauwell.core.data.db.MeasurementEntity
import com.laul.lauwell.core.data.source.HealthDataSource
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** A metric that failed to sync, so the UI can say which one and why. */
data class SyncFailure(val metric: Metric, val system: String, val error: Throwable)

/**
 * The only way screens get data. Reads come from Room; [sync] pulls from sources into Room.
 */
class MeasurementRepository(
    private val dao: MeasurementDao,
    private val sources: List<HealthDataSource>,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val syncLock = Mutex()

    /** Live view of [metric] in [from, to). Re-emits whenever a sync writes. */
    fun observe(metric: Metric, from: Instant, to: Instant): Flow<List<Measurement>> =
        dao.observe(metric.name, from.toEpochMilli(), to.toEpochMilli()).map { rows -> rows.map(MeasurementEntity::toModel) }

    /**
     * Pulls [metrics] from every source that supports them, from [since] (or just before the last
     * stored value, if later) until now. One sync runs at a time; a second call waits for the first.
     * One failing metric doesn't stop the others.
     */
    suspend fun sync(metrics: Collection<Metric>, since: Instant): List<SyncFailure> = syncLock.withLock {
        val now = clock.instant()
        val failures = mutableListOf<SyncFailure>()
        for (source in sources) {
            for (metric in metrics.filter(source::supports)) {
                // Re-read a little before the newest stored value: the current hour's steps keep growing.
                val resumeAt = dao.latestStart(metric.name, source.system)
                    ?.let { maxOf(since, Instant.ofEpochMilli(it).minus(RESYNC_OVERLAP)) }
                    ?: since
                try {
                    dao.upsert(source.read(metric, resumeAt, now).map(MeasurementEntity::from))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures += SyncFailure(metric, source.system, e)
                }
            }
        }
        failures
    }

    private companion object {
        val RESYNC_OVERLAP: Duration = Duration.ofHours(2)
    }
}
