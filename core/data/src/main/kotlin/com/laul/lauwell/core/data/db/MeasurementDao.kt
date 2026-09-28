package com.laul.lauwell.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    /** Measurements of [metric] starting in [fromMs, toMs), oldest first. Emits again on every write. */
    @Query(
        "SELECT * FROM measurement WHERE metric = :metric AND startEpochMs >= :fromMs AND startEpochMs < :toMs " +
            "ORDER BY startEpochMs",
    )
    fun observe(metric: String, fromMs: Long, toMs: Long): Flow<List<MeasurementEntity>>

    /**
     * REPLACE on the unique (source, record, metric) index, not @Upsert: @Upsert matches on
     * the auto-generated primary key and would duplicate re-synced rows.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<MeasurementEntity>)

    /** Newest stored start time for a metric from a source; sync resumes from here. */
    @Query("SELECT MAX(startEpochMs) FROM measurement WHERE metric = :metric AND sourceSystem = :system")
    suspend fun latestStart(metric: String, system: String): Long?
}
