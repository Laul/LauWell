package com.laul.lauwell.core.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.ActivityMetrics
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.catalog.metrics.SleepMetrics
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

// Health Connect records and aggregates → our record model. Pure functions, so they are unit-tested
// without a device; [HealthConnectDataSource] only adds the client calls. A new synced metric adds a
// branch to [toMeasurements], [localDays], [aggregateMetricsFor] and [dailySummary]; `HealthConnectMappingTest`
// fails for a registered metric that has Health Connect types but no mapping.

private val HeartRate = CardiovascularMetrics.HeartRate.id
private val RestingHeartRate = CardiovascularMetrics.RestingHeartRate.id
private val BloodPressure = CardiovascularMetrics.BloodPressure.id
private val Steps = ActivityMetrics.Steps.id
private val SleepDuration = SleepMetrics.SleepDuration.id

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

    // Raw records are read only for their origins: a day's total comes from the aggregation, which
    // keeps the apps' priority (Decisions → Read first: steps and duplicates).
    is StepsRecord -> listOf(
        Measurement(
            id = RecordId(record.metadata.id),
            metric = metric,
            time = record.startTime,
            zoneOffset = record.startZoneOffset,
            values = mapOf("value" to record.count.toDouble()),
            provenance = provenanceOf(record.metadata),
        ),
    )

    // Read for the origins of the source line; the night's values come from [sleepNight].
    is SleepSessionRecord -> listOf(
        Measurement(
            id = RecordId(record.metadata.id),
            metric = metric,
            time = record.endTime,
            zoneOffset = record.endZoneOffset,
            values = mapOf("value" to (minutes(record.startTime, record.endTime) - stageMinutes(record.stages, AwakeStages))),
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
    Steps -> setOf(StepsRecord.COUNT_TOTAL)
    SleepDuration -> setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL)
    else -> {
        check(metric.healthConnectTypes.isEmpty()) { "No aggregation mapping for ${metric.id}" }
        emptySet()
    }
}

/**
 * One day of Health Connect's aggregation as a [DailySummary], or null when the day has no data
 * (Health Connect still returns a group for it, without values). A nightly metric's groups start at
 * [NightStart] the evening before the day they count on ([nightBounds]).
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

        Steps -> {
            val total = result[StepsRecord.COUNT_TOTAL] ?: return null
            mapOf("value" to DayValue.Total(total.toDouble()))
        }

        SleepDuration -> {
            val total = result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.takeIf { !it.isZero } ?: return null
            mapOf("value" to DayValue.Total(total.toMillis() / 60_000.0))
        }

        else -> error("No aggregation mapping for ${metric.id}")
    }
    return DailySummary(
        metric = metric.id,
        date = day.startTime.toLocalDate().let { if (metric.nightly) it.plusDays(1) else it },
        values = values,
        origins = result.dataOrigins.mapTo(mutableSetOf()) { DataOrigin(it.packageName) },
    )
}

/**
 * The local days [record] falls on, each end in its own offset, as the daily aggregation places
 * it: one for a reading, every day from start to end for an interval (a night's heart rate, or a
 * walk across midnight, spans two).
 * [fallback] is used when the writing app gave no offset.
 */
internal fun localDays(record: Record, fallback: ZoneId): Set<LocalDate> = when (record) {
    is HeartRateRecord -> daysOf(record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset, fallback)
    is StepsRecord -> daysOf(record.startTime, record.startZoneOffset, record.endTime, record.endZoneOffset, fallback)
    is SleepSessionRecord -> setOf(nightOf(record.endTime, record.endZoneOffset, fallback))
    is RestingHeartRateRecord -> setOf(localDate(record.time, record.zoneOffset, fallback))
    is BloodPressureRecord -> setOf(localDate(record.time, record.zoneOffset, fallback))
    // Health Connect's instant / interval interfaces are internal, hence one branch per record type.
    else -> error("No mapping for ${record::class.simpleName}")
}

