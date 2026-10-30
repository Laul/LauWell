package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.ActivityMetrics.Steps
import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import com.laul.lauwell.core.catalog.metrics.SleepMetrics.SleepDuration
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.healthconnect.HealthChanges
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.SyncState
import com.laul.lauwell.core.healthconnect.SyncWindowDays
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DayHistoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 29)
    private val windowStart = today.minusDays(SyncWindowDays - 1)

    private val source = FakeSource()
    private val store = InMemoryStore()
    private val entries = MutableStateFlow<List<Measurement>>(emptyList())
    private val records = object : RecordRepository {
        override fun newId() = RecordId("new")
        override fun measurements(metric: MetricId) = entries.map { AppResult.Success(it) }
        override fun save(record: Measurement) = AppResult.Success(Unit)
        override fun delete(id: RecordId) = AppResult.Success(Unit)
    }
    private val history = DayHistory(DailySummarySource(store, records, { ZoneOffset.UTC }), store, source, records, clock)

    @Test
    fun `days inside the sync window need no load`() = runBlocking {
        assertEquals(AppResult.Success(Unit), history.load(Steps, windowStart))
        assertTrue(source.read.isEmpty())
    }

    @Test
    fun `older days are read once, then only the gap further back`() = runBlocking {
        val first = windowStart.minusDays(90)
        history.load(Steps, first)
        history.load(Steps, first.plusDays(10)) // already there
        history.load(Steps, first.minusDays(90))

        assertEquals(listOf(first..windowStart.minusDays(1), first.minusDays(90)..first.minusDays(1)), source.read)
        assertEquals(listOf(first, first.minusDays(90)), store.dates(Steps.id).take(2).reversed())
    }

    @Test
    fun `a failed read loads nothing and is retried next time`() = runBlocking {
        source.fail = true
        assertTrue(history.load(Steps, windowStart.minusDays(30)) is AppResult.Error)
        source.fail = false
        history.load(Steps, windowStart.minusDays(30))
        assertEquals(2, source.read.size)
    }

    @Test
    fun `a logged metric has nothing to load`() = runBlocking {
        assertEquals(AppResult.Success(Unit), history.load(Mood, today.minusYears(2)))
        assertTrue(source.read.isEmpty())
    }

    @Test
    fun `where the data starts`() = runBlocking {
        source.oldest = Instant.parse("2026-08-08T22:30:00Z")
        assertEquals(LocalDate.of(2026, 8, 8), history.firstDay(Steps))
        // A night counts on the morning it ends.
        assertEquals(LocalDate.of(2026, 8, 9), history.firstDay(SleepDuration))

        entries.value = listOf(mood("2026-09-14T08:00:00Z"), mood("2026-09-02T20:00:00Z"))
        assertEquals(LocalDate.of(2026, 9, 2), history.firstDay(Mood))
    }

    private fun mood(at: String) = Measurement(
        RecordId(at), Mood.id, Instant.parse(at), ZoneOffset.UTC, mapOf("value" to 3.0),
        Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse(at)),
    )

    /** One step summary on the first day of every range read. */
    private class FakeSource : HealthDataSource {
        val read = mutableListOf<ClosedRange<LocalDate>>()
        var fail = false
        var oldest: Instant? = null

        override fun availability() = HealthConnectAvailability.Available
        override suspend fun grantedPermissions() = emptySet<String>()
        override fun supportsHistory() = true
        override fun supportsBackgroundReads() = true
        override suspend fun readMeasurements(metric: MetricDefinition, from: Instant, until: Instant) = emptyList<Measurement>()
        override suspend fun dailySummaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): List<DailySummary> {
            read += days
            if (fail) throw SecurityException("no access")
            return listOf(DailySummary(metric.id, days.start, mapOf("value" to DayValue.Total(1000.0)), emptySet()))
        }
        override suspend fun oldestRecordTime(metric: MetricDefinition) = oldest
        override suspend fun changesToken(metric: MetricDefinition) = "token"
        override suspend fun changes(metric: MetricDefinition, token: String) = HealthChanges.TokenExpired
    }

    private class InMemoryStore : SummaryStore {
        private val rows = MutableStateFlow<List<DailySummary>>(emptyList())

        fun dates(metric: MetricId) = rows.value.filter { it.metric == metric }.map { it.date }.sorted()

        override fun summaries(metric: MetricId, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> =
            rows.map { all -> all.filter { it.metric == metric && it.date in days }.sortedBy { it.date } }

        override suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>) {
            rows.value = rows.value.filterNot { it.metric == metric && it.date in days } + summaries
        }

        override suspend fun syncState(metric: MetricId): SyncState? = null
        override suspend fun saveSyncState(metric: MetricId, state: SyncState) = Unit
        override fun lastSyncedAt(metric: MetricId): Flow<Instant?> = flowOf(null)
    }
}
