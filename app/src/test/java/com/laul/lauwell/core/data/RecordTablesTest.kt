package com.laul.lauwell.core.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.record.LinkRelation
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordLink
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

// Runs Room on Robolectric's SQLite: real tables, foreign keys and queries, no device.
@RunWith(AndroidJUnit4::class)
class RecordTablesTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LauWellDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.recordDao()

    @After
    fun close() = db.close()

    private val encounter = Measurement(
        id = RecordId("enc-1"),
        metric = MetricId("encounter_note"),
        time = Instant.parse("2026-10-26T13:00:00Z"),
        zoneOffset = ZoneOffset.ofHours(-4),
        values = mapOf("value" to 1.0),
        provenance = Provenance(source = Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse("2026-10-26T13:01:00Z")),
    )

    private val mood = Measurement(
        id = RecordId("r1"),
        metric = MetricId("mood"),
        time = Instant.parse("2026-10-26T13:30:00Z"),
        zoneOffset = ZoneOffset.ofHours(-4),
        values = mapOf("value" to 7.0),
        provenance = Provenance(source = Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse("2026-10-26T13:31:00Z")),
        links = listOf(RecordLink(encounter.id, LinkRelation.RecordedDuring)),
        note = "after a bad night",
    )

    private val day = Instant.parse("2026-10-26T00:00:00Z").toEpochMilli() to Instant.parse("2026-10-27T00:00:00Z").toEpochMilli()

    private fun moods() = runBlocking { dao.records("mood", day.first, day.second).first().map { measurementFrom(it) } }

    @Test
    fun `a measurement round-trips through its rows`() {
        assertEquals(mood, measurementFrom(toRows(mood)))
    }

    @Test
    fun `rows of another shape or a newer schema are skipped`() {
        val rows = toRows(mood)
        assertNull(measurementFrom(rows.copy(record = rows.record.copy(shape = "Event"))))
        assertNull(measurementFrom(rows.copy(record = rows.record.copy(schemaVersion = RecordRowsSchemaVersion + 1))))
        assertNull(measurementFrom(rows.copy(record = rows.record.copy(source = "fax"))))
    }

    @Test
    fun `a saved measurement reads back whole`() = runBlocking {
        dao.save(toRows(encounter))
        dao.save(toRows(mood))
        assertEquals(listOf(mood), moods())
    }

    @Test
    fun `saving an edit replaces the values and links, and keeps links pointing at it`() = runBlocking {
        dao.save(toRows(encounter))
        dao.save(toRows(mood))
        dao.save(toRows(encounter.copy(values = mapOf("value" to 2.0), note = "edited")))
        val edited = mood.copy(values = mapOf("value" to 5.0), links = emptyList())
        dao.save(toRows(edited))
        assertEquals(listOf(edited), moods())

        dao.save(toRows(mood))
        assertEquals(listOf(mood.id), dao.linkedTo(encounter.id.value).first().map { RecordId(it.record.id) })
    }

    @Test
    fun `a deleted record keeps its row but leaves the lists`() = runBlocking {
        dao.save(toRows(encounter))
        dao.save(toRows(mood))
        dao.markDeleted(mood.id.value, Instant.parse("2026-10-27T08:00:00Z").toEpochMilli())
        assertEquals(emptyList<Measurement>(), moods())
        assertEquals(emptyList<RecordRows>(), dao.linkedTo(encounter.id.value).first())
    }

    @Test
    fun `a link to a record that does not exist is refused`() {
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { dao.save(toRows(mood)) } }
    }
}
