package com.laul.lauwell.feature.tracker

import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.BloodPressure
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.HeartRate
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.RestingHeartRate
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.ui.chart.ChartPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayListTest {

    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

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
        goal = Goal(8000.0, Goal.Direction.AtLeast),
    )

    private fun steps(date: LocalDate, total: Double) =
        DailySummary(steps.id, date, mapOf("value" to DayValue.Total(total)), emptySet())

    private fun hr(date: LocalDate, min: Double, avg: Double, max: Double) =
        DailySummary(HeartRate.id, date, mapOf("value" to DayValue.Range(min, avg, max)), emptySet())

    private fun rhr(date: LocalDate, bpm: Double) =
        DailySummary(RestingHeartRate.id, date, mapOf("value" to DayValue.Mean(bpm)), emptySet())

    @Test
    fun `newest first, gaps say no data, days before the first data are left out`() {
        val rows = dayRows(HeartRate, listOf(hr(today.minusDays(3), 50.0, 70.0, 140.0), hr(today, 60.0, 72.0, 118.0)), ChartPeriod.Week, today)

        assertEquals(listOf("Today", "Tue 20", "Mon 19", "Sun 18"), rows.map { it.label })
        assertEquals(listOf("60–118", null, null, "50–140"), rows.map { it.value })
        assertEquals(listOf("in progress", "no data", "no data", null), rows.map { it.note })
    }

    @Test
    fun `a period without data has no rows`() {
        assertTrue(dayRows(HeartRate, emptyList(), ChartPeriod.Month, today).isEmpty())
    }

    @Test
    fun `longer periods name the month`() {
        val rows = dayRows(HeartRate, listOf(hr(today.minusDays(20), 50.0, 70.0, 140.0)), ChartPeriod.Month, today)
        assertEquals("Thu 1 Oct", rows.last().label)
    }

    @Test
    fun `a day at goal says so, today stays in progress`() {
        val rows = dayRows(steps, listOf(steps(today.minusDays(1), 8260.0), steps(today, 9000.0), steps(today.minusDays(2), 6530.0)), ChartPeriod.Week, today)

        assertEquals(listOf("in progress", "goal met", null), rows.map { it.note })
        assertEquals(listOf(true, true, false), rows.map { it.atGoal })
    }

    @Test
    fun `a single daily reading outside its range says so, also today`() {
        val rows = dayRows(RestingHeartRate, listOf(rhr(today, 104.0), rhr(today.minusDays(1), 62.0)), ChartPeriod.Week, today)

        assertEquals(listOf("above range", null), rows.map { it.note })
    }

    @Test
    fun `blood pressure reads systolic over diastolic`() {
        val day = DailySummary(BloodPressure.id, today, mapOf("systolic" to DayValue.Mean(118.0), "diastolic" to DayValue.Mean(76.0)), emptySet())
        assertEquals("118/76", valueText(BloodPressure, day))
    }
}
