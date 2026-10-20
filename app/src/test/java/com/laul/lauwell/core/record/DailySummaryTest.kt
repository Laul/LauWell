package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.MetricId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailySummaryTest {

    private val day = LocalDate.of(2026, 10, 18)

    @Test
    fun `a summary reports how its day was aggregated`() {
        val hr = DailySummary(MetricId("heart_rate"), day, mapOf("value" to DayValue.Range(52.0, 68.0, 142.0)), emptySet())
        assertEquals(Aggregation.MinAverageMax, hr.aggregation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a day without data has no summary, rather than a zero`() {
        DailySummary(MetricId("steps"), day, emptyMap(), emptySet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `every quantity of a day is aggregated the same way`() {
        DailySummary(
            MetricId("blood_pressure"), day,
            mapOf("systolic" to DayValue.Mean(118.0, 2), "diastolic" to DayValue.Total(150.0)),
            emptySet(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a range is ordered`() {
        DayValue.Range(min = 70.0, average = 60.0, max = 140.0)
    }
}
