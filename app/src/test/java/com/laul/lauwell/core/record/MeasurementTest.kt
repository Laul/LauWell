package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class MeasurementTest {

    private val at = Instant.parse("2026-10-18T08:30:00Z")

    private val garmin = Provenance(
        source = Source.Device,
        origin = DataOrigin("com.garmin.android.apps.connectmobile", SourceDevice("Garmin", "Venu 3", DeviceType.Watch)),
        recordingMethod = RecordingMethod.Automatic,
        recordedAt = at,
    )

    private fun measurement(metric: String, values: Map<String, Double>) = Measurement(
        id = RecordId("r1"),
        metric = MetricId(metric),
        time = at,
        zoneOffset = ZoneOffset.ofHours(-4),
        values = values,
        provenance = garmin,
    )

    @Test
    fun `a heart rate sample reads as one line`() {
        val hr = measurement("heart_rate", mapOf("value" to 72.4))
        assertEquals("Heart rate 72 bpm", summaryLine(hr, CardiovascularMetrics.HeartRate))
    }

    @Test
    fun `values sharing a unit are joined with a slash, in quantity order`() {
        val bp = measurement("blood_pressure", mapOf("diastolic" to 76.0, "systolic" to 118.0))
        assertEquals("Blood pressure 118/76 mmHg", summaryLine(bp, CardiovascularMetrics.BloodPressure))
    }

    @Test
    fun `values are rounded half up to the quantity's precision`() {
        assertEquals("73", formatValue(72.5, 0))
        assertEquals("5.3", formatValue(5.25, 1))
    }

    @Test
    fun `values on screen group thousands, the same on every phone`() {
        assertEquals("8,260", displayValue(8260.0, 0))
        assertEquals("1,234.6", displayValue(1234.55, 1))
        assertEquals("73", displayValue(72.5, 0))
        assertEquals("5.0", displayValue(5.0, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a record must match its metric's quantities`() {
        measurement("blood_pressure", mapOf("systolic" to 118.0))
            .requireMatches(CardiovascularMetrics.BloodPressure)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a record is checked against its own metric`() {
        measurement("heart_rate", mapOf("value" to 72.0)).requireMatches(CardiovascularMetrics.BloodPressure)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a missing value is absent, not NaN`() {
        measurement("heart_rate", mapOf("value" to Double.NaN))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a device record names the app it came from`() {
        Provenance(source = Source.Device, recordedAt = at)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an edit can't predate the record`() {
        Provenance(source = Source.Manual, recordedAt = at, editedAt = at.minusSeconds(1))
    }
}
