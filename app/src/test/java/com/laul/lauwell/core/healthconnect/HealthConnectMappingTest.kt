package com.laul.lauwell.core.healthconnect

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.metrics.ActivityMetrics
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.DeviceType
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.health.connect.client.records.metadata.DataOrigin as HcDataOrigin

class HealthConnectMappingTest {

    private val garmin = "com.garmin.android.apps.connectmobile"
    private val start = Instant.parse("2026-10-18T06:00:00Z")
    private val offset = ZoneOffset.ofHours(-4)

    private fun metadata(id: String, method: Int = Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED) = Metadata(
        id = id,
        dataOrigin = HcDataOrigin(garmin),
        lastModifiedTime = start.plusSeconds(600),
        device = Device(manufacturer = "Garmin", model = "Venu 3", type = Device.TYPE_WATCH),
        recordingMethod = method,
    )

    @Test
    fun `a heart-rate series becomes one measurement per sample`() {
        val record = HeartRateRecord(
            startTime = start,
            startZoneOffset = offset,
            endTime = start.plusSeconds(120),
            endZoneOffset = offset,
            samples = listOf(HeartRateRecord.Sample(start, 61), HeartRateRecord.Sample(start.plusSeconds(60), 64)),
            metadata = metadata("hr-1"),
        )
        val measurements = toMeasurements(CardiovascularMetrics.HeartRate.id, record)

        assertEquals(listOf(RecordId("hr-1#0"), RecordId("hr-1#1")), measurements.map { it.id })
        assertEquals(listOf(61.0, 64.0), measurements.map { it.values.getValue("value") })
        assertEquals(listOf(start, start.plusSeconds(60)), measurements.map { it.time })
        measurements.forEach { it.requireMatches(CardiovascularMetrics.HeartRate) }
    }

    @Test
    fun `blood pressure keeps both values in mmHg`() {
        val record = BloodPressureRecord(
            time = start,
            zoneOffset = offset,
            systolic = Pressure.millimetersOfMercury(118.0),
            diastolic = Pressure.millimetersOfMercury(76.0),
            metadata = metadata("bp-1", Metadata.RECORDING_METHOD_MANUAL_ENTRY),
        )
        val measurement = toMeasurements(CardiovascularMetrics.BloodPressure.id, record).single()

        assertEquals(mapOf("systolic" to 118.0, "diastolic" to 76.0), measurement.values)
        measurement.requireMatches(CardiovascularMetrics.BloodPressure)
    }

    @Test
    fun `provenance names the app and the device, and keeps a typed-in value device-sourced`() {
        val record = RestingHeartRateRecord(start, offset, 58, metadata("rhr-1", Metadata.RECORDING_METHOD_MANUAL_ENTRY))
        val provenance = toMeasurements(CardiovascularMetrics.RestingHeartRate.id, record).single().provenance

        assertEquals(Source.Device, provenance.source)
        assertEquals(RecordingMethod.Manual, provenance.recordingMethod)
        assertEquals(garmin, provenance.origin?.appPackage)
        assertEquals(DeviceType.Watch, provenance.origin?.device?.type)
        assertEquals("Venu 3", provenance.origin?.device?.model)
        assertEquals(start.plusSeconds(600), provenance.recordedAt)
    }

    private fun day(date: LocalDate, longs: Map<String, Long> = emptyMap(), doubles: Map<String, Double> = emptyMap()) =
        AggregationResultGroupedByPeriod(
            result = AggregationResult(longs, doubles, if (longs.isEmpty() && doubles.isEmpty()) emptySet() else setOf(HcDataOrigin(garmin))),
            startTime = date.atStartOfDay(),
            endTime = date.plusDays(1).atStartOfDay(),
        )

