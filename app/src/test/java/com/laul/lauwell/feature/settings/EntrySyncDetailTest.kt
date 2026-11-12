package com.laul.lauwell.feature.settings

import com.laul.lauwell.core.sync.EntrySyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class EntrySyncDetailTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-11T20:00:00Z"), ZoneId.of("America/Montreal"))
    private val today = Instant.parse("2026-11-11T14:12:00Z")

    @Test
    fun `everything sent reads all synced, with the time`() {
        assertEquals("All synced · 09:12", entrySyncDetail(EntrySyncStatus(0, 0, today), clock))
    }

    @Test
    fun `waiting and refused changes come first, then the last sync`() {
        assertEquals(
            "2 entries waiting to sync · 1 entry couldn't be synced · last synced 09:12",
            entrySyncDetail(EntrySyncStatus(waiting = 2, refused = 1, syncedAt = today), clock),
        )
        assertEquals(
            "1 entry waiting to sync · last synced 10 Nov",
            entrySyncDetail(EntrySyncStatus(1, 0, Instant.parse("2026-11-10T15:00:00Z")), clock),
        )
    }

    @Test
    fun `before the first sync`() {
        assertEquals("Not synced yet", entrySyncDetail(EntrySyncStatus(0, 0, null), clock))
        assertEquals("3 entries waiting to sync", entrySyncDetail(EntrySyncStatus(3, 0, null), clock))
        assertEquals("Checking…", entrySyncDetail(null, clock))
    }
}
