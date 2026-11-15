package com.laul.lauwell.feature.tracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekLabelTest {

    @Test
    fun `a week reads as its first and last day`() {
        assertEquals("10 – 16 Oct", weekLabel(LocalDate.of(2026, 10, 16)))
        assertEquals("28 Sep – 4 Oct", weekLabel(LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun `the week's dates run oldest first, ending on its last day`() {
        val dates = weekDates(LocalDate.of(2026, 10, 16))
        assertEquals(LocalDate.of(2026, 10, 10), dates.first())
        assertEquals(LocalDate.of(2026, 10, 16), dates.last())
        assertEquals(7, dates.size)
    }
}
