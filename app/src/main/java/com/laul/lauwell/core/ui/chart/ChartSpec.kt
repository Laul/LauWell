package com.laul.lauwell.core.ui.chart

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayEntry
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.daySeries
import com.laul.lauwell.core.record.display
import com.laul.lauwell.core.record.isMetBy
import com.laul.lauwell.core.record.main
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * The range a detail chart shows, always ending today. Week and Month have one slot per day;
 * 3 months has one per week (13), since 91 daily bars would be too thin to read.
 */
enum class ChartPeriod(val label: String, val slotCount: Int, val daysPerSlot: Int) {
    Week("Week", 7, 1),
    Month("Month", 30, 1),
    ThreeMonths("3 months", 13, 7);

    val dayCount: Int get() = slotCount * daysPerSlot

    fun days(today: LocalDate): ClosedRange<LocalDate> = today.minusDays(dayCount - 1L)..today
}

/**
 * One slot's value: [low] to [high] for a range bar, [value] for a bar or a line (and a range's
 * average). A bar or a line point has low = value = high.
 *
 * @param parts a stacked bar's segments from the bottom, one per [ChartSpec.parts], adding up to
 *   [value]; empty when the slot has no breakdown (drawn as one plain bar).
 */
data class ChartPoint(val low: Double, val value: Double, val high: Double, val parts: List<Double> = emptyList())

/** A day, or a week in [ChartPeriod.ThreeMonths]; [point] null when it holds no data (a gap, never 0). */
data class ChartSlot(val days: ClosedRange<LocalDate>, val point: ChartPoint?)

/**
 * A detail chart as the app describes it, from the domain's daily summaries. `VicoChart.kt` is the
 * only code that turns it into a Vico chart (CLAUDE.md → Stack → Charts), so changing library
 * touches that file only. The last slot is the current one (today, or this week).
 *
 * @param band the quantity's normal range, drawn behind; null for none or for several quantities.
 * @param markAverage whether a range bar carries its average as a dot (heart rate, not blood pressure).
 * @param quantity the charted value's quantity, for its labels ("8,260 steps", "7h 12m").
 * @param parts a stacked bar's parts, from the bottom (deep, REM, light sleep); empty otherwise.
 */
data class ChartSpec(
    val kind: ChartKind,
    val category: Category,
    val period: ChartPeriod,
    val slots: List<ChartSlot>,
    val band: ClosedFloatingPointRange<Double>?,
    val goal: Goal?,
    val markAverage: Boolean,
    val quantity: Quantity,
    val parts: List<Quantity> = emptyList(),
) {
    init {
        require(slots.size == period.slotCount) { "${period.slotCount} slots expected, got ${slots.size}" }
    }

    val currentIndex: Int get() = slots.lastIndex
    val hasData: Boolean get() = slots.any { it.point != null }
}

/**
 * [metric]'s chart over [period] ending [today], from its daily [summaries]; null for a metric
 * without a chart kind. A range bar spans a day's own min–max (heart rate) or its quantities
 * (blood pressure: diastolic to systolic); a bar or a line uses the day's value. A stacked bar is
 * as tall as the day's value, split in proportion to its parts (the stages of one app's night may
 * not add up exactly to the night's value). A week slot holds the mean of its days' values and
 * parts, and the extremes of their lows and highs.
 */
fun chartSpec(metric: MetricDefinition, summaries: List<DailySummary>, period: ChartPeriod, today: LocalDate): ChartSpec? {
    val kind = metric.chartKind ?: return null
    val first = metric.quantities.firstOrNull() ?: return null
    val series = daySeries(metric, summaries, period.days(today))
    val parts = if (kind == ChartKind.StackedBar) metric.breakdown.filter { it.inTotal } else emptyList()
    return ChartSpec(
        kind = kind,
        category = metric.category,
        period = period,
        slots = series.chunked(period.daysPerSlot).map { days ->
            ChartSlot(days.first().date..days.last().date, slotPoint(days.mapNotNull { dayPoint(kind, metric, parts, it) }))
        },
        band = metric.quantities.singleOrNull()?.normalRange,
        goal = metric.goal,
        markAverage = metric.aggregation == Aggregation.MinAverageMax,
        quantity = first,
        parts = parts,
    )
}

