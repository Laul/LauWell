package com.laul.lauwell.core.ui.chart

import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.BloodPressure
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.EcgTrace
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.HeartRate
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.RestingHeartRate
import com.laul.lauwell.core.catalog.metrics.SleepMetrics.SleepDuration
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ChartSpecTest {

    private val today = LocalDate.of(2026, 10, 20) // a Tuesday
    private val en = Locale.ENGLISH

    private fun hr(daysAgo: Long, min: Double, avg: Double, max: Double) =
        DailySummary(HeartRate.id, today.minusDays(daysAgo), mapOf("value" to DayValue.Range(min, avg, max)), emptySet())

    private val steps = MetricDefinition(
        id = MetricId("steps"),
        label = "Steps",
        subCategory = SubCategory.LifestyleActivity,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_favorite,
        sources = setOf(DataSource.Wearable),
        screens = setOf(NavScreen.Tracker),
        quantities = listOf(Quantity(key = "value", label = "Steps", unit = "steps")),
        aggregation = Aggregation.Sum,
        chartKind = ChartKind.Bar,
        goal = Goal(8000.0, Goal.Direction.AtLeast),
    )

    private fun steps(daysAgo: Long, total: Double) =
        DailySummary(steps.id, today.minusDays(daysAgo), mapOf("value" to DayValue.Total(total)), emptySet())

    @Test
    fun `a week has a slot per day ending today, a missing day is null`() {
        val spec = chartSpec(HeartRate, listOf(hr(6, 52.0, 71.4, 148.0), hr(0, 83.0, 101.0, 130.0)), ChartPeriod.Week, today)!!

        assertEquals(7, spec.slots.size)
        assertEquals(today.minusDays(6)..today.minusDays(6), spec.slots.first().days)
        assertEquals(ChartPoint(52.0, 71.4, 148.0), spec.slots.first().point)
        assertEquals(listOf(true, false, false, false, false, false, true), spec.slots.map { it.point != null })
        assertEquals(Category.Vitals, spec.category)
        assertEquals(60.0..100.0, spec.band)
        assertEquals(true, spec.markAverage)
    }

    @Test
    fun `a month has 30 daily slots, 3 months 13 weekly ones ending today`() {
        assertEquals(30, chartSpec(HeartRate, emptyList(), ChartPeriod.Month, today)!!.slots.size)
        val quarter = chartSpec(HeartRate, emptyList(), ChartPeriod.ThreeMonths, today)!!
        assertEquals(13, quarter.slots.size)
        assertEquals(today.minusDays(6)..today, quarter.slots.last().days)
        assertEquals(today.minusDays(90), quarter.slots.first().days.start)
    }

    @Test
    fun `a week slot holds the mean of its days and the extremes of their range`() {
        val spec = chartSpec(HeartRate, listOf(hr(0, 60.0, 80.0, 120.0), hr(3, 50.0, 70.0, 150.0), hr(9, 40.0, 90.0, 190.0)), ChartPeriod.ThreeMonths, today)!!

        assertEquals(ChartPoint(50.0, 75.0, 150.0), spec.slots.last().point)
        assertEquals(ChartPoint(40.0, 90.0, 190.0), spec.slots[11].point)
    }

    @Test
    fun `blood pressure spans diastolic to systolic, without an average dot or a single band`() {
        val day = DailySummary(BloodPressure.id, today, mapOf("systolic" to DayValue.Mean(124.0), "diastolic" to DayValue.Mean(78.0)), emptySet())
        val spec = chartSpec(BloodPressure, listOf(day), ChartPeriod.Week, today)!!

        assertEquals(78.0, spec.slots.last().point!!.low, 0.0)
        assertEquals(124.0, spec.slots.last().point!!.high, 0.0)
        assertFalse(spec.markAverage)
        assertNull(spec.band)
    }

    @Test
    fun `a line uses the day's value, and a metric without a chart gets none`() {
        val rest = DailySummary(RestingHeartRate.id, today, mapOf("value" to DayValue.Mean(64.0)), emptySet())
        assertEquals(ChartPoint(64.0, 64.0, 64.0), chartSpec(RestingHeartRate, listOf(rest), ChartPeriod.Week, today)!!.slots.last().point)
        assertNull(chartSpec(EcgTrace, emptyList(), ChartPeriod.Week, today))
    }

    @Test
    fun `bars start at zero and reach above the goal, others cover the band on round steps`() {
        val bars = chartSpec(steps, listOf(steps(1, 6000.0)), ChartPeriod.Week, today)!!
        assertEquals(0.0, bars.yBounds()!!.start, 0.0)
        assert(bars.yBounds()!!.endInclusive >= 8800.0)

        val hrSpec = chartSpec(HeartRate, listOf(hr(1, 52.0, 71.0, 148.0)), ChartPeriod.Week, today)!!
        assertEquals(40.0..160.0, hrSpec.yBounds())
        assertNull(chartSpec(HeartRate, emptyList(), ChartPeriod.Week, today)!!.yBounds())
    }

    @Test
    fun `below-goal slots are known, so they can be dimmed`() {
        val spec = chartSpec(steps, listOf(steps(2, 9000.0), steps(1, 7999.0)), ChartPeriod.Week, today)!!

        assertEquals(true, spec.meetsGoal(4))
        assertEquals(false, spec.meetsGoal(5))
        assertNull(spec.meetsGoal(6))
        assertEquals("Goal 8,000", spec.referenceLabel())
    }

    @Test
    fun `labels are weekday letters for a week, every 7th day for a month, every 4th week for 3 months`() {
        val week = chartSpec(HeartRate, emptyList(), ChartPeriod.Week, today)!!
        assertEquals(listOf("W", "T", "F", "S", "S", "M", "T"), week.slots.indices.map { week.slotLabel(it, en) })

        assertEquals(1 to 0, week.labelSpacing to week.labelOffset)

        // Labeled slots are counted back from today, so today always has its label.
        val month = chartSpec(HeartRate, emptyList(), ChartPeriod.Month, today)!!
        assertEquals(7 to 1, month.labelSpacing to month.labelOffset) // slots 1, 8, 15, 22, 29
        assertEquals("20", month.slotLabel(29, en))
        assertEquals("13", month.slotLabel(22, en))

        val quarter = chartSpec(HeartRate, emptyList(), ChartPeriod.ThreeMonths, today)!!
        assertEquals(4 to 0, quarter.labelSpacing to quarter.labelOffset) // slots 0, 4, 8, 12
        assertEquals("20 Oct", quarter.slotLabel(12, en))
        assertEquals("22 Sep", quarter.slotLabel(8, en))
    }

    @Test
    fun `header dates and the band's reference line`() {
        assertEquals("14 – 20 Oct", chartSpec(HeartRate, emptyList(), ChartPeriod.Week, today)!!.rangeLabel(en))
        assertEquals("21 Sep – 20 Oct", chartSpec(HeartRate, emptyList(), ChartPeriod.Month, today)!!.rangeLabel(en))
        assertEquals("Normal 60–100", chartSpec(HeartRate, emptyList(), ChartPeriod.Week, today)!!.referenceLabel())
    }

    @Test
    fun `tooltips read their own slot, by day or by week`() {
        val week = chartSpec(HeartRate, listOf(hr(6, 52.0, 71.4, 148.0)), ChartPeriod.Week, today)!!
        assertEquals("Wed 14 · 52–148 bpm · avg 71", week.tooltip(0, en))
        assertNull(week.tooltip(1, en))

        val quarter = chartSpec(steps, listOf(steps(0, 6000.0), steps(1, 8000.0)), ChartPeriod.ThreeMonths, today)!!
        assertEquals("14–20 Oct · avg 7,000 steps a day", quarter.tooltip(12, en))
        assertEquals("Tue 20 · 6,000 steps", chartSpec(steps, listOf(steps(0, 6000.0)), ChartPeriod.Week, today)!!.tooltip(6, en))
    }

    private fun night(daysAgo: Long, asleep: Double, vararg stages: Pair<String, Double>) =
        DailySummary(SleepDuration.id, today.minusDays(daysAgo), mapOf("value" to DayValue.Total(asleep)) + stages.map { (k, v) -> k to DayValue.Total(v) }, emptySet())

    @Test
    fun `a stacked bar is the night's length, split in proportion to its stages`() {
        val spec = chartSpec(
            SleepDuration,
            listOf(
                night(1, 420.0, "deep" to 60.0, "rem" to 90.0, "light" to 250.0, "awake" to 30.0), // stages add up to 400
                night(0, 400.0),
            ),
            ChartPeriod.Week,
            today,
        )!!

        assertEquals(listOf("Deep", "REM", "Light"), spec.parts.map { it.label })
        assertEquals(listOf(63.0, 94.5, 262.5), spec.slots[5].point!!.parts)
        assertEquals(emptyList<Double>(), spec.slots[6].point!!.parts) // no stages: one plain bar
        assertEquals("Goal 8h", spec.referenceLabel())
        assertEquals(0.0, spec.yBounds()!!.start, 0.0)
        assertEquals("Mon 19 · 7h · Deep 1h 3m · REM 1h 35m · Light 4h 23m", spec.tooltip(5, en))
    }

    @Test
    fun `a week of nights averages their split`() {
        val nights = listOf(night(0, 400.0, "deep" to 100.0, "rem" to 100.0, "light" to 200.0), night(1, 300.0))
        val point = chartSpec(SleepDuration, nights, ChartPeriod.ThreeMonths, today)!!.slots.last().point!!

        assertEquals(350.0, point.value, 0.0)
        assertEquals(listOf(87.5, 87.5, 175.0), point.parts)
    }

    @Test
    fun `a line breaks into runs at each gap`() {
        assertEquals(listOf(listOf(0, 1), listOf(3, 4, 5), listOf(9)), runs(listOf(0, 1, 3, 4, 5, 9)))
        assertEquals(emptyList<List<Int>>(), runs(emptyList()))
    }
}
