package com.laul.lauwell.core.thing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ThingTest {

    private data class Pharmacy(
        override val id: ThingId,
        override val name: String,
        override val notUsedSince: Instant? = null,
        override val note: String? = null,
        override val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        override val editedAt: Instant? = null,
        override val type: String? = "pharmacy",
        override val address: String? = null,
        override val phone: String? = null,
        override val website: String? = null,
        override val contactPerson: String? = null,
    ) : Resource

    private val corner = Pharmacy(ThingId("p1"), "Pharmaprix Mont-Royal")
    private val jean = Pharmacy(ThingId("p2"), "Jean Coutu Laurier")
    private val closed = Pharmacy(ThingId("p3"), "Pharmacie Crème", notUsedSince = Instant.parse("2026-11-03T15:00:00Z"))
    private val all = listOf(corner, jean, closed)

    private val used = mapOf(jean.id to Instant.parse("2026-11-01T10:00:00Z"), closed.id to Instant.parse("2026-11-02T10:00:00Z"))

    @Test
    fun `things not used anymore are left out, the rest most recently used first, then by name`() {
        assertEquals(listOf(jean, corner), pickerOptions(all, "", used::get))
    }

    @Test
    fun `starting again offers things not used anymore, after the ones in use`() {
        assertEquals(listOf(jean, corner, closed), pickerOptions(all, "", used::get, includeNotUsed = true))
    }

    @Test
    fun `search ignores case and accents`() {
        assertEquals(listOf(corner), pickerOptions(all, " PHARMAPRIX ", used::get))
        assertEquals(listOf(closed), pickerOptions(all, "creme", used::get, includeNotUsed = true))
    }

    @Test
    fun `the tag reads the same for every thing, with its local date`() {
        assertEquals("Not used anymore · since Tue 3 Nov", notUsedLabel(closed, ZoneId.of("America/Toronto")))
        assertNull(notUsedLabel(corner, ZoneId.of("America/Toronto")))
    }
}