private fun dayPoint(kind: ChartKind, metric: MetricDefinition, parts: List<Quantity>, entry: DayEntry): ChartPoint? {
    val values = entry.summary?.values ?: return null
    if (kind == ChartKind.RangeBar) {
        (values.values.singleOrNull() as? DayValue.Range)?.let { return ChartPoint(it.min, it.average, it.max) }
        val mains = metric.quantities.mapNotNull { values[it.key]?.main }.takeIf { it.isNotEmpty() } ?: return null
        return ChartPoint(mains.min(), mains.average(), mains.max())
    }
    val value = values[metric.quantities.first().key]?.main ?: return null
    return ChartPoint(value, value, value, scaledParts(parts.map { values[it.key]?.main }, value))
}

/** [parts] rescaled to add up to [total]; empty when one is missing or they add up to nothing. */
private fun scaledParts(parts: List<Double?>, total: Double): List<Double> {
    if (parts.isEmpty() || parts.any { it == null }) return emptyList()
    val sum = parts.sumOf { it!! }
    return if (sum <= 0.0) emptyList() else parts.map { it!! * total / sum }
}

private fun slotPoint(days: List<ChartPoint>): ChartPoint? {
    if (days.isEmpty()) return null
    val value = days.map { it.value }.average()
    // A week's parts: the average split of its days that have one, scaled to the week's value.
    val split = days.filter { it.parts.isNotEmpty() }
    val parts = split.firstOrNull()?.parts?.indices?.map { i -> split.map { it.parts[i] }.average() }.orEmpty()
    return ChartPoint(days.minOf { it.low }, value, days.maxOf { it.high }, scaledParts(parts, value))
}

/**
 * The opacity of stacked part [index] over the category fg, darkest at the bottom (deep sleep);
 * the breakdown on the detail screen uses the same, so it reads as the chart's legend.
 */
fun partAlpha(index: Int): Float = PartAlphas.getOrElse(index) { PartAlphas.last() }

private val PartAlphas = listOf(1f, 0.6f, 0.3f)

/** Whether slot [index] meets the goal; null without a goal or data. Below-goal bars are dimmed. */
fun ChartSpec.meetsGoal(index: Int): Boolean? {
    val point = slots.getOrNull(index)?.point ?: return null
    return goal?.isMetBy(point.value)
}

/**
 * The y axis: bars from 0 up to the highest bar or the goal, otherwise every value and the band;
 * widened to round steps with room; null when no slot has data (the chart shows its empty state).
 */
fun ChartSpec.yBounds(): ClosedFloatingPointRange<Double>? {
    val points = slots.mapNotNull { it.point }.takeIf { it.isNotEmpty() } ?: return null
    if (kind == ChartKind.Bar || kind == ChartKind.StackedBar) {
        val top = maxOf(points.maxOf { it.high }, goal?.value ?: 0.0) * 1.1
        val step = niceStep(top)
        return 0.0..(ceil(top / step) * step).coerceAtLeast(step)
    }
    val low = minOf(points.minOf { it.low }, band?.start ?: Double.MAX_VALUE)
    val high = maxOf(points.maxOf { it.high }, band?.endInclusive ?: -Double.MAX_VALUE)
    val step = niceStep(high - low)
    return floor((low - step / 2) / step) * step..ceil((high + step / 2) / step) * step
}

/** 1, 2 or 5 × 10ⁿ: about ten steps over [span], fine enough that rounding wastes little height. */
private fun niceStep(span: Double): Double {
    if (span <= 0) return 1.0
    val raw = span / 10
    val magnitude = 10.0.pow(floor(log10(raw)))
    return listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= raw } * magnitude
}

