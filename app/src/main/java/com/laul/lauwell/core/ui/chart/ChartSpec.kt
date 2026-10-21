package com.laul.lauwell.core.ui.chart

import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.record.DayEntry
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.formatValue
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A chart as the app describes it, from the domain's daily summaries: one slot per day, `null` for
 * a day without data (drawn as a gap, never as 0). `VicoChart.kt` is the only code that turns it
 * into a Vico chart (CLAUDE.md → Stack → Charts), so changing library touches that file only.
 *
 * Only the range bar exists so far (the chart validation test, PLAN.md → M3); bars and lines come
 * with the reusable chart component.
 */
sealed interface ChartSpec {
    /** The metric's category: the chart's color. */
    val category: Category

    /** One per slot, oldest first. */
    val days: List<LocalDate>

    /**
     * One low–high bar per day with its average as a dot, over the quantity's normal [band]
     * (heart rate).
     */
    data class RangeBars(
        override val category: Category,
        override val days: List<LocalDate>,
        val bars: List<RangeBar?>,
        val band: ClosedFloatingPointRange<Double>?,
        val unit: String,
        val precision: Int,
    ) : ChartSpec {
        init {
            require(bars.size == days.size) { "one bar slot per day" }
        }
    }
}

data class RangeBar(val low: Double, val average: Double, val high: Double)

/** [series] (from `daySeries`) as range bars of [quantity], the metric's first by default. */
fun rangeBarsSpec(
    metric: MetricDefinition,
    series: List<DayEntry>,
    quantity: String = metric.quantities.first().key,
): ChartSpec.RangeBars {
    val definition = requireNotNull(metric.quantities.find { it.key == quantity }) { "${metric.id} has no quantity $quantity" }
    return ChartSpec.RangeBars(
        category = metric.category,
        days = series.map { it.date },
        bars = series.map { entry ->
            when (val value = entry.summary?.values?.get(quantity)) {
                null -> null
                is DayValue.Range -> RangeBar(value.min, value.average, value.max)
                else -> error("${metric.id} $quantity is not a range: ${value.aggregation}")
            }
        },
        band = definition.normalRange,
        unit = definition.unit,
        precision = definition.precision,
    )
}

/**
 * The y axis: every bar and the band, widened to multiples of [step] with at least half a step of
 * room; null when no day has data (the chart shows its empty state instead).
 */
fun ChartSpec.RangeBars.yBounds(step: Double = 10.0): ClosedFloatingPointRange<Double>? {
    val present = bars.filterNotNull().takeIf { it.isNotEmpty() } ?: return null
    val low = minOf(present.minOf { it.low }, band?.start ?: Double.MAX_VALUE)
    val high = maxOf(present.maxOf { it.high }, band?.endInclusive ?: -Double.MAX_VALUE)
    return floor((low - step / 2) / step) * step..ceil((high + step / 2) / step) * step
}

/** The x-axis label of slot [index]: the weekday's first letter ("M", "T"…). */
fun ChartSpec.dayLabel(index: Int, locale: Locale = Locale.getDefault()): String =
    days.getOrNull(index)?.dayOfWeek?.getDisplayName(TextStyle.NARROW, locale).orEmpty()

/** The tooltip of slot [index]: "Tue 14 · 52–148 bpm · avg 71"; null for a day without data. */
fun ChartSpec.RangeBars.tooltip(index: Int, locale: Locale = Locale.getDefault()): String? {
    val bar = bars.getOrNull(index) ?: return null
    val day = days[index]
    val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    fun f(value: Double) = formatValue(value, precision)
    return "$weekday ${day.dayOfMonth} · ${f(bar.low)}–${f(bar.high)} $unit · avg ${f(bar.average)}"
}
