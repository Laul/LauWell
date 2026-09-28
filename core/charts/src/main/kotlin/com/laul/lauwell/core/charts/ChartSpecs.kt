package com.laul.lauwell.core.charts

import com.laul.lauwell.core.domain.DateSpan
import com.laul.lauwell.core.domain.DaySlot
import com.laul.lauwell.core.domain.RangeSummary
import com.laul.lauwell.core.domain.TimePoint
import com.laul.lauwell.core.domain.dailyRanges as aggregateRanges
import com.laul.lauwell.core.domain.dailyTotals as aggregateTotals
import com.laul.lauwell.core.model.Aggregation
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.TargetRange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Builders from domain data to [ChartSpec]. Gaps (null days) produce no mark at all. */
object ChartSpecs {

    /** The daily chart that suits [metric]: ranges for point readings, totals for counts. */
    fun daily(metric: Metric, measurements: List<Measurement>, span: DateSpan, zone: ZoneId): ChartSpec =
        when (metric.aggregation) {
            Aggregation.RANGE -> dailyRanges(metric, aggregateRanges(measurements, span, zone), zone)
            Aggregation.SUM -> dailyTotals(metric, aggregateTotals(measurements, span, zone), zone)
        }

    /** Daily min–max bars with the average marked. x = days since the first slot. */
    fun dailyRanges(
        metric: Metric,
        days: List<DaySlot<RangeSummary>>,
        zone: ZoneId,
        target: TargetRange? = metric.defaultTarget,
    ): ChartSpec {
        val first = days.firstOrNull()?.date ?: LocalDate.now(zone)
        val bars = days.mapNotNull { slot ->
            slot.value?.let { RangeBar(dayIndex(first, slot.date), it.min, it.max, it.average) }
        }
        return ChartSpec(
            metric = metric,
            xAxis = dayAxis(first, zone),
            series = Series.RangeBars(bars),
            targetBand = target,
            yRange = yRange(bars.flatMap { listOf(it.low, it.high) }, target, fromZero = false),
        )
    }

    /** Daily totals as bars from zero. x = days since the first slot. */
    fun dailyTotals(
        metric: Metric,
        days: List<DaySlot<Double>>,
        zone: ZoneId,
        target: TargetRange? = metric.defaultTarget,
    ): ChartSpec {
        val first = days.firstOrNull()?.date ?: LocalDate.now(zone)
        val bars = days.mapNotNull { slot -> slot.value?.let { Bar(dayIndex(first, slot.date), it) } }
        return ChartSpec(
            metric = metric,
            xAxis = dayAxis(first, zone),
            series = Series.Bars(bars),
            targetBand = target,
            yRange = yRange(bars.map { it.value }, target, fromZero = true),
        )
    }

    /** Every reading on a line. x = minutes since [origin] (start of the period). */
    fun timeline(
        metric: Metric,
        points: List<TimePoint>,
        origin: Instant,
        zone: ZoneId,
        target: TargetRange? = metric.defaultTarget,
    ): ChartSpec {
        val axis = XAxis(origin, Duration.ofMinutes(1), zone)
        val line = points.map { Point(axis.xOf(it.time), it.value) }
        return ChartSpec(
            metric = metric,
            xAxis = axis,
            series = Series.Line(line),
            targetBand = target,
            yRange = yRange(line.map { it.y }, target, fromZero = false),
        )
    }

    private fun dayAxis(first: LocalDate, zone: ZoneId) =
        XAxis(first.atStartOfDay(zone).toInstant(), Duration.ofDays(1), zone)

    // Calendar days, not 24 h blocks, so DST days still land on whole numbers.
    private fun dayIndex(first: LocalDate, date: LocalDate): Double =
        ChronoUnit.DAYS.between(first, date).toDouble()

    /** Data range widened to include the target band, padded by 10 %. Null when nothing to show. */
    internal fun yRange(values: List<Double>, target: TargetRange?, fromZero: Boolean): ClosedFloatingPointRange<Double>? {
        val all = values + listOfNotNull(target?.low, target?.high)
        if (values.isEmpty() || all.isEmpty()) return null
        val lo = if (fromZero) 0.0 else all.min()
        val hi = all.max()
        val pad = (hi - lo).takeIf { it > 0 }?.times(0.1) ?: (hi * 0.1).coerceAtLeast(1.0)
        return (if (fromZero) 0.0 else lo - pad)..(hi + pad)
    }
}