/**
 * Which slots carry an x-axis label: every [labelSpacing]-th, counted back from the current one
 * so that today (or this week) is always labeled. Every day of a week; every 7th of a month;
 * every 4th week of 3 months.
 */
val ChartSpec.labelSpacing: Int
    get() = when (period) {
        ChartPeriod.Week -> 1
        ChartPeriod.Month -> 7
        ChartPeriod.ThreeMonths -> 4
    }

/** The first labeled slot (see [labelSpacing]). */
val ChartSpec.labelOffset: Int get() = currentIndex % labelSpacing

/** Slot [index]'s x-axis label: the weekday's letter in a week, the day ("20") in a month, "20 Oct" in 3 months. */
fun ChartSpec.slotLabel(index: Int, locale: Locale = Locale.getDefault()): String {
    // Any index, also just outside the slots: Vico measures labels there too, and refuses blank ones.
    val end = slots.last().days.endInclusive.minusDays((currentIndex - index).toLong() * period.daysPerSlot)
    return when (period) {
        ChartPeriod.Week -> end.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
        ChartPeriod.Month -> end.dayOfMonth.toString()
        ChartPeriod.ThreeMonths -> end.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }
}

/** The chart header's dates: "14 – 20 Oct", "21 Sep – 20 Oct". */
fun ChartSpec.rangeLabel(locale: Locale = Locale.getDefault()): String {
    val start = slots.first().days.start
    val end = slots.last().days.endInclusive
    val month = DateTimeFormatter.ofPattern("MMM", locale)
    val startText = if (start.month == end.month && start.year == end.year) "${start.dayOfMonth}" else "${start.dayOfMonth} ${start.format(month)}"
    return "$startText – ${end.dayOfMonth} ${end.format(month)}"
}

/** The chart header's reference line: "Goal 8,000", "Goal 8h" or "Normal 60–100"; null for neither. */
fun ChartSpec.referenceLabel(): String? = when {
    goal != null -> "Goal ${quantity.display(goal.value)}"
    band != null -> "Normal ${quantity.display(band.start)}–${quantity.display(band.endInclusive)}"
    else -> null
}

/**
 * The tooltip of slot [index]: "Tue 14 · 52–148 bpm · avg 71", "Tue 14 · 8432 steps", or for a
 * week "6–12 Oct · avg 8432 steps a day"; null for a slot without data.
 */
fun ChartSpec.tooltip(index: Int, locale: Locale = Locale.getDefault()): String? {
    val slot = slots.getOrNull(index) ?: return null
    val point = slot.point ?: return null
    val weekly = period.daysPerSlot > 1
    val day = slot.days.endInclusive
    val month = DateTimeFormatter.ofPattern("MMM", locale)
    val start = slot.days.start
    val label = if (weekly) {
        val startText = if (start.month == day.month) "${start.dayOfMonth}" else "${start.dayOfMonth} ${start.format(month)}"
        "$startText–${day.dayOfMonth} ${day.format(month)}"
    } else {
        "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${day.dayOfMonth}"
    }
    fun f(value: Double) = quantity.display(value)
    val unit = if (quantity.unit.isEmpty()) "" else " ${quantity.unit}"
    val value = when (kind) {
        ChartKind.RangeBar -> "${f(point.low)}–${f(point.high)}$unit" + if (markAverage) " · avg ${f(point.value)}" else ""
        ChartKind.Bar -> if (weekly) "avg ${f(point.value)}$unit a day" else "${f(point.value)}$unit"
        // "Tue 14 · 7h 12m · Deep 1h 30m · REM 1h 40m · Light 4h 2m"
        ChartKind.StackedBar -> (if (weekly) "avg ${f(point.value)}" else f(point.value)) +
            parts.zip(point.parts).joinToString("") { (part, length) -> " · ${part.label} ${part.display(length)}" }
        ChartKind.Line -> if (weekly) "avg ${f(point.value)}$unit" else "${f(point.value)}$unit"
    }
    return "$label · $value"
}
