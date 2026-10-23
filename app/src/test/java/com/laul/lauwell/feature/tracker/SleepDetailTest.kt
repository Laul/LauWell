package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.metrics.SleepMetrics.SleepDuration
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.periodStats
import com.laul.lauwell.core.summary.metricSummary
import com.laul.lauwell.core.ui.chart.ChartPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Sleep through the shared detail and summary code: durations, nights, and the stage breakdown. */
class SleepDetailTest {

    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

    private fun night(daysAgo: Long, asleep: Double, vararg stages: Pair<String, Double>) =
        DailySummary(SleepDuration.id, today.minusDays(daysAgo), mapOf("value" to DayValue.Total(asleep)) + stages.map { (k, v) -> k to DayValue.Total(v) }, emptySet())

    private val lastNight = night(0, 432.0, "deep" to 92.0, "rem" to 100.0, "light" to 240.0, "awake" to 25.0)

    @Test
    fun `the tile and row read last night's sleep against the goal`() {
        val summary = metricSummary(SleepDuration, listOf(night(1, 500.0), lastNight), today)

        assertEquals("7h 12m", summary.value)
        assertEquals("Last night", summary.takeaway)
        assertEquals("of 8h", summary.context)
        assertEquals(0.9f, summary.progress!!, 0.0001f)
    }

    @Test
    fun `an older night says how many nights ago`() {
        assertEquals("3 nights ago", metricSummary(SleepDuration, listOf(night(3, 400.0)), today).takeaway)
    }

    @Test
    fun `the per-night list names last night, never in progress`() {
        val rows = dayRows(SleepDuration, listOf(night(1, 490.0), lastNight), ChartPeriod.Week, today)

        assertEquals(listOf("Last night", "Tue 20"), rows.map { it.label })
        assertEquals(listOf("7h 12m", "8h 10m"), rows.map { it.value })
        assertEquals(listOf(null, "goal met"), rows.map { it.note })
    }

    @Test
    fun `the stats say average, longest and nights at goal`() {
        val stats = periodStats(SleepDuration, listOf(night(1, 490.0), lastNight), ChartPeriod.Week.days(today))
        assertEquals(
            listOf("Average" to "7h 41m", "Longest" to "8h 10m", "At goal" to "1 of 2"),
            statItems(SleepDuration, stats),
        )
    }

    @Test
    fun `the breakdown gives each stage its share of the sleep, and awake beside it`() {
        val rows = breakdownRows(SleepDuration, lastNight)

        assertEquals(listOf("Deep", "REM", "Light", "Awake"), rows.map { it.label })
        assertEquals(listOf("1h 32m", "1h 40m", "4h", "25m"), rows.map { it.value })
        assertEquals(listOf("21%", "23%", "56%", null), rows.map { it.shareText })
        assertEquals(listOf(0, 1, 2, null), rows.map { it.partIndex })
    }

    @Test
    fun `a night without stages has no breakdown`() {
        assertEquals(emptyList<BreakdownRow>(), breakdownRows(SleepDuration, night(0, 400.0)))
        assertEquals(emptyList<BreakdownRow>(), breakdownRows(SleepDuration, night(0, 400.0, "awake" to 20.0)))
    }
}
