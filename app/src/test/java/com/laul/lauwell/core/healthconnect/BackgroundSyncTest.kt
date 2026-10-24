package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.catalog.EverythingOnToggleRepository
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.Measurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BackgroundSyncTest {

    private val source = FakeSource()
    private val session = MutableStateFlow<Session?>(Session("uid"))
    private val schedule = FakeSchedule()
    private val background = BackgroundSync(
        HealthSync(source, NoStore, Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)),
        source,
        EverythingOnToggleRepository,
        session,
        schedule,
    )

    private val visibleSynced = MetricRegistry.all.filter { it.healthConnectTypes.isNotEmpty() }.map { it.id }.toSet()

    @Test
    fun `coming to the foreground syncs every visible metric, then starts the periodic sync if allowed`() = runBlocking {
        source.granted = readPermissions(MetricRegistry.all) + BackgroundPermission
        background.onForeground()

        assertEquals(visibleSynced, source.read)
        assertEquals(true, schedule.running)
    }

    @Test
    fun `without the background permission the periodic sync is stopped and reads nothing`() = runBlocking {
        source.granted = readPermissions(MetricRegistry.all)
        schedule.running = true
        background.onForeground()
        assertEquals(false, schedule.running)

        source.read.clear()
        background.runPeriodic()
        assertTrue(source.read.isEmpty())
    }

    @Test
    fun `signed out, nothing is read and the periodic sync stops`() = runBlocking {
        source.granted = readPermissions(MetricRegistry.all) + BackgroundPermission
        session.value = null
        schedule.running = true

        background.onForeground()
        background.runPeriodic()

        assertTrue(source.read.isEmpty())
        assertEquals(false, schedule.running)
    }

    @Test
    fun `a periodic run syncs the visible metrics when background reads are granted`() = runBlocking {
        source.granted = readPermissions(MetricRegistry.all) + BackgroundPermission
        background.runPeriodic()

        assertEquals(visibleSynced, source.read)
    }

    @Test
    fun `a Health Connect failure on app start doesn't escape`() = runBlocking {
        source.failGrants = true
        background.onForeground()
    }

    private class FakeSchedule : SyncSchedule {
        var running: Boolean? = null
        override fun start() { running = true }
        override fun stop() { running = false }
    }

    /** Records which metrics were read; has no data. */
    private class FakeSource : HealthDataSource {
        var granted: Set<String> = emptySet()
        var failGrants = false
        val read = mutableSetOf<MetricId>()

        override fun availability() = HealthConnectAvailability.Available
        override suspend fun grantedPermissions(): Set<String> {
            if (failGrants) throw IllegalStateException("Health Connect busy")
            return granted
        }
        override fun supportsHistory() = true
        override fun supportsBackgroundReads() = true
        override suspend fun readMeasurements(metric: MetricDefinition, from: Instant, until: Instant) =
            emptyList<Measurement>()

        override suspend fun dailySummaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): List<DailySummary> {
            read += metric.id
            return emptyList()
        }

        override suspend fun changesToken(metric: MetricDefinition) = "token"
        override suspend fun changes(metric: MetricDefinition, token: String) = HealthChanges.TokenExpired
    }

    private object NoStore : SummaryStore {
        override fun summaries(metric: MetricId, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> = flowOf(emptyList())
        override suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>) = Unit
        override suspend fun syncState(metric: MetricId): SyncState? = null
        override suspend fun saveSyncState(metric: MetricId, state: SyncState) = Unit
        override fun lastSyncedAt(metric: MetricId): Flow<Instant?> = flowOf(null)
    }
}
