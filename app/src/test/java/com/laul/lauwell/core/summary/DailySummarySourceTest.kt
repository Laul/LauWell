package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.ActivityMetrics.Steps
import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.SyncState
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DailySummarySourceTest {

    private val day = LocalDate.of(2026, 10, 26)
    private val steps = DailySummary(Steps.id, day, mapOf("value" to DayValue.Total(6287.0)), emptySet())

    private fun mood(id: String, at: String, value: Double) = Measurement(
        id = RecordId(id),
        metric = Mood.id,
        time = Instant.parse(at),
        zoneOffset = ZoneOffset.ofHours(-4),
        values = mapOf("value" to value),
        provenance = Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse(at)),
    )

    private val source = DailySummarySource(
        store = object : SummaryStore {
            override fun summaries(metric: MetricId, days: ClosedRange<LocalDate>) = flowOf(listOf(steps))
            override suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>) = Unit
            override suspend fun syncState(metric: MetricId): SyncState? = null
            override suspend fun saveSyncState(metric: MetricId, state: SyncState) = Unit
            override fun lastSyncedAt(metric: MetricId): Flow<Instant?> = flowOf(null)
        },
        records = object : RecordRepository {
            override fun newId() = RecordId("new")
            override fun measurements(metric: MetricId) = flowOf(
                AppResult.Success(
                    listOf(
                        mood("m1", "2026-10-26T12:00:00Z", 3.0),
                        mood("m2", "2026-10-26T20:00:00Z", 4.0),
                        mood("m3", "2026-10-20T12:00:00Z", 2.0), // before the days asked for
                    ),
                ),
            )
            override fun save(record: Measurement) = AppResult.Success(Unit)
            override fun delete(id: RecordId) = AppResult.Success(Unit)
        },
        zone = { ZoneOffset.UTC },
    )

    @Test
    fun `a synced metric reads the Health Connect cache`() = runBlocking {
        assertEquals(listOf(steps), source.summaries(Steps, day..day).first())
    }

    @Test
    fun `a manual metric reads your entries, one mean per day with its count`() = runBlocking {
        val days = source.summaries(Mood, day.minusDays(2)..day).first()
        assertEquals(listOf(day), days.map { it.date })
        assertEquals(mapOf("value" to DayValue.Mean(3.5, 2)), days.single().values)
    }
}
