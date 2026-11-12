package com.laul.lauwell.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Mood on Room: what the screens see and what gets queued for the sync.
@RunWith(AndroidJUnit4::class)
class RoomRecordRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LauWellDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.recordDao()
    private val session = MutableStateFlow<Session?>(Session("alice"))
    private val writes = SupervisorJob()
    private var syncRequests = 0
    private val now = Instant.parse("2026-11-11T20:00:00Z")
    private val repository = RoomRecordRepository(
        dao, session, CoroutineScope(writes + Dispatchers.Unconfined), { syncRequests++ }, Clock.fixed(now, ZoneOffset.UTC),
    )

    @After
    fun close() = db.close()

    private fun awaitWrites() = runBlocking { writes.children.toList().joinAll() }

    private fun moods() = runBlocking {
        (repository.measurements(Mood).first() as AppResult.Success).data
    }

    private fun mood(id: RecordId, value: Double, at: String) = Measurement(
        id = id,
        metric = Mood,
        time = Instant.parse(at),
        zoneOffset = ZoneOffset.ofHours(-5),
        values = mapOf("value" to value),
        provenance = Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse(at)),
    )

    @Test
    fun `a saved entry shows at once, oldest first, and is queued for the sync`() = runBlocking {
        val later = mood(repository.newId(), 6.0, "2026-11-10T20:00:00Z")
        val earlier = mood(repository.newId(), 4.0, "2026-11-09T20:00:00Z")
        assertEquals(AppResult.Success(Unit), repository.save(later))
        repository.save(earlier)
        awaitWrites()

        assertEquals(listOf(earlier, later), moods())
        assertEquals(2, dao.queued().size)
        assertEquals(2, syncRequests)
    }

    @Test
    fun `new ids are UUIDs, as the server requires`() {
        val id = repository.newId().value
        assertEquals(36, id.length)
        assertNotEquals(id, repository.newId().value)
    }

    @Test
    fun `a deleted entry disappears and its deletion is queued`() = runBlocking {
        val entry = mood(repository.newId(), 5.0, "2026-11-10T20:00:00Z")
        repository.save(entry)
        awaitWrites()
        dao.sent(entry.id.value, dao.queued().single().version)

        repository.delete(entry.id)
        awaitWrites()

        assertTrue(moods().isEmpty())
        assertEquals(now.toEpochMilli(), dao.rows(listOf(entry.id.value)).single().record.deletedAt)
        assertEquals(1, dao.queued().size)
    }

    @Test
    fun `nothing shows or saves while signed out, nor for another account than the tables' owner`() = runBlocking {
        repository.save(mood(repository.newId(), 5.0, "2026-11-10T20:00:00Z"))
        awaitWrites()
        dao.setCursor(SyncCursorEntity(RecordCursor, "alice", 0))

        session.value = Session("bob")
        assertTrue(moods().isEmpty())
        session.value = null
        assertTrue(moods().isEmpty())
        assertTrue(repository.save(mood(repository.newId(), 5.0, "2026-11-10T21:00:00Z")) is AppResult.Error)
    }

    private companion object {
        val Mood = MetricId("mood")
    }
}
