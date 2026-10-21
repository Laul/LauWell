package com.laul.lauwell.core.data

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HealthCacheTest {

    private val day = LocalDate.of(2026, 10, 19)
    private val garmin = DataOrigin("com.garmin.android.apps.connectmobile")
    private val samsung = DataOrigin("com.sec.android.app.shealth")

    private fun roundTrip(vararg summaries: DailySummary) =
        assertEquals(summaries.toList(), toSummaries(summaries.flatMap(::toEntities)))

    @Test
    fun `every kind of day value comes back as it went in`() = roundTrip(
        DailySummary(MetricId("heart_rate"), day, mapOf("value" to DayValue.Range(52.0, 71.0, 148.0)), setOf(garmin, samsung)),
        DailySummary(MetricId("blood_pressure"), day, mapOf("systolic" to DayValue.Mean(118.0, 3), "diastolic" to DayValue.Mean(76.0, 3)), setOf(garmin)),
        DailySummary(MetricId("resting_heart_rate"), day, mapOf("value" to DayValue.Mean(61.0)), emptySet()),
        DailySummary(MetricId("steps"), day.plusDays(1), mapOf("value" to DayValue.Total(8432.0)), setOf(samsung)),
        DailySummary(MetricId("weight"), day, mapOf("value" to DayValue.Last(64.2, Instant.parse("2026-10-19T07:12:00Z"))), setOf(samsung)),
    )

    @Test
    fun `one row per quantity, each carrying the day's apps`() {
        val rows = toEntities(
            DailySummary(MetricId("blood_pressure"), day, mapOf("systolic" to DayValue.Mean(118.0), "diastolic" to DayValue.Mean(76.0)), setOf(samsung, garmin)),
        )
        assertEquals(listOf("systolic", "diastolic"), rows.map { it.quantity })
        assertEquals(setOf("com.garmin.android.apps.connectmobile,com.sec.android.app.shealth"), rows.map { it.origins }.toSet())
        assertEquals(setOf(day.toEpochDay()), rows.map { it.date }.toSet())
    }
}
