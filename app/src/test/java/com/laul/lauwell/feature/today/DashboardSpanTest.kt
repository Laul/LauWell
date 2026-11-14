package com.laul.lauwell.feature.today

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.ui.components.WidgetSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardSpanTest {

    @Test
    fun `steps is large, fatigue wide, the others one cell`() {
        assertEquals(WidgetSpan(2, 2), dashboardSpan(MetricId("steps")))
        assertEquals(WidgetSpan(2, 1), dashboardSpan(MetricId("fatigue")))
        assertEquals(WidgetSpan(1, 1), dashboardSpan(MetricId("resting_heart_rate")))
    }
}
