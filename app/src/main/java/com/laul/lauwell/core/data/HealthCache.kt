package com.laul.lauwell.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.SyncState
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

// The Room cache of Health Connect data (CLAUDE.md → Stack): daily summaries and sync state only.
// Raw samples stay in Health Connect and are read when a screen needs them.

/**
 * One quantity of one metric on one local day. [value] is the total, mean, average or last value
 * (by [aggregation]); [min] and [max] are set for a range, [lastAt] for a latest value.
 * [aggregation] stores the enum's name: never rename an [Aggregation] entry once shipped.
 * [origins] holds the writing apps' packages, comma-separated, repeated on each quantity of the day.
 */
@Entity(tableName = "daily_summary_value", primaryKeys = ["metric", "date", "quantity"])
data class DailySummaryValueEntity(
    val metric: String,
    /** Local date as epoch day. */
    val date: Long,
    val quantity: String,
    val aggregation: String,
    val value: Double,
    val min: Double? = null,
    val max: Double? = null,
    val count: Int? = null,
    /** Epoch milliseconds. */
    val lastAt: Long? = null,
    val origins: String,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val metric: String,
    val changesToken: String,
    /** Epoch milliseconds. */
    val syncedAt: Long,
)

@Dao
abstract class HealthCacheDao {

    @Query("SELECT * FROM daily_summary_value WHERE metric = :metric AND date BETWEEN :from AND :to ORDER BY date, quantity")
    abstract fun values(metric: String, from: Long, to: Long): Flow<List<DailySummaryValueEntity>>

    @Query("DELETE FROM daily_summary_value WHERE metric = :metric AND date BETWEEN :from AND :to")
    protected abstract suspend fun deleteValues(metric: String, from: Long, to: Long)

    @Insert
    protected abstract suspend fun insertValues(values: List<DailySummaryValueEntity>)

    /** Deletes then inserts in one transaction, so a reader never sees the days half-written. */
    @Transaction
    open suspend fun replaceValues(metric: String, from: Long, to: Long, values: List<DailySummaryValueEntity>) {
        deleteValues(metric, from, to)
        insertValues(values)
    }

    @Query("SELECT * FROM sync_state WHERE metric = :metric")
    abstract suspend fun syncState(metric: String): SyncStateEntity?

    @Query("SELECT syncedAt FROM sync_state WHERE metric = :metric")
    abstract fun syncedAt(metric: String): Flow<Long?>

    @Upsert
    abstract suspend fun saveSyncState(state: SyncStateEntity)
}

/** [SummaryStore] in the app's Room database. */
class RoomSummaryStore(private val dao: HealthCacheDao) : SummaryStore {

    override fun summaries(metric: MetricId, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> =
        dao.values(metric.value, days.start.toEpochDay(), days.endInclusive.toEpochDay()).map(::toSummaries)

    override suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>) {
        require(summaries.all { it.metric == metric && it.date in days }) { "summaries outside $metric $days" }
        dao.replaceValues(metric.value, days.start.toEpochDay(), days.endInclusive.toEpochDay(), summaries.flatMap(::toEntities))
    }

    override suspend fun syncState(metric: MetricId): SyncState? =
        dao.syncState(metric.value)?.let { SyncState(it.changesToken, Instant.ofEpochMilli(it.syncedAt)) }

    override suspend fun saveSyncState(metric: MetricId, state: SyncState) =
        dao.saveSyncState(SyncStateEntity(metric.value, state.changesToken, state.syncedAt.toEpochMilli()))

    override fun lastSyncedAt(metric: MetricId): Flow<Instant?> =
        dao.syncedAt(metric.value).map { it?.let(Instant::ofEpochMilli) }
}

/** One row per quantity of [summary]. */
internal fun toEntities(summary: DailySummary): List<DailySummaryValueEntity> {
    val origins = summary.origins.map { it.appPackage }.sorted().joinToString(",")
    return summary.values.map { (quantity, dayValue) ->
        val base = DailySummaryValueEntity(
            metric = summary.metric.value,
            date = summary.date.toEpochDay(),
            quantity = quantity,
            aggregation = dayValue.aggregation.name,
            value = 0.0,
            origins = origins,
        )
        when (dayValue) {
            is DayValue.Total -> base.copy(value = dayValue.value)
            is DayValue.Mean -> base.copy(value = dayValue.value, count = dayValue.count)
            is DayValue.Range -> base.copy(value = dayValue.average, min = dayValue.min, max = dayValue.max)
            is DayValue.Last -> base.copy(value = dayValue.value, lastAt = dayValue.at.toEpochMilli())
        }
    }
}

/** Rows back into summaries, one per metric and day, in the rows' order. */
internal fun toSummaries(rows: List<DailySummaryValueEntity>): List<DailySummary> =
    rows.groupBy { it.metric to it.date }.map { (key, dayRows) ->
        DailySummary(
            metric = MetricId(key.first),
            date = LocalDate.ofEpochDay(key.second),
            values = dayRows.associate { it.quantity to it.toDayValue() },
            origins = dayRows.first().origins.split(",").filter { it.isNotEmpty() }.mapTo(mutableSetOf(), ::DataOrigin),
        )
    }

private fun DailySummaryValueEntity.toDayValue(): DayValue = when (Aggregation.valueOf(aggregation)) {
    Aggregation.Sum -> DayValue.Total(value)
    Aggregation.Average -> DayValue.Mean(value, count)
    Aggregation.MinAverageMax -> DayValue.Range(checkNotNull(min), value, checkNotNull(max))
    Aggregation.Latest -> DayValue.Last(value, Instant.ofEpochMilli(checkNotNull(lastAt)))
}
