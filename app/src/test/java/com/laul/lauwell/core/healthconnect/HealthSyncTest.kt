package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.BloodPressure
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.EcgTrace
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.RestingHeartRate
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.Measurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class HealthSyncTest {

    private val now = Instant.parse("2026-10-20T12:00:00Z")
    private val today = LocalDate.of(2026, 10, 20)
    private val window = today.minusDays(SyncWindowDays - 1)..today
    private val source = FakeSource()
    private val store = InMemoryStore()
    private val sync = HealthSync(source, store, Clock.fixed(now, ZoneOffset.UTC))

    private fun summary(date: LocalDate, bpm: Double) =
        DailySummary(RestingHeartRate.id, date, mapOf("value" to DayValue.Mean(bpm)), emptySet())

    @Test
    fun `the first sync takes a token, then fills the whole window`() = runBlocking {
        source.days = mapOf(today to 61.0, today.minusDays(3) to 63.0)
        val result = sync.sync(listOf(RestingHeartRate))

        assertEquals(setOf(RestingHeartRate.id), result.synced)
        assertEquals(listOf(window), source.aggregated)
        assertEquals(listOf("token-1"), source.tokensTaken)
        assertEquals(listOf(today.minusDays(3), today), store.dates(RestingHeartRate.id))
        assertEquals(SyncState("token-1", now), store.syncState(RestingHeartRate.id))
    }

    @Test
    fun `a later sync refreshes only the days that changed`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now.minusSeconds(3600)))
        source.changes = HealthChanges.Changed(setOf(today.minusDays(2), today), hadDeletions = false, nextToken = "token-2")
        source.days = mapOf(today to 60.0)

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(today.minusDays(2)..today), source.aggregated)
        assertEquals("token-2", store.syncState(RestingHeartRate.id)?.changesToken)
        assertTrue(source.tokensTaken.isEmpty())
    }

    @Test
    fun `a refreshed day that lost its data is emptied`() = runBlocking {
        store.replace(RestingHeartRate.id, window, listOf(summary(today.minusDays(1), 70.0)))
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now))
        source.changes = HealthChanges.Changed(setOf(today.minusDays(1)), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertEquals(emptyList<LocalDate>(), store.dates(RestingHeartRate.id))
    }

    @Test
    fun `a deletion refreshes the whole window`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now))
        source.changes = HealthChanges.Changed(emptySet(), hadDeletions = true, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(window), source.aggregated)
    }

    @Test
    fun `an expired token starts over`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("old", now))
        source.changes = HealthChanges.TokenExpired

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(window), source.aggregated)
        assertEquals("token-1", store.syncState(RestingHeartRate.id)?.changesToken)
    }

    @Test
    fun `nothing changed means nothing is read, but the sync time moves`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now.minusSeconds(3600)))
        source.changes = HealthChanges.Changed(emptySet(), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertTrue(source.aggregated.isEmpty())
        assertEquals(SyncState("token-2", now), store.syncState(RestingHeartRate.id))
    }

    @Test
    fun `edits older than the window are ignored`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now))
        source.changes = HealthChanges.Changed(setOf(today.minusDays(400), today), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(today..today), source.aggregated)
    }

    @Test
    fun `metrics without permission are skipped, those without Health Connect types ignored`() = runBlocking {
        source.granted = readPermissions(listOf(RestingHeartRate))
        val result = sync.sync(listOf(RestingHeartRate, BloodPressure, EcgTrace))

        assertEquals(setOf(RestingHeartRate.id), result.synced)
        assertEquals(setOf(BloodPressure.id), result.notGranted)
        assertTrue(result.failed.isEmpty())
    }

    @Test
    fun `a failing metric doesn't stop the others`() = runBlocking {
        source.failFor = BloodPressure.id
        val result = sync.sync(listOf(BloodPressure, RestingHeartRate))

        assertEquals(setOf(BloodPressure.id), result.failed)
        assertEquals(setOf(RestingHeartRate.id), result.synced)
    }

    @Test
    fun `granting past data refreshes the whole window`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now, withHistory = false))
        source.granted += HistoryPermission
        source.changes = HealthChanges.Changed(emptySet(), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(window), source.aggregated)
        assertEquals(SyncState("token-1", now, withHistory = true), store.syncState(RestingHeartRate.id))
    }

    @Test
    fun `with past data still allowed, a later sync stays incremental`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now, withHistory = true))
        source.granted += HistoryPermission
        source.changes = HealthChanges.Changed(emptySet(), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertTrue(source.aggregated.isEmpty())
        assertEquals(SyncState("token-2", now, withHistory = true), store.syncState(RestingHeartRate.id))
    }

    @Test
    fun `taking past data back refreshes the whole window too`() = runBlocking {
        store.saveSyncState(RestingHeartRate.id, SyncState("token-1", now, withHistory = true))
        source.changes = HealthChanges.Changed(emptySet(), hadDeletions = false, nextToken = "token-2")

        sync.sync(listOf(RestingHeartRate))

        assertEquals(listOf(window), source.aggregated)
    }

    @Test
    fun `nothing is read when Health Connect is unavailable`() = runBlocking {
        source.availability = HealthConnectAvailability.UpdateRequired
        val result = sync.sync(listOf(RestingHeartRate))

        assertTrue(result.unavailable)
        assertTrue(source.aggregated.isEmpty())
    }

    /** Resting heart rate per day in [days]; every other metric has no data. */
    private inner class FakeSource : HealthDataSource {
        var availability = HealthConnectAvailability.Available
        var granted: Set<String> = readPermissions(listOf(RestingHeartRate, BloodPressure))
        var days: Map<LocalDate, Double> = emptyMap()
        var changes: HealthChanges = HealthChanges.Changed(emptySet(), false, "unused")
        var failFor: MetricId? = null
        val aggregated = mutableListOf<ClosedRange<LocalDate>>()
        val tokensTaken = mutableListOf<String>()

        override fun availability() = availability
        override suspend fun grantedPermissions() = granted
        override fun supportsHistory() = true
        override suspend fun readMeasurements(metric: MetricDefinition, from: Instant, until: Instant) =
            emptyList<Measurement>()

        override suspend fun dailySummaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): List<DailySummary> {
            if (metric.id == failFor) throw SecurityException("revoked")
            aggregated += days
            if (metric.id != RestingHeartRate.id) return emptyList()
            return this.days.filterKeys { it in days }.toSortedMap().map { (date, bpm) -> summary(date, bpm) }
        }

        override suspend fun changesToken(metric: MetricDefinition) = "token-${tokensTaken.size + 1}".also { tokensTaken += it }
        override suspend fun changes(metric: MetricDefinition, token: String) = changes
    }

    private class InMemoryStore : SummaryStore {
        private val rows = MutableStateFlow<List<DailySummary>>(emptyList())
        private val states = MutableStateFlow<Map<MetricId, SyncState>>(emptyMap())

        fun dates(metric: MetricId) = rows.value.filter { it.metric == metric }.map { it.date }.sorted()

        override fun summaries(metric: MetricId, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> =
            rows.map { all -> all.filter { it.metric == metric && it.date in days }.sortedBy { it.date } }

        override suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>) {
            rows.value = rows.value.filterNot { it.metric == metric && it.date in days } + summaries
        }

        override suspend fun syncState(metric: MetricId) = states.value[metric]
        override suspend fun saveSyncState(metric: MetricId, state: SyncState) {
            states.value += metric to state
        }

        override fun lastSyncedAt(metric: MetricId) = states.map { it[metric]?.syncedAt }
    }
}
