package com.laul.lauwell.core.ui.chart

import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.HeartRate
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.daySeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ChartSpecTest {

    private val monday = LocalDate.of(2026, 10, 12)
    private val week = monday..monday.plusDays(6)

    private fun hr(date: LocalDate, min: Double, avg: Double, max: Double) =
        DailySummary(HeartRate.id, date, mapOf("value" to DayValue.Range(min, avg, max)), emptySet())

    private val spec = rangeBarsSpec(
        HeartRate,
        daySeries(HeartRate, listOf(hr(monday, 52.0, 71.4, 148.0), hr(monday.plusDays(5), 62.0, 104.0, 168.0)), week),
    )

    @Test
    fun `one slot per day, a missing day is null`() {
        assertEquals(7, spec.days.size)
        assertEquals(RangeBar(52.0, 71.4, 148.0), spec.bars[0])
        assertEquals(listOf(true, false, false, false, false, true, false), spec.bars.map { it != null })
        assertEquals(Category.Vitals, spec.category)
        assertEquals(60.0..100.0, spec.band)
    }

    @Test
    fun `the y axis holds every bar and the band, on round steps with room`() {
        assertEquals(40.0..180.0, spec.yBounds())
    }

    @Test
    fun `no data, no axis`() {
        assertNull(rangeBarsSpec(HeartRate, daySeries(HeartRate, emptyList(), week)).yBounds())
    }

    @Test
    fun `the tooltip reads the slot's own day, rounded to the quantity's precision`() {
        assertEquals("Mon 12 · 52–148 bpm · avg 71", spec.tooltip(0, Locale.ENGLISH))
        assertNull(spec.tooltip(1, Locale.ENGLISH))
        assertEquals("M", spec.dayLabel(0, Locale.ENGLISH))
        assertEquals("S", spec.dayLabel(6, Locale.ENGLISH))
    }
}