/** Every local day from an interval's start to its end, each end in its own offset. */
private fun daysOf(start: Instant, startOffset: ZoneOffset?, end: Instant, endOffset: ZoneOffset?, fallback: ZoneId): Set<LocalDate> {
    val first = localDate(start, startOffset, fallback)
    val last = localDate(end, endOffset, fallback)
    return generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.toSet()
}

private fun localDate(time: Instant, offset: ZoneOffset?, fallback: ZoneId): LocalDate =
    time.atZone(offset ?: fallback).toLocalDate()

/** When a nightly metric's day starts, the evening before: a night is never cut at midnight. */
internal val NightStart: LocalTime = LocalTime.of(18, 0)

/** The day a night ending at [end] counts on: the next day once past [NightStart] (an evening nap). */
internal fun nightOf(end: Instant, offset: ZoneOffset?, fallback: ZoneId): LocalDate =
    end.atZone(offset ?: fallback).toLocalDateTime().plusHours(24L - NightStart.hour).toLocalDate()

/**
 * The instants [date] covers for [metric]: midnight to midnight, or for a nightly metric
 * [NightStart] the evening before to [NightStart] that day.
 */
fun dayBounds(metric: MetricDefinition, date: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
    if (metric.nightly) {
        date.minusDays(1).atTime(NightStart).atZone(zone).toInstant() to date.atTime(NightStart).atZone(zone).toInstant()
    } else {
        date.atStartOfDay(zone).toInstant() to date.plusDays(1).atStartOfDay(zone).toInstant()
    }

/**
 * A night's [summary], from the aggregation (which applies the apps' priority), completed from the
 * [sessions] that end on that night: several apps may each have written the night. The app whose
 * sessions last closest to the aggregated total is taken as the one Health Connect counted, and the
 * night is read from its sessions only, never mixing two apps' stages:
 * - with stages, time asleep is the sessions' length minus the time awake, split into deep, REM,
 *   light (when the app tells them apart) and awake;
 * - without stages, time asleep is the aggregated total, as is.
 * Its [DailySummary.origins] become that app. Without sessions, [summary] is returned as is.
 */
internal fun sleepNight(summary: DailySummary, sessions: List<SleepSessionRecord>): DailySummary {
    val total = summary.values["value"]?.let { (it as? DayValue.Total)?.value } ?: return summary
    val chosen = sessions.groupBy { it.metadata.dataOrigin.packageName }.values
        .minByOrNull { app -> kotlin.math.abs(app.sumOf { minutes(it.startTime, it.endTime) } - total) }
        ?: return summary
    val stages = chosen.flatMap { it.stages }
    val values = if (stages.isEmpty()) {
        mapOf("value" to DayValue.Total(total))
    } else {
        val awake = stageMinutes(stages, AwakeStages)
        buildMap {
            put("value", DayValue.Total(chosen.sumOf { minutes(it.startTime, it.endTime) } - awake))
            put("awake", DayValue.Total(awake))
            if (stages.any { it.stage in SplitStages.values.flatten() }) {
                SplitStages.forEach { (key, types) -> put(key, DayValue.Total(stageMinutes(stages, types))) }
            }
        }
    }
    return summary.copy(
        values = values,
        origins = chosen.mapTo(mutableSetOf()) { DataOrigin(it.metadata.dataOrigin.packageName) },
    )
}

/** Health Connect's stage types for time awake during a night. */
private val AwakeStages = setOf(
    SleepSessionRecord.STAGE_TYPE_AWAKE,
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
)

/** The breakdown keys of [SleepMetrics.SleepDuration] and their stage types. "Sleeping" (stage unknown) is in none. */
private val SplitStages = mapOf(
    "deep" to setOf(SleepSessionRecord.STAGE_TYPE_DEEP),
    "rem" to setOf(SleepSessionRecord.STAGE_TYPE_REM),
    "light" to setOf(SleepSessionRecord.STAGE_TYPE_LIGHT),
)

private fun stageMinutes(stages: List<SleepSessionRecord.Stage>, types: Set<Int>): Double =
    stages.filter { it.stage in types }.sumOf { minutes(it.startTime, it.endTime) }

private fun minutes(start: Instant, end: Instant): Double = Duration.between(start, end).toMillis() / 60_000.0

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
