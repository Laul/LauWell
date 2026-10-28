package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DaysFromReadingsTest {

    private val day = LocalDate.of(2026, 10, 26)

    private fun reading(at: String, value: Double) = Measurement(
        RecordId(at), Mood.id, Instant.parse(at), ZoneOffset.UTC, mapOf("value" to value),
        Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse(at)),
    )

    // Three check-ins on one day, logged out of order.
    private val readings = listOf(
        reading("2026-10-26T20:00:00Z", 5.0),
        reading("2026-10-26T08:00:00Z", 2.0),
        reading("2026-10-26T13:00:00Z", 3.0),
    )

    private fun dayAs(aggregation: Aggregation?): DayValue =
        daysFromReadings(Mood.copy(aggregation = aggregation, chartKind = null), readings, day..day, ZoneOffset.UTC)
            .single().values.getValue("value")

    @Test
    fun `each aggregation shapes the day`() {
        assertEquals(DayValue.Range(2.0, 10.0 / 3, 5.0), dayAs(Aggregation.MinAverageMax))
        assertEquals(DayValue.Mean(10.0 / 3, 3), dayAs(Aggregation.Average))
        assertEquals(DayValue.Mean(10.0 / 3, 3), dayAs(null))
        assertEquals(DayValue.Total(10.0), dayAs(Aggregation.Sum))
        assertEquals(DayValue.Last(5.0, Instant.parse("2026-10-26T20:00:00Z")), dayAs(Aggregation.Latest))
    }

    @Test
    fun `one check-in is a range of one point`() {
        val single = daysFromReadings(Mood, readings.take(1), day..day, ZoneOffset.UTC).single()
        assertEquals(DayValue.Range(5.0, 5.0, 5.0), single.values.getValue("value"))
    }
}
