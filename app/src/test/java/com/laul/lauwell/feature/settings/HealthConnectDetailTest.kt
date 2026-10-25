package com.laul.lauwell.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class HealthConnectDetailTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneId.of("America/Montreal"))

    @Test
    fun `connected reads the metric count and today's sync time`() {
        assertEquals(
            "Connected · 5 metrics · synced 09:12",
            healthConnectDetail(HealthConnectStatus.Available(5), Instant.parse("2026-10-24T13:12:00Z"), clock),
        )
    }

    @Test
    fun `an earlier sync reads its day, in the phone's zone`() {
        // 01:30 UTC on the 24th is still the 23rd in Montreal.
        assertEquals(
            "Connected · 1 metric · synced 23 Oct",
            healthConnectDetail(HealthConnectStatus.Available(1), Instant.parse("2026-10-24T01:30:00Z"), clock),
        )
    }

    @Test
    fun `never synced, nothing granted, or unavailable`() {
        assertEquals("Connected · 2 metrics", healthConnectDetail(HealthConnectStatus.Available(2), null, clock))
        assertEquals("Not connected", healthConnectDetail(HealthConnectStatus.Available(0), null, clock))
        assertEquals("Not available on this phone", healthConnectDetail(HealthConnectStatus.Unavailable(false), null, clock))
        assertEquals("Needs to be installed or updated", healthConnectDetail(HealthConnectStatus.Unavailable(true), null, clock))
        assertEquals("Checking…", healthConnectDetail(null, null, clock))
    }
}
