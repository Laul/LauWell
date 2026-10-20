package com.laul.lauwell.core.settings

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedMetricsTest {

    private val visible = MetricRegistry.all

    @Test
    fun `the defaults show what the registry has, in their order`() {
        // Steps, sleep, fatigue and ostomy output land with their features.
        assertEquals(
            listOf(CardiovascularMetrics.HeartRate, CardiovascularMetrics.BloodPressure),
            pinnedTiles(DefaultPinned, visible),
        )
    }

    @Test
    fun `the user's order is kept and a metric shows once`() {
        val ids = listOf("blood_pressure", "heart_rate", "blood_pressure").map(::MetricId)
        assertEquals(
            listOf(CardiovascularMetrics.BloodPressure, CardiovascularMetrics.HeartRate),
            pinnedTiles(ids, visible),
        )
    }

    @Test
    fun `a hidden metric or one without a latest value is not shown`() {
        val ids = listOf("ecg_trace", "heart_rate", "blood_pressure").map(::MetricId)
        assertEquals(
            listOf(CardiovascularMetrics.HeartRate),
            pinnedTiles(ids, visible - CardiovascularMetrics.BloodPressure),
        )
    }

    @Test
    fun `nothing stored means the defaults, an empty string an empty grid`() {
        assertEquals(DefaultPinned, decodePinned(null))
        assertEquals(emptyList<MetricId>(), decodePinned(""))
    }

    @Test
    fun `stored IDs round-trip and unreadable ones are skipped`() {
        assertEquals(DefaultPinned, decodePinned(encodePinned(DefaultPinned)))
        assertEquals(listOf(MetricId("steps")), decodePinned("steps,Not An Id"))
    }
}
