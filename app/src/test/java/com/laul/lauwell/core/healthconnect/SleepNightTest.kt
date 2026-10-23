package com.laul.lauwell.core.healthconnect

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_AWAKE
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_DEEP
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_LIGHT
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_REM
import androidx.health.connect.client.records.metadata.Metadata
import com.laul.lauwell.core.catalog.metrics.SleepMetrics.SleepDuration
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.health.connect.client.records.metadata.DataOrigin as HcDataOrigin

class SleepNightTest {

    private val garmin = "com.garmin.android.apps.connectmobile"
    private val samsung = "com.sec.android.app.shealth"
    private val offset = ZoneOffset.ofHours(-4)
    private val night = LocalDate.of(2026, 10, 21)

    /** Local time on [night]'s evening (hour >= 18) or morning. */
    private fun at(hour: Int, minute: Int = 0): Instant =
        (if (hour >= 18) night.minusDays(1) else night).atTime(hour, minute).toInstant(offset)

    private fun session(app: String, start: Instant, end: Instant, vararg stages: Triple<Instant, Instant, Int>) = SleepSessionRecord(
        startTime = start,
        startZoneOffset = offset,
        endTime = end,
        endZoneOffset = offset,
        stages = stages.map { (s, e, type) -> SleepSessionRecord.Stage(s, e, type) },
        metadata = Metadata(id = "$app-$start", dataOrigin = HcDataOrigin(app)),
    )

    private fun aggregated(minutes: Double) =
        DailySummary(SleepDuration.id, night, mapOf("value" to DayValue.Total(minutes)), emptySet())

    @Test
    fun `a night counts on the day it ends, an evening nap on the next`() {
        assertEquals(night, nightOf(at(7), offset, offset))
        assertEquals(night, nightOf(at(17, 59), offset, offset))
        assertEquals(night, nightOf(at(23), offset, offset)) // ends 23:00 the evening before: this night
        assertEquals(night.plusDays(1), nightOf(night.atTime(18, 30).toInstant(offset), offset, offset))
    }

    @Test
    fun `a nightly day runs from 18_00 the evening before`() {
        val (from, until) = dayBounds(SleepDuration, night, offset)
        assertEquals(night.minusDays(1).atTime(18, 0).toInstant(offset), from)
        assertEquals(night.atTime(18, 0).toInstant(offset), until)
    }

    @Test
    fun `the aggregated night lands on the day after its group starts`() {
        val group = AggregationResultGroupedByPeriod(
            result = AggregationResult(mapOf(SleepSessionRecord.SLEEP_DURATION_TOTAL.metricKey to 432L * 60_000), emptyMap(), emptySet()),
            startTime = night.minusDays(1).atTime(18, 0),
            endTime = night.atTime(18, 0),
        )
        val summary = dailySummary(SleepDuration, group)!!
        assertEquals(night, summary.date)
        assertEquals(mapOf("value" to DayValue.Total(432.0)), summary.values)
    }

    @Test
    fun `a night without sleep has no summary`() {
        val group = AggregationResultGroupedByPeriod(AggregationResult(emptyMap(), emptyMap(), emptySet()), night.atTime(18, 0), night.plusDays(1).atTime(18, 0))
        assertNull(dailySummary(SleepDuration, group))
    }

    @Test
    fun `stages split the night and time awake is not counted asleep`() {
        val s = session(
            garmin, at(23), at(7),
            Triple(at(23), at(1), STAGE_TYPE_LIGHT),
            Triple(at(1), at(2, 30), STAGE_TYPE_DEEP),
            Triple(at(2, 30), at(3), STAGE_TYPE_AWAKE),
            Triple(at(3), at(5), STAGE_TYPE_LIGHT),
            Triple(at(5), at(7), STAGE_TYPE_REM),
        )
        val summary = sleepNight(aggregated(480.0), listOf(s))

        assertEquals(
            mapOf(
                "value" to DayValue.Total(450.0),
                "awake" to DayValue.Total(30.0),
                "deep" to DayValue.Total(90.0),
                "rem" to DayValue.Total(120.0),
                "light" to DayValue.Total(240.0),
            ),
            summary.values,
        )
        assertEquals(setOf(DataOrigin(garmin)), summary.origins)
    }

    @Test
    fun `with several apps, the night is the one matching the total, never a mix`() {
        val fromGarmin = session(garmin, at(23), at(7), Triple(at(23), at(7), STAGE_TYPE_LIGHT)) // 8 h
        val fromSamsung = session(samsung, at(23, 30), at(6), Triple(at(23, 30), at(6), STAGE_TYPE_DEEP)) // 6 h 30
        val summary = sleepNight(aggregated(390.0), listOf(fromGarmin, fromSamsung))

        assertEquals(DayValue.Total(390.0), summary.values["value"])
        assertEquals(DayValue.Total(390.0), summary.values["deep"])
        assertEquals(DayValue.Total(0.0), summary.values["light"])
        assertEquals(setOf(DataOrigin(samsung)), summary.origins)
    }

    @Test
    fun `without stages the night is the aggregated total`() {
        val summary = sleepNight(aggregated(415.0), listOf(session(garmin, at(23), at(6, 10))))
        assertEquals(mapOf("value" to DayValue.Total(415.0)), summary.values)
        assertEquals(setOf(DataOrigin(garmin)), summary.origins)
    }

    @Test
    fun `without sessions the aggregated night is kept as is`() {
        assertEquals(aggregated(415.0), sleepNight(aggregated(415.0), emptyList()))
    }

    @Test
    fun `a session is read for its origin and time asleep`() {
        val s = session(garmin, at(23), at(7), Triple(at(2), at(3), STAGE_TYPE_AWAKE))
        val measurement = toMeasurements(SleepDuration.id, s).single()
        assertEquals(420.0, measurement.values.getValue("value"), 0.0)
        assertEquals(at(7), measurement.time)
        assertEquals(setOf(night), localDays(s, offset))
    }
}
