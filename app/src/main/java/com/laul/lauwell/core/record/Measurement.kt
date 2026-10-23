package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.ValueFormat
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * The `Measurement` shape: value · unit · instant (PLAN.md → Decisions → Storage shapes). One heart
 * rate sample, one blood pressure reading.
 *
 * [values] is keyed by the metric's `Quantity.key` ("value", or "systolic" and "diastolic"); units
 * are the definition's, never stored per record. The other six shapes get their record type with
 * their first metric.
 */
data class Measurement(
    override val id: RecordId,
    override val metric: MetricId,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    val values: Map<String, Double>,
    override val provenance: Provenance,
    override val links: List<RecordLink> = emptyList(),
) : HealthRecord {
    init {
        require(values.isNotEmpty()) { "$id: a measurement needs a value" }
        require(values.values.all { it.isFinite() }) { "$id: values must be finite" }
    }

    /** Fails when this record doesn't fit [definition]: wrong metric, shape or quantities. */
    fun requireMatches(definition: MetricDefinition) {
        require(definition.id == metric) { "$id is a $metric record, not ${definition.id}" }
        require(definition.shape == StorageShape.Measurement) { "${definition.id} is not a Measurement" }
        require(values.keys == definition.quantities.map { it.key }.toSet()) {
            "$id: values ${values.keys} don't match the quantities of ${definition.id}"
        }
    }
}

/**
 * The one-line summary of History and link labels: "Heart rate 72 bpm", "Blood pressure
 * 118/76 mmHg" (values in quantity order, joined by "/" when they share a unit).
 */
fun summaryLine(record: Measurement, definition: MetricDefinition): String {
    record.requireMatches(definition)
    val quantities = definition.quantities
    val numbers = quantities.map { formatValue(record.values.getValue(it.key), it.precision) }
    val sharedUnit = quantities.map { it.unit }.distinct().singleOrNull()
    val text = if (sharedUnit != null) {
        numbers.joinToString("/") + unitSuffix(sharedUnit)
    } else {
        quantities.zip(numbers).joinToString(" · ") { (q, n) -> "${q.label} $n${unitSuffix(q.unit)}" }
    }
    return "${definition.label} $text"
}

private fun unitSuffix(unit: String) = if (unit.isEmpty()) "" else " $unit"

/** Rounded half-up to [precision] decimals, no grouping: a value, not a display string of the UI. */
internal fun formatValue(value: Double, precision: Int): String =
    value.toBigDecimal().setScale(precision, RoundingMode.HALF_UP).toPlainString()

/**
 * A value as the UI shows it: [formatValue] with thousands grouped ("8,260"), the same on every
 * phone (the app is in English, so the separator doesn't follow the phone's locale).
 */
fun displayValue(value: Double, precision: Int): String =
    DecimalFormat("#,##0" + if (precision > 0) "." + "0".repeat(precision) else "", DecimalFormatSymbols(Locale.ENGLISH))
        .apply { roundingMode = RoundingMode.HALF_UP }
        .format(value.toBigDecimal().setScale(precision, RoundingMode.HALF_UP))

/** [value] of this quantity as the UI shows it: "8,260", or "7h 12m" for a duration. */
fun Quantity.display(value: Double): String = when (format) {
    ValueFormat.Number -> displayValue(value, precision)
    ValueFormat.Duration -> durationText(value)
}

/** Minutes as "7h 12m", "8h", "45m" (rounded to the minute). */
internal fun durationText(minutes: Double): String {
    val total = minutes.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toLong()
    val hours = total / 60
    val rest = total % 60
    return when {
        hours == 0L -> "${rest}m"
        rest == 0L -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}
