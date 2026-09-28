package com.laul.lauwell.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.Source
import java.time.Instant

/**
 * Local copy of every measurement. Room is the source of truth: screens read only from here, and
 * sources (Health Connect, FHIR, manual entry) write into it.
 *
 * The unique index makes re-syncs idempotent: the same source record replaces its previous copy.
 * SQLite treats NULLs as distinct in a unique index, so sources must give every synced value a
 * stable [sourceRecordId] (see HealthConnectSource); only manual entries may leave it null.
 */
@Entity(
    tableName = "measurement",
    indices = [
        Index(value = ["sourceSystem", "sourceRecordId", "metric"], unique = true),
        Index(value = ["metric", "startEpochMs"]),
    ],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val value: Double,
    val sourceSystem: String,
    val sourceRecordId: String?,
) {
    fun toModel() = Measurement(
        metric = Metric.valueOf(metric),
        start = Instant.ofEpochMilli(startEpochMs),
        end = Instant.ofEpochMilli(endEpochMs),
        value = value,
        source = Source(sourceSystem, sourceRecordId),
    )

    companion object {
        fun from(m: Measurement) = MeasurementEntity(
            metric = m.metric.name,
            startEpochMs = m.start.toEpochMilli(),
            endEpochMs = m.end.toEpochMilli(),
            value = m.value,
            sourceSystem = m.source.system,
            sourceRecordId = m.source.recordId,
        )
    }
}
