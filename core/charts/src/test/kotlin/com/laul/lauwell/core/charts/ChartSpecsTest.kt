package com.laul.lauwell.core.charts

import com.laul.lauwell.core.domain.DateSpan
import com.laul.lauwell.core.domain.DaySlot
import com.laul.lauwell.core.domain.RangeSummary
import com.laul.lauwell.core.domain.TimePoint
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.Source
import com.laul.lauwell.core.model.TargetRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartSpecsTest {
    private val zone = ZoneId.of("America/New_York")
    private val monday = LocalDate.parse("2026-09-21")

    @Test
    fun `range bars carry real low and high, not a stacking difference`() {
        val spec = ChartSpecs.dailyRanges(
            Metric.BLOOD_GLUCOSE,
            listOf(DaySlot(monday, RangeSummary(min = 4.2, max = 11.0, average = 6.8, count = 12))),
            zone,
        )

        assertEquals(listOf(RangeBar(x = 0.0, low = 4.2, high = 11.0, mid = 6.8)), (spec.series as Series.RangeBars).bars)
    }

    @Test
    fun `gap days produce no bar and keep later days on their own x`() {
        val spec = ChartSpecs.dailyTotals(
            Metric.STEPS,
            listOf(DaySlot(monday, 5000.0), DaySlot(monday.plusDays(1), null), DaySlot(monday.plusDays(2), 0.0)),
            zone,
        )

        assertEquals(listOf(Bar(0.0, 5000.0), Bar(2.0, 0.0)), (spec.series as Series.Bars).bars)
    }

    @Test
    fun `day index survives the DST change`() {
        val sunday = LocalDate.parse("2026-11-01") // 25 hours long in New York
        val spec = ChartSpecs.dailyTotals(Metric.STEPS, listOf(DaySlot(sunday, 1.0), DaySlot(sunday.plusDays(1), 2.0)), zone)

        assertEquals(listOf(0.0, 1.0), (spec.series as Series.Bars).bars.map { it.x })
        assertEquals(sunday.plusDays(1), spec.xAxis.dateAt(1.0))
    }

    @Test
    fun `timeline x values keep minute precision a week in`() {
        // TrackAid stored epoch millis in a Float: about 131 s of error at today's dates.
        val origin = Instant.parse("2026-09-21T04:00:00Z")
        val reading = origin.plusSeconds(6 * 86_400L + 5 * 60L) // 6 days and 5 minutes later
        val spec = ChartSpecs.timeline(Metric.BLOOD_GLUCOSE, listOf(TimePoint(reading, 5.5)), origin, zone)

        val x = (spec.series as Series.Line).points.single().x
        assertEquals(8645.0, x)
        assertEquals(reading, spec.xAxis.timeAt(x))
    }

    @Test
    fun `daily picks range bars for readings and bars for counts`() {
        val span = DateSpan.lastDays(7, monday)
        val at = monday.atTime(9, 0).atZone(zone).toInstant()
        val src = Source(Source.MANUAL, null)

        val glucose = ChartSpecs.daily(Metric.BLOOD_GLUCOSE, listOf(Measurement(Metric.BLOOD_GLUCOSE, at, value = 5.0, source = src)), span, zone)
        val steps = ChartSpecs.daily(Metric.STEPS, listOf(Measurement(Metric.STEPS, at, value = 900.0, source = src)), span, zone)

        assertEquals(listOf(RangeBar(6.0, 5.0, 5.0, 5.0)), (glucose.series as Series.RangeBars).bars)
        assertEquals(listOf(Bar(6.0, 900.0)), (steps.series as Series.Bars).bars)
    }

    @Test
    fun `y range includes the target band`() {
        val spec = ChartSpecs.dailyRanges(
            Metric.BLOOD_GLUCOSE,
            listOf(DaySlot(monday, RangeSummary(5.0, 6.0, 5.5, 3))),
            zone,
            target = TargetRange(4.0, 9.0),
        )

        val range = requireNotNull(spec.yRange)
        assertTrue(range.start < 4.0 && range.endInclusive > 9.0, "was $range")
    }
}
