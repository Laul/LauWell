package com.laul.lauwell.core.domain

import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.core.model.Source
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyAggregationTest {
    private val zone = ZoneId.of("America/New_York")
    private val src = Source(Source.MANUAL, null)

    private fun reading(metric: Metric, at: String, value: Double) =
        Measurement(metric, LocalDateTime.parse(at).atZone(zone).toInstant(), value = value, source = src)

    @Test
    fun `range keeps real min, max and average per day`() {
        val span = DateSpan(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-09-22"))
        val ms = listOf(
            reading(Metric.BLOOD_GLUCOSE, "2026-09-21T08:00", 4.0),
            reading(Metric.BLOOD_GLUCOSE, "2026-09-21T12:00", 10.0),
            reading(Metric.BLOOD_GLUCOSE, "2026-09-21T20:00", 7.0),
        )

        val days = dailyRanges(ms, span, zone)

        assertEquals(RangeSummary(min = 4.0, max = 10.0, average = 7.0, count = 3), days[0].value)
    }

    @Test
    fun `a day without readings is a gap, not zero`() {
        val span = DateSpan(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-09-23"))
        val ms = listOf(
            reading(Metric.HEART_RATE, "2026-09-21T09:00", 60.0),
            reading(Metric.HEART_RATE, "2026-09-23T09:00", 80.0),
        )

        val days = dailyRanges(ms, span, zone)

        assertEquals(listOf("2026-09-21", "2026-09-22", "2026-09-23"), days.map { it.date.toString() })
        assertNull(days[1].value)
    }

    @Test
    fun `a zero-step day stays zero and a day with no records is null`() {
        val span = DateSpan(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-09-22"))
        val ms = listOf(reading(Metric.STEPS, "2026-09-21T10:00", 0.0))

        val days = dailyTotals(ms, span, zone)

        assertEquals(0.0, days[0].value)
        assertNull(days[1].value)
    }

    @Test
    fun `readings late on a DST day land on that day`() {
        // 2026-03-08 is 23 hours long in New York. A fixed 24 h slicer would push 23:30 into the next day.
        val span = DateSpan(LocalDate.parse("2026-03-08"), LocalDate.parse("2026-03-09"))
        val ms = listOf(
            reading(Metric.STEPS, "2026-03-08T01:30", 100.0),
            reading(Metric.STEPS, "2026-03-08T23:30", 50.0),
            reading(Metric.STEPS, "2026-03-09T00:30", 7.0),
        )

        val days = dailyTotals(ms, span, zone)

        assertEquals(listOf(150.0, 7.0), days.map { it.value })
    }

    @Test
    fun `lastDays includes today`() {
        val span = DateSpan.lastDays(7, LocalDate.parse("2026-09-28"))

        assertEquals(7, span.days.size)
        assertEquals(LocalDate.parse("2026-09-22"), span.first)
        assertEquals(LocalDate.parse("2026-09-28"), span.last)
    }
}
