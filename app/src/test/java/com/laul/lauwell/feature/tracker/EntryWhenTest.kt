package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class EntryWhenTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-26T18:00:00Z"), ZoneId.of("America/Montreal"))

    private fun entry(at: String) = Measurement(
        RecordId("m"), Mood.id, Instant.parse(at), ZoneOffset.ofHours(-4), mapOf("value" to 4.0),
        Provenance(Source.Manual, recordingMethod = RecordingMethod.Manual, recordedAt = Instant.parse(at)),
    )

    @Test
    fun `today, yesterday, then the date, in the entry's own offset`() {
        assertEquals("Today · 09:30", entryWhen(entry("2026-10-26T13:30:00Z"), clock))
        // 01:00 UTC on the 26th is 21:00 on the 25th where it was taken.
        assertEquals("Yesterday · 21:00", entryWhen(entry("2026-10-26T01:00:00Z"), clock))
        assertEquals("Tue 20 Oct · 21:00", entryWhen(entry("2026-10-21T01:00:00Z"), clock))
    }
}
