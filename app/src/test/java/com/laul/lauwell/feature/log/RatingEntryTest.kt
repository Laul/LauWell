package com.laul.lauwell.feature.log

import com.laul.lauwell.core.catalog.metrics.ActivityMetrics.Steps
import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class RatingEntryTest {

    private val zone = ZoneId.of("America/Montreal")
    // 00:20 on 27 Oct in Montreal.
    private val clock = Clock.fixed(Instant.parse("2026-10-27T04:20:00Z"), zone)

    @Test
    fun `no time chosen means now`() {
        assertEquals(ZonedDateTime.now(clock), entryTime(null, clock))
    }

    @Test
    fun `an earlier time is today, a later one yesterday evening`() {
        assertEquals(ZonedDateTime.of(2026, 10, 27, 0, 5, 0, 0, zone), entryTime(LocalTime.of(0, 5), clock))
        assertEquals(ZonedDateTime.of(2026, 10, 26, 23, 30, 0, 0, zone), entryTime(LocalTime.of(23, 30), clock))
    }

    @Test
    fun `a rating becomes a manual measurement in the local offset, note trimmed`() {
        val at = ZonedDateTime.of(2026, 10, 26, 21, 0, 0, 0, zone)
        val entry = ratingEntry(RecordId("m1"), Mood, 4, at, "  after a walk ", clock)

        assertEquals(Mood.id, entry.metric)
        assertEquals(mapOf("value" to 4.0), entry.values)
        assertEquals(at.toInstant(), entry.time)
        assertEquals(ZoneOffset.ofHours(-4), entry.zoneOffset)
        assertEquals(Source.Manual, entry.provenance.source)
        assertEquals("after a walk", entry.note)
        assertNull(ratingEntry(RecordId("m2"), Mood, 4, at, "   ", clock).note)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a value off the scale is refused`() {
        ratingEntry(RecordId("m1"), Mood, 6, ZonedDateTime.now(clock), "", clock)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a metric without a scale is not a rating`() {
        ratingEntry(RecordId("m1"), Steps, 3, ZonedDateTime.now(clock), "", clock)
    }
}
