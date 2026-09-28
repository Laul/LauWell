package com.laul.lauwell.core.domain

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeriodStatsTest {
    private val monday = LocalDate.parse("2026-09-21")

    private fun day(offset: Long, summary: RangeSummary?) = DaySlot(monday.plusDays(offset), summary)

    @Test
    fun `min, max and average each land in their own field`() {
        // TrackAid wrote the average into min and left average at 0.
        val stats = listOf(
            day(0, RangeSummary(min = 5.0, max = 8.0, average = 6.0, count = 1)),
            day(1, RangeSummary(min = 4.0, max = 9.0, average = 7.0, count = 1)),
        ).rangeStats()

        assertEquals(PeriodStats(min = 4.0, max = 9.0, average = 6.5), stats)
    }

    @Test
    fun `the last day counts toward the max`() {
        // TrackAid looped `until duration` over duration + 1 days and skipped today.
        val stats = listOf(
            day(0, RangeSummary(60.0, 90.0, 70.0, 10)),
            day(1, RangeSummary(55.0, 150.0, 80.0, 10)),
        ).rangeStats()

        assertEquals(150.0, stats?.max)
    }

    @Test
    fun `average is weighted by readings, not by days`() {
        val stats = listOf(
            day(0, RangeSummary(10.0, 10.0, 10.0, count = 1)),
            day(1, RangeSummary(4.0, 4.0, 4.0, count = 3)),
        ).rangeStats()

        assertEquals(5.5, stats?.average)
    }

    @Test
    fun `gaps are ignored and all-gap periods have no stats`() {
        val withGap = listOf(day(0, null), day(1, RangeSummary(4.0, 6.0, 5.0, 2))).rangeStats()
        val allGaps = listOf(day(0, null), day(1, null)).rangeStats()

        assertEquals(PeriodStats(4.0, 6.0, 5.0), withGap)
        assertNull(allGaps)
    }

    @Test
    fun `total stats keep zero days and skip missing days`() {
        val stats = listOf(
            DaySlot(monday, 0.0),
            DaySlot(monday.plusDays(1), null),
            DaySlot(monday.plusDays(2), 3000.0),
        ).totalStats()

        assertEquals(PeriodStats(min = 0.0, max = 3000.0, average = 1500.0), stats)
    }
}
