package com.laul.lauwell.core.settings

import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.metrics.MindMetrics.Mood
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickLogTest {

    @Test
    fun `only metrics that exist and have an entry sheet become chips`() {
        // Fatigue, ostomy output and symptom aren't in the registry yet; blood pressure has no sheet yet.
        assertEquals(listOf(Mood), quickLogChips(DefaultQuickLog, MetricRegistry.all))
    }

    @Test
    fun `a switched-off metric gets no chip`() {
        assertEquals(emptyList<Any>(), quickLogChips(DefaultQuickLog, MetricRegistry.all - Mood))
    }
}
