package com.laul.lauwell.core.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.data.LauWellDatabase
import com.laul.lauwell.core.data.RecordEntity
import com.laul.lauwell.core.data.RecordLinkEntity
import com.laul.lauwell.core.data.RecordRows
import com.laul.lauwell.core.data.RecordValueEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

// EntrySync on Robolectric's SQLite (real Room tables) against a fake server that keeps what it is sent.
@RunWith(AndroidJUnit4::class)
class EntrySyncTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LauWellDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.recordDao()
    private val server = FakeServer()
    private val session = MutableStateFlow<Session?>(Session("alice"))
    private val sync = EntrySync(dao, server, session)

    @After
    fun close() = db.close()

    private fun mood(value: Double = 7.0, links: List<String> = emptyList(), id: String = UUID.randomUUID().toString()) =
        RecordRows(
            record = RecordEntity(
                id = id, metricId = "mood", shape = "Measurement", timeUtc = T, zoneOffsetS = -18000,
                source = "manual", recordedAt = T + 60_000, schemaVersion = 1,
            ),
            values = listOf(RecordValueEntity(id, "value", value)),
            links = links.map { RecordLinkEntity(id, it, "recorded_during") },
        )

    private fun localMoods() = runBlocking { dao.records("mood", 0, Long.MAX_VALUE).first() }

    @Test
    fun `saved entries are sent once, then the outbox is empty`() = runBlocking {
        val a = mood()
        val b = mood()
        dao.save(a)
        dao.save(b)
        dao.markDeleted(a.record.id, T + 120_000)

        assertEquals(EntrySync.Outcome.Synced(sent = 2, refused = 0, received = 2), sync.run())
        assertEquals(listOf(a.record.id, b.record.id), server.pushed.flatten())
        assertEquals(T + 120_000, server.records.getValue(a.record.id).record.deletedAt)
        assertTrue(dao.queued().isEmpty())

        assertEquals(EntrySync.Outcome.Synced(sent = 0, refused = 0, received = 0), sync.run())
    }

    @Test
    fun `an edit made while a push is in flight is sent by the next run`() = runBlocking {
        val entry = mood(value = 3.0)
        dao.save(entry)
        server.duringPush = { dao.save(mood(value = 4.0, id = entry.record.id)) }
        sync.run()
        server.duringPush = {}

        assertEquals(1, dao.queued().size)
        sync.run()
        assertEquals(4.0, server.records.getValue(entry.record.id).values.single().value, 0.0)
    }

    @Test
    fun `a refused entry is kept with its reason and not resent until edited`() = runBlocking {
        val bad = mood()
        dao.save(bad)
        server.refuse[bad.record.id] = "invalid"

        assertEquals(EntrySync.Outcome.Synced(sent = 0, refused = 1, received = 0), sync.run())
        sync.run()
        assertEquals(1, server.pushed.size)

        server.refuse.clear()
        dao.save(mood(value = 5.0, id = bad.record.id))
        sync.run()
        assertTrue(dao.queued().isEmpty())
        assertTrue(bad.record.id in server.records)
    }

    @Test
    fun `an id the server can't read is refused without being sent`() = runBlocking {
        dao.save(mood(id = "not-a-uuid"))
        assertEquals(EntrySync.Outcome.Synced(sent = 0, refused = 1, received = 0), sync.run())
        assertTrue(server.pushed.isEmpty())
    }

    @Test
    fun `a link refused for a missing record stays queued and passes later`() = runBlocking {
        val encounter = mood()
        val entry = mood(links = listOf(encounter.record.id))
        dao.save(encounter)
        dao.save(entry)
        server.missing += encounter.record.id
        sync.run()
        assertEquals(listOf(entry.record.id), dao.queued().map { it.recordId })

        server.missing.clear()
        sync.run()
        assertTrue(dao.queued().isEmpty())
    }

    @Test
    fun `changes made on another phone come down, links to later records included`() = runBlocking {
        val encounter = mood()
        val entry = mood(links = listOf(encounter.record.id))
        // The entry's change number comes before its encounter's, as when the encounter was edited later.
        server.receive(entry)
        server.receive(encounter)

        assertEquals(EntrySync.Outcome.Synced(sent = 0, refused = 0, received = 2), sync.run())
        assertEquals(setOf(entry.record.id, encounter.record.id), localMoods().map { it.record.id }.toSet())
        assertEquals(entry.links, localMoods().single { it.record.id == entry.record.id }.links)
        assertEquals(2L, dao.cursor()?.changeSeq)

        // The next run asks only for what is new.
        sync.run()
        assertEquals(listOf(0L, 2L), server.pulledFrom)
    }

    @Test
    fun `a pulled change doesn't overwrite a local edit still waiting to be sent`() = runBlocking {
        val entry = mood(value = 1.0)
        server.receive(entry)
        sync.run()

        dao.save(mood(value = 9.0, id = entry.record.id))
        server.receive(mood(value = 2.0, id = entry.record.id))
        // Still queued after this run's push, so the pull that follows must leave it alone.
        server.refuse[entry.record.id] = MissingLink
        assertEquals(EntrySync.Outcome.Synced(sent = 0, refused = 0, received = 1), sync.run())

        assertEquals(9.0, localMoods().single().values.single().value, 0.0)
        assertEquals(1, dao.queued().size)
    }

    @Test
    fun `nothing is sent or fetched for another account, nor when signed out`() = runBlocking {
        dao.save(mood())
        sync.run()
        dao.save(mood())

        session.value = Session("bob")
        assertEquals(EntrySync.Outcome.OtherAccount, sync.run())
        session.value = null
        assertEquals(EntrySync.Outcome.SignedOut, sync.run())
        assertEquals(1, server.pushed.size)
        assertEquals(1, dao.queued().size)
    }

    /** Keeps records like the API: numbers each change, refuses on demand, pages pulls. */
    private class FakeServer : SyncApi {
        val records = linkedMapOf<String, RecordRows>()
        private val seqs = mutableMapOf<String, Long>()
        private var seq = 0L
        val pushed = mutableListOf<List<String>>()
        val pulledFrom = mutableListOf<Long>()
        val refuse = mutableMapOf<String, String>()
        val missing = mutableSetOf<String>()
        var duringPush: suspend () -> Unit = {}

        fun receive(rows: RecordRows) {
            records[rows.record.id] = rows
            seqs[rows.record.id] = ++seq
        }

        override suspend fun push(changes: List<RecordRows>): PushResult {
            pushed += changes.map { it.record.id }
            duringPush()
            val refused = mutableListOf<Refusal>()
            val accepted = mutableListOf<String>()
            for (change in changes) {
                val id = change.record.id
                when {
                    id in refuse -> refused += Refusal(id, refuse.getValue(id))
                    change.links.any { it.toId in missing } -> refused += Refusal(id, MissingLink)
                    else -> { receive(change); accepted += id }
                }
            }
            return PushResult(accepted, refused)
        }

        override suspend fun pull(after: Long, limit: Int): PullPage {
            pulledFrom += after
            val changed = seqs.entries.filter { it.value > after }.sortedBy { it.value }.take(limit)
            return PullPage(changed.map { records.getValue(it.key) }, changed.lastOrNull()?.value ?: after, changed.size == limit)
        }
    }

    private companion object {
        val T = 1_793_000_000_000L
    }
}
