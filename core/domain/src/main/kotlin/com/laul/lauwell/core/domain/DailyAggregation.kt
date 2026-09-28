package com.laul.lauwell.core.domain

import com.laul.lauwell.core.model.Measurement
import java.time.LocalDate
import java.time.ZoneId

/** One day of a chart. [value] is null when there is no data for that day: a gap, not a zero. */
data class DaySlot<out T>(val date: LocalDate, val value: T?)

/** Summary of point readings over one day. Real values, not shaped for any chart library. */
data class RangeSummary(val min: Double, val max: Double, val average: Double, val count: Int)

/**
 * Min, max and average per day for point readings (heart rate, glucose).
 *
 * Returns one slot per day of [span], in order. Days are local days in [zone].
 */
fun dailyRanges(measurements: Iterable<Measurement>, span: DateSpan, zone: ZoneId): List<DaySlot<RangeSummary>> {
    val byDay = measurements.groupBy { it.start.atZone(zone).toLocalDate() }
    return span.days.map { day ->
        val values = byDay[day]?.map { it.value }
        DaySlot(
            date = day,
            value = values?.takeIf { it.isNotEmpty() }?.let {
                RangeSummary(min = it.min(), max = it.max(), average = it.average(), count = it.size)
            },
        )
    }
}

/**
 * Total per day for interval values (steps). A day whose records add up to 0 is 0; a day with no
 * records is null.
 *
 * An interval is counted on the local day it starts. Sources should deliver hourly or finer
 * buckets so that no interval crosses midnight.
 */
fun dailyTotals(measurements: Iterable<Measurement>, span: DateSpan, zone: ZoneId): List<DaySlot<Double>> {
    val byDay = measurements.groupBy { it.start.atZone(zone).toLocalDate() }
    return span.days.map { day -> DaySlot(day, byDay[day]?.sumOf { it.value }) }
}
