package com.laul.lauwell.core.record

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodStatsTest {

    private val monday = LocalDate.of(2026, 10, 12)
    private val sunday = monday.plusDays(6)
    private val week = monday..sunday

    private fun hr(date: LocalDate, min: Double, avg: Double, max: Double) =
        DailySummary(HeartRate.id, date, mapOf("value" to DayValue.Range(min, avg, max)), emptySet())

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

    @Test
    fun `the series covers every day of the range, both ends, with gaps as null`() {
        val series = daySeries(HeartRate, listOf(hr(monday, 50.0, 60.0, 70.0), hr(sunday, 55.0, 65.0, 80.0)), week)

        assertEquals(7, series.size)
        assertEquals(monday, series.first().date)
        assertEquals(sunday, series.last().date)
        assertEquals(listOf(true, false, false, false, false, false, true), series.map { it.summary != null })
    }

    @Test
    fun `the range's min, average and max each come from their own field`() {
        // TrackAid wrote the average into the minimum.
        val stats = periodStats(HeartRate, listOf(hr(monday, 48.0, 62.0, 131.0), hr(monday.plusDays(2), 52.0, 70.0, 120.0)), week)

        assertEquals(DayPoint(monday, 48.0), stats.lowest)
        assertEquals(66.0, stats.average!!, 1e-9)
        assertEquals(DayPoint(monday, 131.0), stats.highest)
    }

    @Test
    fun `the last day of the range counts`() {
        // TrackAid's loop stopped one day short, so today's maximum was left out.
        val stats = periodStats(HeartRate, listOf(hr(monday, 50.0, 60.0, 100.0), hr(sunday, 50.0, 60.0, 150.0)), week)

        assertEquals(DayPoint(sunday, 150.0), stats.highest)
        assertEquals(7, stats.dayCount)
        assertEquals(2, stats.daysWithData)
    }

    @Test
    fun `days without data don't pull the average down`() {
        val stats = periodStats(steps, listOf(steps(monday, 6000.0), steps(sunday, 10000.0)), week)

        assertEquals(8000.0, stats.average!!, 1e-9)
    }

    @Test
    fun `no data gives no average and no extremes, not zeros`() {
        val stats = periodStats(HeartRate, emptyList(), week)

        assertNull(stats.average)
        assertNull(stats.lowest)
        assertNull(stats.highest)
        assertEquals(0, stats.daysWithData)
        assertEquals(0, stats.daysInRange)
    }

    @Test
    fun `summaries outside the range or of another metric are ignored`() {
        val stats = periodStats(
            HeartRate,
            listOf(hr(monday.minusDays(1), 40.0, 90.0, 190.0), hr(monday, 50.0, 60.0, 70.0), steps(monday, 9000.0)),
            week,
        )

        assertEquals(1, stats.daysWithData)
        assertEquals(DayPoint(monday, 70.0), stats.highest)
    }

    @Test
    fun `best day and days at goal, reaching the goal exactly counts`() {
        val stats = periodStats(
            steps,
            listOf(steps(monday, 8000.0), steps(monday.plusDays(1), 12000.0), steps(monday.plusDays(2), 7999.0)),
            week,
        )

        assertEquals(DayPoint(monday.plusDays(1), 12000.0), stats.highest)
        assertEquals(2, stats.daysAtGoal)
        assertNull(periodStats(HeartRate, emptyList(), week).daysAtGoal)
    }

    @Test
    fun `an at-most goal is met at or below it`() {
        val goal = Goal(30.0, Goal.Direction.AtMost)
        assertEquals(listOf(true, true, false), listOf(29.0, 30.0, 31.0).map(goal::isMetBy))
    }

    @Test
    fun `on a tie the latest day is the one shown`() {
        val stats = periodStats(steps, listOf(steps(monday, 9000.0), steps(monday.plusDays(3), 9000.0)), week)

        assertEquals(DayPoint(monday.plusDays(3), 9000.0), stats.highest)
        assertEquals(DayPoint(monday.plusDays(3), 9000.0), stats.lowest)
    }

    @Test
    fun `days in range use the daily value against the quantity's normal range`() {
        // Heart rate's normal range is 60–100 bpm; the day's average is what is compared.
        val stats = periodStats(
            HeartRate,
            listOf(hr(monday, 45.0, 59.0, 120.0), hr(monday.plusDays(1), 50.0, 60.0, 140.0), hr(monday.plusDays(2), 55.0, 101.0, 150.0)),
            week,
        )

        assertEquals(1, stats.daysInRange)
    }

    @Test
    fun `each quantity of blood pressure has its own stats`() {
        val day = DailySummary(
            BloodPressure.id,
            monday,
            mapOf("systolic" to DayValue.Mean(124.0, 2), "diastolic" to DayValue.Mean(78.0, 2)),
            emptySet(),
        )

        assertEquals(124.0, periodStats(BloodPressure, listOf(day), week, "systolic").average!!, 1e-9)
        assertEquals(78.0, periodStats(BloodPressure, listOf(day), week, "diastolic").average!!, 1e-9)
        assertEquals(0, periodStats(BloodPressure, listOf(day), week, "systolic").daysInRange) // 90–120
    }
}
