package com.laul.lauwell.core.summary

import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.BloodPressure
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.HeartRate
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.RestingHeartRate
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.ui.components.MiniChartData
import com.laul.lauwell.core.ui.components.MiniChartDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MetricSummaryTest {

    @Test
    fun `before any data, a metric shows a dash and a placeholder week of its own chart kind`() {
        val summary = MetricSummary.empty(CardiovascularMetrics.BloodPressure)

        assertNull(summary.value)
        val chart = summary.chart as MiniChartData.Range
        assertEquals(MiniChartDays, chart.days.size)
        assertTrue(chart.days.all { it == null })
    }

    @Test
    fun `a metric without a mini-chart gets none`() {
        assertNull(MetricSummary.empty(CardiovascularMetrics.EcgTrace).chart)
    }

    @Test
    fun `every chart kind has an all-placeholder empty state, never zeros`() {
        MiniChartKind.entries.forEach { kind ->
            when (val data = MiniChartData.empty(kind)) {
                is MiniChartData.Bars -> assertTrue(data.days.all { it == null })
                is MiniChartData.Line -> assertTrue(data.days.all { it == null })
                is MiniChartData.Range -> assertTrue(data.days.all { it == null })
                is MiniChartData.SeverityDots -> assertTrue(data.days.all { it == null })
                is MiniChartData.CycleStrip -> assertNull(data.day)
            }
        }
    }

    private val today = LocalDate.of(2026, 10, 20)

    private fun day(metric: MetricId, daysAgo: Long, vararg values: Pair<String, DayValue>) =
        DailySummary(metric, today.minusDays(daysAgo), values.toMap(), emptySet())

    @Test
    fun `heart rate shows today's average, its range in the takeaway and a range mini chart`() {
        val summary = metricSummary(
            HeartRate,
            listOf(
                day(HeartRate.id, 1, "value" to DayValue.Range(55.0, 70.0, 140.0)),
                day(HeartRate.id, 0, "value" to DayValue.Range(83.0, 95.4, 130.0)),
            ),
            today,
        )

        assertEquals("95", summary.value)
        // No in/above/below range: the day's average includes activity, the 60–100 range is at rest.
        assertEquals("83–130 · today", summary.takeaway)
        val chart = summary.chart as MiniChartData.Range
        assertEquals(listOf(null, null, null, null, null, 55.0..140.0, 83.0..130.0), chart.days)
        assertNull(summary.progress)
        assertEquals("Today", summary.context)
    }

    @Test
    fun `an older latest day says when, and a line keeps its gaps`() {
        val summary = metricSummary(RestingHeartRate, listOf(day(RestingHeartRate.id, 3, "value" to DayValue.Mean(58.0))), today)

        assertEquals("58", summary.value)
        assertEquals("3 days ago · below range", summary.takeaway)
        assertEquals("3 days ago", summary.context.lowercase())
        val chart = summary.chart as MiniChartData.Line
        assertEquals(listOf(null, null, null, 58.0, null, null, null), chart.days)
        assertTrue(chart.min < 58.0 && chart.max > 58.0)
    }

    @Test
    fun `blood pressure joins its values and is above range when either value is`() {
        val summary = metricSummary(
            BloodPressure,
            listOf(day(BloodPressure.id, 0, "systolic" to DayValue.Mean(124.0), "diastolic" to DayValue.Mean(78.0))),
            today,
        )

        assertEquals("124/78", summary.value)
        assertEquals("Today · above range", summary.takeaway)
        assertEquals(78.0..124.0, (summary.chart as MiniChartData.Range).days.last())
    }

    @Test
    fun `data older than the week, or none, reads as empty`() {
        val old = metricSummary(RestingHeartRate, listOf(day(RestingHeartRate.id, 9, "value" to DayValue.Mean(60.0))), today)
        assertEquals(MetricSummary.empty(RestingHeartRate), old)
        assertEquals(MetricSummary.empty(HeartRate), metricSummary(HeartRate, emptyList(), today))
    }

    @Test
    fun `a goal gives the tile its context and progress, and the bars their goal line`() {
        val steps = MetricDefinition(
            id = MetricId("steps"),
            label = "Steps",
            subCategory = SubCategory.LifestyleActivity,
            shape = StorageShape.Measurement,
            icon = R.drawable.ic_favorite,
            sources = setOf(DataSource.Wearable),
            screens = setOf(NavScreen.Tracker),
            quantities = listOf(Quantity(key = "value", label = "Steps", unit = "steps")),
            aggregation = Aggregation.Sum,
            miniChart = MiniChartKind.Bars,
            goal = Goal(8000.0, Goal.Direction.AtLeast),
        )
        val summary = metricSummary(steps, listOf(day(steps.id, 0, "value" to DayValue.Total(6000.0))), today)

        assertEquals("6,000", summary.value)
        assertEquals(0.75f, summary.progress!!, 1e-6f)
        assertTrue(summary.context.startsWith("of 8"))
        assertEquals(8000.0, (summary.chart as MiniChartData.Bars).goal!!, 0.0)
    }
}
