package com.laul.lauwell.core.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.DeviceType
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import com.laul.lauwell.core.record.SourceDevice
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Health Connect records and aggregates → our record model. Pure functions, so they are unit-tested
// without a device; [HealthConnectDataSource] only adds the client calls. A new synced metric adds a
// branch to [toMeasurements], [localDays], [aggregateMetricsFor] and [dailySummary]; `HealthConnectMappingTest`
// fails for a registered metric that has Health Connect types but no mapping.

private val HeartRate = CardiovascularMetrics.HeartRate.id
private val RestingHeartRate = CardiovascularMetrics.RestingHeartRate.id
private val BloodPressure = CardiovascularMetrics.BloodPressure.id

/** [record] as [Measurement]s of [metric]: one per sample for a series, one otherwise. */
internal fun toMeasurements(metric: MetricId, record: Record): List<Measurement> = when (record) {
    is HeartRateRecord -> record.samples.mapIndexed { index, sample ->
        Measurement(
            id = RecordId("${record.metadata.id}#$index"),
            metric = metric,
            time = sample.time,
            zoneOffset = record.startZoneOffset,
            values = mapOf("value" to sample.beatsPerMinute.toDouble()),
            provenance = provenanceOf(record.metadata),
        )
    }

    is RestingHeartRateRecord -> listOf(
        Measurement(
            id = RecordId(record.metadata.id),
            metric = metric,
            time = record.time,
            zoneOffset = record.zoneOffset,
            values = mapOf("value" to record.beatsPerMinute.toDouble()),
            provenance = provenanceOf(record.metadata),
        ),
    )

    is BloodPressureRecord -> listOf(
        Measurement(
            id = RecordId(record.metadata.id),
            metric = metric,
            time = record.time,
            zoneOffset = record.zoneOffset,
            values = mapOf(
                "systolic" to record.systolic.inMillimetersOfMercury,
                "diastolic" to record.diastolic.inMillimetersOfMercury,
            ),
            provenance = provenanceOf(record.metadata),
        ),
    )

    else -> error("No mapping for ${record::class.simpleName} ($metric)")
}

/** What a synced record says about where it came from. [Provenance.editedAt] stays null: Health Connect only keeps the last change. */
internal fun provenanceOf(metadata: Metadata): Provenance = Provenance(
    source = Source.Device,
    origin = DataOrigin(metadata.dataOrigin.packageName, metadata.device?.toSourceDevice()),
    recordingMethod = when (metadata.recordingMethod) {
        Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> RecordingMethod.Automatic
        Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> RecordingMethod.Active
        Metadata.RECORDING_METHOD_MANUAL_ENTRY -> RecordingMethod.Manual
        else -> RecordingMethod.Unknown
    },
    recordedAt = metadata.lastModifiedTime,
)

private fun Device.toSourceDevice() = SourceDevice(
    manufacturer = manufacturer,
    model = model,
    type = when (type) {
        Device.TYPE_WATCH -> DeviceType.Watch
        Device.TYPE_PHONE -> DeviceType.Phone
        Device.TYPE_SCALE -> DeviceType.Scale
        Device.TYPE_RING -> DeviceType.Ring
        Device.TYPE_HEAD_MOUNTED -> DeviceType.HeadMounted
        Device.TYPE_FITNESS_BAND -> DeviceType.FitnessBand
        Device.TYPE_CHEST_STRAP -> DeviceType.ChestStrap
        Device.TYPE_SMART_DISPLAY -> DeviceType.SmartDisplay
        else -> DeviceType.Unknown
    },
)

/** The aggregates [dailySummary] reads for [metric]; empty for a metric Health Connect doesn't hold. */
internal fun aggregateMetricsFor(metric: MetricDefinition): Set<AggregateMetric<*>> = when (metric.id) {
    HeartRate -> setOf(HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX)
    RestingHeartRate -> setOf(RestingHeartRateRecord.BPM_AVG)
    BloodPressure -> setOf(BloodPressureRecord.SYSTOLIC_AVG, BloodPressureRecord.DIASTOLIC_AVG)
    else -> {
        check(metric.healthConnectTypes.isEmpty()) { "No aggregation mapping for ${metric.id}" }
        emptySet()
    }
}

/**
 * One day of Health Connect's aggregation as a [DailySummary], or null when the day has no data
 * (Health Connect still returns a group for it, without values).
 */
internal fun dailySummary(metric: MetricDefinition, day: AggregationResultGroupedByPeriod): DailySummary? {
    val result = day.result
    val values: Map<String, DayValue> = when (metric.id) {
        HeartRate -> {
            val min = result[HeartRateRecord.BPM_MIN] ?: return null
            val avg = result[HeartRateRecord.BPM_AVG] ?: return null
            val max = result[HeartRateRecord.BPM_MAX] ?: return null
            mapOf("value" to DayValue.Range(min.toDouble(), avg.toDouble(), max.toDouble()))
        }

        RestingHeartRate -> {
            val avg = result[RestingHeartRateRecord.BPM_AVG] ?: return null
            mapOf("value" to DayValue.Mean(avg.toDouble()))
        }

        BloodPressure -> {
            val systolic = result[BloodPressureRecord.SYSTOLIC_AVG] ?: return null
            val diastolic = result[BloodPressureRecord.DIASTOLIC_AVG] ?: return null
            mapOf(
                "systolic" to DayValue.Mean(systolic.inMillimetersOfMercury),
                "diastolic" to DayValue.Mean(diastolic.inMillimetersOfMercury),
            )
        }

        else -> error("No aggregation mapping for ${metric.id}")
    }
    return DailySummary(
        metric = metric.id,
        date = day.startTime.toLocalDate(),
        values = values,
        origins = result.dataOrigins.mapTo(mutableSetOf()) { DataOrigin(it.packageName) },
    )
}

/**
 * The local days [record] falls on, each end in its own offset, as the daily aggregation places
 * it: one for a reading, every day from start to end for a series (a night's heart rate spans two).
 * [fallback] is used when the writing app gave no offset.
 */
internal fun localDays(record: Record, fallback: ZoneId): Set<LocalDate> = when (record) {
    is HeartRateRecord -> {
        val first = localDate(record.startTime, record.startZoneOffset, fallback)
        val last = localDate(record.endTime, record.endZoneOffset, fallback)
        generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.toSet()
    }
    is RestingHeartRateRecord -> setOf(localDate(record.time, record.zoneOffset, fallback))
    is BloodPressureRecord -> setOf(localDate(record.time, record.zoneOffset, fallback))
    // Health Connect's instant / interval interfaces are internal, hence one branch per record type.
    else -> error("No mapping for ${record::class.simpleName}")
}

private fun localDate(time: Instant, offset: ZoneOffset?, fallback: ZoneId): LocalDate =
    time.atZone(offset ?: fallback).toLocalDate()

/** Reads every page: [fetch] gets the previous page's token (null first) and returns its items and the next token. */
internal suspend fun <T> readAllPages(fetch: suspend (pageToken: String?) -> Pair<List<T>, String?>): List<T> {
    val all = mutableListOf<T>()
    var token: String? = null
    do {
        val (items, next) = fetch(token)
        all += items
        token = next?.takeIf { it.isNotEmpty() }
    } while (token != null)
    return all
}
