package com.laul.lauwell.feature.log

import androidx.lifecycle.ViewModel
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Saves what the quick-log sheet collects, to your account. */
class LogEntryViewModel(private val records: RecordRepository, private val clock: Clock) : ViewModel() {

    /** Saves a [value] for the rating [metric], taken at [at] (null: now), with an optional [note]. */
    fun saveRating(metric: MetricDefinition, value: Int, at: LocalTime?, note: String): AppResult<Unit> =
        records.save(ratingEntry(records.newId(), metric, value, entryTime(at, clock), note, clock))
}

/**
 * When an entry was taken: now, or today at [at]. A time later than now means yesterday: logging
 * "23:30" just after midnight is about the evening before.
 */
internal fun entryTime(at: LocalTime?, clock: Clock): ZonedDateTime {
    val now = ZonedDateTime.now(clock)
    if (at == null) return now
    val today = ZonedDateTime.of(LocalDate.now(clock), at, clock.zone)
    return if (today.isAfter(now)) today.minusDays(1) else today
}

/**
 * The record a rating becomes: one value on the metric's scale, logged by hand, in the offset where
 * it was taken (so it lands on the right local day), with the note trimmed or left out.
 */
internal fun ratingEntry(
    id: RecordId,
    metric: MetricDefinition,
    value: Int,
    at: ZonedDateTime,
    note: String,
    clock: Clock,
): Measurement {
    val quantity = metric.quantities.single()
    require(value in requireNotNull(quantity.scale) { "${metric.id} is not a rating" }) { "$value is off ${metric.id}'s scale" }
    return Measurement(
        id = id,
        metric = metric.id,
        time = at.toInstant(),
        zoneOffset = at.offset,
        values = mapOf(quantity.key to value.toDouble()),
        provenance = Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.now(clock)),
        note = note.trim().takeIf { it.isNotEmpty() },
    )
}
