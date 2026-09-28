package com.laul.lauwell.core.charts

import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.TargetRange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * What to draw, independent of any chart library.
 *
 * Screens build a [ChartSpec] from domain data; a single mapper per library (for Vico:
 * `app/.../ui/chart/VicoChart.kt`) turns it into library models. Swapping or upgrading the
 * library touches that mapper only.
 *
 * All x values are small doubles counted in [XAxis.unit] from [XAxis.origin], never raw epoch
 * milliseconds (a Float cannot hold those to better than about two minutes, and Vico derives its
 * x step from the GCD of x deltas).
 */
data class ChartSpec(
    val metric: Metric,
    val xAxis: XAxis,
    val series: Series,
    val targetBand: TargetRange?,
    /** Visible y range. Computed from the data, so the target band and every value fit. */
    val yRange: ClosedFloatingPointRange<Double>?,
)

/** Maps x values back to time. Day axes count calendar days, so they stay whole across DST. */
data class XAxis(val origin: Instant, val unit: Duration, val zone: ZoneId) {
    private val isDays = unit == Duration.ofDays(1)

    fun timeAt(x: Double): Instant =
        if (isDays) dateAt(x).atStartOfDay(zone).toInstant() else origin.plusMillis((x * unit.toMillis()).roundToLong())

    fun dateAt(x: Double): LocalDate =
        if (isDays) origin.atZone(zone).toLocalDate().plusDays(x.roundToLong()) else timeAt(x).atZone(zone).toLocalDate()

    fun xOf(time: Instant): Double = Duration.between(origin, time).toMillis().toDouble() / unit.toMillis()
}

sealed interface Series {
    /** One bar from [RangeBar.low] to [RangeBar.high] with a mark at [RangeBar.mid], per x. */
    data class RangeBars(val bars: List<RangeBar>) : Series

    /** Bars from zero. */
    data class Bars(val bars: List<Bar>) : Series

    /** A line through the points, in x order. */
    data class Line(val points: List<Point>) : Series
}

data class RangeBar(val x: Double, val low: Double, val high: Double, val mid: Double)

data class Bar(val x: Double, val value: Double)

data class Point(val x: Double, val y: Double)
