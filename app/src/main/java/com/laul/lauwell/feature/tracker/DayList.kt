package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.daySeries
import com.laul.lauwell.core.record.display
import com.laul.lauwell.core.record.isMetBy
import com.laul.lauwell.core.record.main
import com.laul.lauwell.core.summary.rangeStatus
import com.laul.lauwell.core.ui.chart.ChartPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One day of the detail screen's per-day list (design: "Fri 9 · goal met · 8,260").
 *
 * @param value the day's value, a range as "70–118"; null without data (shown "—").
 * @param note "in progress", "goal met", "above range", "no data"…, or null.
 * @param atGoal the value meets the metric's goal (shown in the category colour).
 */
data class DayRow(val date: LocalDate, val label: String, val value: String?, val note: String?, val atGoal: Boolean)

/**
 * The days of [period] ending [today], newest first, from the oldest day with data on (days before
 * it say nothing, e.g. before the watch was worn); empty when the period has no data at all.
 */
fun dayRows(metric: MetricDefinition, summaries: List<DailySummary>, period: ChartPeriod, today: LocalDate): List<DayRow> {
    val series = daySeries(metric, summaries, period.days(today))
    val first = series.indexOfFirst { it.summary != null }
    if (first < 0) return emptyList()
    val format = if (period == ChartPeriod.Week) WeekDay else MonthDay
    return series.drop(first).asReversed().map { entry ->
        val day = entry.summary
        val main = day?.let { mainValue(metric, it) }
        val atGoal = main != null && metric.goal?.isMetBy(main) == true
        DayRow(
            date = entry.date,
            label = if (entry.date == today) (if (metric.nightly) "Last night" else "Today") else format.format(entry.date),
            value = day?.let { valueText(metric, it) },
            note = when {
                day == null -> "no data"
                // A total or a range keeps growing until midnight; one daily reading, or last night, doesn't.
                entry.date == today && metric.aggregation in GrowingDuringTheDay && !metric.nightly -> "in progress"
                atGoal -> "goal met"
                else -> rangeStatus(metric, day)?.takeIf { it != "in range" }
            },
            atGoal = atGoal,
        )
    }
}

private val GrowingDuringTheDay = setOf(Aggregation.Sum, Aggregation.MinAverageMax)

private val WeekDay = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)
private val MonthDay = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

private fun mainValue(metric: MetricDefinition, day: DailySummary): Double? =
    metric.quantities.firstOrNull()?.let { day.values[it.key]?.main }

/**
 * The day's value as shown: a range as "70–118", several quantities as "118/76" (blood pressure),
 * otherwise the main value; null when the day has none of them.
 */
internal fun valueText(metric: MetricDefinition, day: DailySummary): String? {
    val parts = metric.quantities.mapNotNull { quantity ->
        when (val value = day.values[quantity.key]) {
            null -> null
            is DayValue.Range -> "${quantity.display(value.min)}–${quantity.display(value.max)}"
            else -> quantity.display(value.main)
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("/")
}
