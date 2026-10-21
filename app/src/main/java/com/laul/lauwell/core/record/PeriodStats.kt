package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import java.time.LocalDate

/** One day of a chart or per-day list: its summary, or null when the day has no data (never 0). */
data class DayEntry(val date: LocalDate, val summary: DailySummary?)

/**
 * Every day of [days], oldest first, both ends included, each with its summary or null: the chart's
 * x axis with its gaps. Summaries outside [days] or of another metric are left out.
 */
fun daySeries(metric: MetricDefinition, summaries: List<DailySummary>, days: ClosedRange<LocalDate>): List<DayEntry> {
    val byDate = summaries.filter { it.metric == metric.id }.associateBy { it.date }
    return datesIn(days).map { DayEntry(it, byDate[it]) }.toList()
}

/** A value and the day it was reached. */
data class DayPoint(val date: LocalDate, val value: Double)

/**
 * One quantity of a metric over a period, for the detail screen's stats row and the Tracker
 * takeaways. Days without data count in [dayCount] only: they never pull the average down.
 *
 * @param average mean of the daily values (for a range, of the daily averages); null with no data.
 * @param lowest the period's lowest point: a range's daily minimum, otherwise the daily value.
 * @param highest the period's highest point (the "best day" of steps); on a tie, the latest day.
 * @param daysAtGoal days that met the metric's goal; null when it has none.
 * @param daysInRange days whose value sits in the quantity's normal range; null when it has none.
 */
data class PeriodStats(
    val dayCount: Int,
    val daysWithData: Int,
    val average: Double?,
    val lowest: DayPoint?,
    val highest: DayPoint?,
    val daysAtGoal: Int?,
    val daysInRange: Int?,
)

/**
 * The stats of [quantity] (the metric's first by default) over [days], both ends included, from the
 * day's [summaries]. Summaries outside [days] or of another metric are ignored.
 */
fun periodStats(
    metric: MetricDefinition,
    summaries: List<DailySummary>,
    days: ClosedRange<LocalDate>,
    quantity: String = metric.quantities.first().key,
): PeriodStats {
    val definition = requireNotNull(metric.quantities.find { it.key == quantity }) { "${metric.id} has no quantity $quantity" }
    val values = daySeries(metric, summaries, days).mapNotNull { entry ->
        entry.summary?.values?.get(quantity)?.let { entry.date to it }
    }
    val lows = values.map { (date, value) -> DayPoint(date, value.low) }
    val highs = values.map { (date, value) -> DayPoint(date, value.high) }
    val mains = values.map { (_, value) -> value.main }
    return PeriodStats(
        dayCount = datesIn(days).count(),
        daysWithData = values.size,
        average = mains.takeIf { it.isNotEmpty() }?.average(),
        // minBy keeps the first of equal values and the days are oldest first, so walk them newest first.
        lowest = lows.asReversed().minByOrNull { it.value },
        highest = highs.asReversed().maxByOrNull { it.value },
        daysAtGoal = metric.goal?.let { goal -> mains.count { goal.isMetBy(it) } },
        daysInRange = definition.normalRange?.let { range -> mains.count { it in range } },
    )
}

/** Whether a day's value meets the goal; reaching it exactly counts. */
fun Goal.isMetBy(value: Double): Boolean = when (direction) {
    Goal.Direction.AtLeast -> value >= this.value
    Goal.Direction.AtMost -> value <= this.value
}

/** The day's headline value: the total, the mean, the average of a range, or the last value. */
val DayValue.main: Double
    get() = when (this) {
        is DayValue.Total -> value
        is DayValue.Mean -> value
        is DayValue.Range -> average
        is DayValue.Last -> value
    }

private val DayValue.low: Double get() = if (this is DayValue.Range) min else main
private val DayValue.high: Double get() = if (this is DayValue.Range) max else main

private fun datesIn(days: ClosedRange<LocalDate>): Sequence<LocalDate> =
    generateSequence(days.start) { it.plusDays(1) }.takeWhile { it <= days.endInclusive }
