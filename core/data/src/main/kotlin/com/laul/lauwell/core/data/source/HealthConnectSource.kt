package com.laul.lauwell.core.data.source

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.Source
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Reads from Health Connect.
 *
 * Health Connect only returns data from about 30 days before the user first granted access (more
 * needs the history permission). Anything older must already be in the local database, which is
 * why LauWell keeps its own copy.
 */
class HealthConnectSource(private val client: HealthConnectClient) : HealthDataSource {

    override val system: String = Source.HEALTH_CONNECT

    override fun supports(metric: Metric): Boolean = metric in RECORD_TYPES

    /** Read permission for [metric]. Request per metric so one refusal doesn't block the others. */
    fun permissionFor(metric: Metric): String = HealthPermission.getReadPermission(RECORD_TYPES.getValue(metric))

    suspend fun grantedMetrics(): Set<Metric> {
        val granted = client.permissionController.getGrantedPermissions()
        return RECORD_TYPES.keys.filterTo(mutableSetOf()) { permissionFor(it) in granted }
    }

    override suspend fun read(metric: Metric, from: Instant, to: Instant): List<Measurement> = when (metric) {
        Metric.HEART_RATE -> readAll(HeartRateRecord::class, from, to).flatMap { record ->
            // One record holds many samples; each sample gets its own stable id.
            record.samples.mapIndexed { i, sample ->
                measurement(metric, sample.time, sample.beatsPerMinute.toDouble(), "${record.metadata.id}#$i")
            }
        }

        Metric.BLOOD_GLUCOSE -> readAll(BloodGlucoseRecord::class, from, to).map {
            measurement(metric, it.time, it.level.inMillimolesPerLiter, it.metadata.id)
        }

        Metric.BLOOD_PRESSURE_SYSTOLIC -> readAll(BloodPressureRecord::class, from, to).map {
            measurement(metric, it.time, it.systolic.inMillimetersOfMercury, it.metadata.id)
        }

        Metric.BLOOD_PRESSURE_DIASTOLIC -> readAll(BloodPressureRecord::class, from, to).map {
            measurement(metric, it.time, it.diastolic.inMillimetersOfMercury, it.metadata.id)
        }

        Metric.STEPS -> hourlySteps(from, to)
    }

    /** Follows `pageToken` until the end. TrackAid stopped at one page and lost data silently. */
    private suspend fun <T : Record> readAll(type: KClass<T>, from: Instant, to: Instant): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    pageSize = PAGE_SIZE,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    /**
     * Steps as hourly totals. Health Connect de-duplicates overlapping sources (phone + watch) in
     * aggregates, which raw records don't. Hourly buckets never cross midnight, so daily totals
     * are exact in any time zone.
     */
    private suspend fun hourlySteps(from: Instant, to: Instant): List<Measurement> =
        client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from, to),
                timeRangeSlicer = Duration.ofHours(1),
            ),
        ).mapNotNull { bucket ->
            bucket.result[StepsRecord.COUNT_TOTAL]?.let { count ->
                Measurement(
                    metric = Metric.STEPS,
                    start = bucket.startTime,
                    end = bucket.endTime,
                    value = count.toDouble(),
                    // Aggregates have no record id; the bucket start is stable across syncs.
                    source = Source(system, "steps-hour-${bucket.startTime.toEpochMilli()}"),
                )
            }
        }

    private fun measurement(metric: Metric, time: Instant, value: Double, recordId: String) =
        Measurement(metric, start = time, value = value, source = Source(system, recordId))

    companion object {
        private const val PAGE_SIZE = 1000

        val RECORD_TYPES: Map<Metric, KClass<out Record>> = mapOf(
            Metric.HEART_RATE to HeartRateRecord::class,
            Metric.BLOOD_GLUCOSE to BloodGlucoseRecord::class,
            Metric.STEPS to StepsRecord::class,
            Metric.BLOOD_PRESSURE_SYSTOLIC to BloodPressureRecord::class,
            Metric.BLOOD_PRESSURE_DIASTOLIC to BloodPressureRecord::class,
        )
    }
}
