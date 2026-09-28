package com.laul.lauwell.core.domain

import com.laul.lauwell.core.model.Measurement

/** Headline numbers for a period. */
data class PeriodStats(val min: Double, val max: Double, val average: Double)

/**
 * Stats over daily ranges: lowest min, highest max, and the average of every reading in the
 * period (weighted by readings, not a mean of daily means). Null when no day has data.
 */
fun List<DaySlot<RangeSummary>>.rangeStats(): PeriodStats? {
    val days = mapNotNull { it.value }
    if (days.isEmpty()) return null
    val readings = days.sumOf { it.count }
    return PeriodStats(
        min = days.minOf { it.min },
        max = days.maxOf { it.max },
        average = days.sumOf { it.average * it.count } / readings,
    )
}

/** Stats over daily totals, counting only days that have data. Null when no day has data. */
fun List<DaySlot<Double>>.totalStats(): PeriodStats? {
    val totals = mapNotNull { it.value }
    if (totals.isEmpty()) return null
    return PeriodStats(min = totals.min(), max = totals.max(), average = totals.average())
}

/** The most recent measurement, by end time. */
fun Iterable<Measurement>.latest(): Measurement? = maxByOrNull { it.end }
