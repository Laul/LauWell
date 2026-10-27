package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.MetricId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class RecordDocumentTest {

    private val mood = Measurement(
        id = RecordId("r1"),
        metric = MetricId("mood"),
        time = Instant.parse("2026-10-26T13:30:00Z"),
        zoneOffset = ZoneOffset.ofHours(-4),
        values = mapOf("value" to 7.0),
        provenance = Provenance(source = Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse("2026-10-26T13:31:00Z")),
        links = listOf(RecordLink(RecordId("enc-1"), LinkRelation.RecordedDuring)),
        note = "after a bad night",
    )

    @Test
    fun `a measurement round-trips through its document`() {
        assertEquals(mood, measurementFrom("r1", mood.toDocument()))
    }

    @Test
    fun `the document holds the common fields, and no category`() {
        val doc = mood.toDocument()
        assertEquals("measurement", doc["type"])
        assertEquals("mood", doc["metric"])
        assertEquals(1L, doc["schemaVersion"])
        assertEquals(Date.from(mood.time), doc["time"])
        assertEquals(-4 * 3600L, doc["zoneOffset"])
        assertEquals(mapOf("value" to 7.0), doc["values"])
        assertEquals("manual", (doc["provenance"] as Map<*, *>)["source"])
        assertEquals(listOf(mapOf("to" to "enc-1", "relation" to "recorded_during")), doc["links"])
        assertEquals("after a bad night", doc["note"])
        assertEquals(false, doc.containsKey("category"))
    }

    @Test
    fun `optional fields are left out, not stored as null`() {
        val bare = mood.copy(zoneOffset = null, links = emptyList(), note = null)
        val doc = bare.toDocument()
        assertEquals(false, doc.containsKey("zoneOffset"))
        assertEquals(false, doc.containsKey("note"))
        assertEquals(false, (doc["provenance"] as Map<*, *>).containsKey("editedAt"))
        assertEquals(bare, measurementFrom("r1", doc))
    }

    @Test
    fun `whole numbers read back from Firestore as Long still become values`() {
        val doc = mood.toDocument() + mapOf("values" to mapOf("value" to 7L), "zoneOffset" to -14400L)
        assertEquals(7.0, measurementFrom("r1", doc)!!.values.getValue("value"), 0.0)
    }

    @Test
    fun `other types, newer schemas and malformed documents are skipped`() {
        assertNull(measurementFrom("r1", mood.toDocument() + ("type" to "event")))
        assertNull(measurementFrom("r1", mood.toDocument() + ("schemaVersion" to 2L)))
        assertNull(measurementFrom("r1", mood.toDocument() - "time"))
        assertNull(measurementFrom("r1", mood.toDocument() + ("values" to "seven")))
        assertNull(measurementFrom("r1", emptyMap()))
    }
}