    @Test
    fun `heart rate summarises a day as its range`() {
        val date = LocalDate.of(2026, 10, 18)
        val summary = dailySummary(
            CardiovascularMetrics.HeartRate,
            day(
                date,
                longs = mapOf(
                    HeartRateRecord.BPM_MIN.metricKey to 52,
                    HeartRateRecord.BPM_AVG.metricKey to 71,
                    HeartRateRecord.BPM_MAX.metricKey to 148,
                ),
            ),
        )!!

        assertEquals(date, summary.date)
        assertEquals(mapOf("value" to DayValue.Range(52.0, 71.0, 148.0)), summary.values)
        assertEquals(setOf(DataOrigin(garmin)), summary.origins)
    }

    @Test
    fun `blood pressure summarises a day as the mean of each value`() {
        val summary = dailySummary(
            CardiovascularMetrics.BloodPressure,
            day(
                LocalDate.of(2026, 10, 18),
                doubles = mapOf(
                    BloodPressureRecord.SYSTOLIC_AVG.metricKey to 119.5,
                    BloodPressureRecord.DIASTOLIC_AVG.metricKey to 77.0,
                ),
            ),
        )!!

        assertEquals(mapOf("systolic" to DayValue.Mean(119.5), "diastolic" to DayValue.Mean(77.0)), summary.values)
    }

    @Test
    fun `steps summarise a day as Health Connect's total`() {
        val summary = dailySummary(
            ActivityMetrics.Steps,
            day(LocalDate.of(2026, 10, 18), longs = mapOf(StepsRecord.COUNT_TOTAL.metricKey to 8260)),
        )!!

        assertEquals(mapOf("value" to DayValue.Total(8260.0)), summary.values)
    }

    @Test
    fun `a walk across midnight falls on both days`() {
        val record = StepsRecord(
            startTime = Instant.parse("2026-10-19T03:50:00Z"),
            startZoneOffset = offset,
            endTime = Instant.parse("2026-10-19T04:10:00Z"),
            endZoneOffset = offset,
            count = 1200,
            metadata = metadata("steps-1"),
        )
        assertEquals(setOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)), localDays(record, ZoneOffset.UTC))
        assertEquals(1200.0, toMeasurements(ActivityMetrics.Steps.id, record).single().values.getValue("value"), 0.0)
    }

    @Test
    fun `a day without data has no summary, not a zero`() {
        assertNull(dailySummary(CardiovascularMetrics.RestingHeartRate, day(LocalDate.of(2026, 10, 18))))
    }

    @Test
    fun `every registered metric with Health Connect types has an aggregation mapping`() {
        MetricRegistry.all.filter { it.healthConnectTypes.isNotEmpty() }.forEach { metric ->
            assert(aggregateMetricsFor(metric).isNotEmpty()) { "${metric.id} has no aggregation mapping" }
        }
        assertEquals(emptySet<Any>(), aggregateMetricsFor(CardiovascularMetrics.EcgTrace))
    }

    @Test
    fun `a night's heart-rate series falls on both days, in its own offset`() {
        // 23:30 to 00:30 in Montreal (UTC-4), i.e. 03:30 to 04:30 UTC the next day.
        val record = HeartRateRecord(
            startTime = Instant.parse("2026-10-19T03:30:00Z"),
            startZoneOffset = offset,
            endTime = Instant.parse("2026-10-19T04:30:00Z"),
            endZoneOffset = offset,
            samples = emptyList(),
            metadata = metadata("hr-night"),
        )
        assertEquals(setOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)), localDays(record, ZoneOffset.UTC))
    }

    @Test
    fun `a reading without an offset uses the phone's zone`() {
        val record = RestingHeartRateRecord(Instant.parse("2026-10-19T02:00:00Z"), null, 58, metadata("rhr-2"))
        assertEquals(setOf(LocalDate.of(2026, 10, 18)), localDays(record, ZoneId.of("America/Montreal")))
    }

    @Test
    fun `reading follows page tokens until there are none`() = runBlocking {
        val pages = mapOf(null to (listOf(1, 2) to "a"), "a" to (listOf(3) to "b"), "b" to (listOf(4) to ""))
        val requested = mutableListOf<String?>()
        val all = readAllPages { token -> requested += token; pages.getValue(token) }

        assertEquals(listOf(1, 2, 3, 4), all)
        assertEquals(listOf(null, "a", "b"), requested)
    }
}
