package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayEntry
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.daySeries
import com.laul.lauwell.core.record.formatValue
import com.laul.lauwell.core.record.main
import com.laul.lauwell.core.ui.components.MiniChartData
import com.laul.lauwell.core.ui.components.MiniChartDays
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What a Tracker row or widget, and a pinned tile on Today, show for one metric: built by
 * [metricSummary] from its last [MiniChartDays] daily summaries, or [empty] before any data.
 *
 * @param value the latest day's value, Mono on screen ("72", "118/76"); null shows "—", never 0.
 * @param takeaway one line that reads it ("52–148 · today", "Yesterday · in range").
 * @param context the pinned tile's line under the value: "of 8,000" with a goal, else when.
 * @param progress share of the goal reached by the latest day, 0..1; null without an at-least goal.
 */
data class MetricSummary(
    val value: String?,
    val takeaway: String,
    val chart: MiniChartData?,
    val context: String = takeaway,
    val progress: Float? = null,
) {
    companion object {
        fun empty(metric: MetricDefinition) = MetricSummary(
            value = null,
            takeaway = "No data yet",
            chart = metric.miniChart?.let(MiniChartData::empty),
        )
    }
}

/**
 * [metric]'s summary from its daily [summaries] (any range; only the [MiniChartDays] days ending
 * [today] are read). The value and lines describe the latest day with data, which may be before
 * today; with no data in those days, [MetricSummary.empty].
 */
fun metricSummary(metric: MetricDefinition, summaries: List<DailySummary>, today: LocalDate): MetricSummary {
    val series = daySeries(metric, summaries, today.minusDays(MiniChartDays - 1L)..today)
    val latest = series.lastOrNull { it.summary != null }?.summary ?: return MetricSummary.empty(metric)
    val first = metric.quantities.firstOrNull() ?: return MetricSummary.empty(metric)
    val firstValue = latest.values[first.key]
    val ago = ago(latest.date, today)
    val takeaway = listOfNotNull(
        (firstValue as? DayValue.Range)?.let { "${formatValue(it.min, first.precision)}–${formatValue(it.max, first.precision)}" },
        ago,
        rangeStatus(metric, latest),
    ).joinToString(" · ").replaceFirstChar { it.uppercase() }
    val goal = metric.goal
    return MetricSummary(
        value = displayValue(metric, latest),
        takeaway = takeaway,
        chart = metric.miniChart?.let { miniChart(it, metric, series) },
        context = goal?.let { "of ${NumberFormat.getIntegerInstance().format(it.value)}" } ?: ago.replaceFirstChar { it.uppercase() },
        progress = firstValue?.main?.takeIf { goal?.direction == Goal.Direction.AtLeast }
            ?.let { (it / goal!!.value).coerceIn(0.0, 1.0).toFloat() },
    )
}

/** "118/76" when the quantities share a unit, else the first quantity alone. */
private fun displayValue(metric: MetricDefinition, day: DailySummary): String? {
    val shared = metric.quantities.map { it.unit }.distinct().size == 1
    val shown = if (shared) metric.quantities else metric.quantities.take(1)
    val parts = shown.map { q -> day.values[q.key]?.let { formatValue(it.main, q.precision) } ?: return null }
    return parts.joinToString("/")
}

/** "today", "yesterday", "3 days ago": when [date] was, seen from [today]. */
fun ago(date: LocalDate, today: LocalDate): String = when (val days = ChronoUnit.DAYS.between(date, today)) {
    0L -> "today"
    1L -> "yesterday"
    else -> "$days days ago"
}

/**
 * The day against the quantities' normal ranges: "above range" if any value is above, else "below
 * range" if any is below, else "in range"; null when no quantity has a range. Not said for a
 * min–average–max day (heart rate): its average includes activity, while the range is at rest.
 */
private fun rangeStatus(metric: MetricDefinition, day: DailySummary): String? {
    if (metric.aggregation == Aggregation.MinAverageMax) return null
    val checked = metric.quantities.mapNotNull { q ->
        val range = q.normalRange ?: return@mapNotNull null
        val value = day.values[q.key]?.main ?: return@mapNotNull null
        value to range
    }
    return when {
        checked.isEmpty() -> null
        checked.any { (v, r) -> v > r.endInclusive } -> "above range"
        checked.any { (v, r) -> v < r.start } -> "below range"
        else -> "in range"
    }
}

private fun miniChart(kind: MiniChartKind, metric: MetricDefinition, series: List<DayEntry>): MiniChartData {
    val key = metric.quantities.first().key
    return when (kind) {
        MiniChartKind.Line -> {
            val days = series.map { it.summary?.values?.get(key)?.main }
            val (min, max) = padded(days.filterNotNull())
            MiniChartData.Line(days, min, max)
        }
        MiniChartKind.Range -> {
            val days = series.map { entry -> entry.summary?.let(::lowHigh) }
            val (min, max) = padded(days.filterNotNull().flatMap { listOf(it.start, it.endInclusive) })
            MiniChartData.Range(days, min, max)
        }
        MiniChartKind.Bars -> {
            val days = series.map { it.summary?.values?.get(key)?.main }
            val goal = metric.goal?.value
            MiniChartData.Bars(days, max = maxOf(days.filterNotNull().maxOrNull() ?: 0.0, goal ?: 0.0, 1.0), goal = goal)
        }
        // Logged in the app (M4): nothing synced to draw yet.
        MiniChartKind.SeverityDots, MiniChartKind.CycleStrip -> MiniChartData.empty(kind)
    }
}

/** A day's span: a range's own min–max, else from the lowest to the highest of its quantities (blood pressure). */
private fun lowHigh(day: DailySummary): ClosedFloatingPointRange<Double>? {
    val values = day.values.values
    (values.singleOrNull() as? DayValue.Range)?.let { return it.min..it.max }
    val mains = values.map { it.main }.takeIf { it.isNotEmpty() } ?: return null
    return mains.min()..mains.max()
}

/** The values' span with 10% room each side (at least 1), so a flat week isn't drawn on an edge. */
private fun padded(values: List<Double>): Pair<Double, Double> {
    if (values.isEmpty()) return 0.0 to 1.0
    val low = values.min()
    val high = values.max()
    val room = maxOf((high - low) * 0.1, 1.0)
    return low - room to high + room
}
