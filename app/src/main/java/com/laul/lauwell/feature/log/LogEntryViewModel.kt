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

/** Saves and deletes what the quick-log sheet collects, in your account. */
class LogEntryViewModel(private val records: RecordRepository, private val clock: Clock) : ViewModel() {

    /**
     * Saves a [value] for the rating [metric], taken [at] (null: now), with an optional [note]. With
     * an [original], that entry is updated in place instead of a new one being added.
     */
    fun saveRating(metric: MetricDefinition, value: Int, at: ZonedDateTime?, note: String, original: Measurement?): AppResult<Unit> {
        val id = original?.id ?: records.newId()
        return records.save(ratingEntry(id, metric, value, at ?: ZonedDateTime.now(clock), note, clock, original))
    }

    fun delete(entry: Measurement): AppResult<Unit> = records.delete(entry.id)
}

/**
 * When an entry was taken, once a [time] is picked. Editing an entry keeps its day ([on]). A new one
 * is today, or yesterday when [time] is later than now: logging "23:30" just after midnight is
 * about the evening before.
 */
internal fun entryTime(time: LocalTime, on: LocalDate?, clock: Clock): ZonedDateTime {
    if (on != null) return ZonedDateTime.of(on, time, clock.zone)
    val today = ZonedDateTime.of(LocalDate.now(clock), time, clock.zone)
    return if (today.isAfter(ZonedDateTime.now(clock))) today.minusDays(1) else today
}

/**
 * The record a rating becomes: one value on the metric's scale, logged by hand, in the offset where
 * it was taken (so it lands on the right local day), with the note trimmed or left out. Editing an
 * [original] keeps its id, links and creation time, and stamps the edit.
 */
internal fun ratingEntry(
    id: RecordId,
    metric: MetricDefinition,
    value: Int,
    at: ZonedDateTime,
    note: String,
    clock: Clock,
    original: Measurement? = null,
): Measurement {
    val quantity = metric.quantities.single()
    require(value in requireNotNull(quantity.scale) { "${metric.id} is not a rating" }) { "$value is off ${metric.id}'s scale" }
    val now = Instant.now(clock)
    return Measurement(
        id = id,
        metric = metric.id,
        time = at.toInstant(),
        zoneOffset = at.offset,
        values = mapOf(quantity.key to value.toDouble()),
        provenance = original?.provenance?.copy(editedAt = now)
            ?: Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = now),
        links = original?.links.orEmpty(),
        note = note.trim().takeIf { it.isNotEmpty() },
    )
}
