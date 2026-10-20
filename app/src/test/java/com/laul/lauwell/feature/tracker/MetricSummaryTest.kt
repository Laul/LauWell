package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.ui.components.MiniChartData
import com.laul.lauwell.core.ui.components.MiniChartDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricSummaryTest {

    @Test
    fun `before any data, a metric shows a dash and a placeholder week of its own chart kind`() {
        val summary = MetricSummary.empty(CardiovascularMetrics.BloodPressure)

        assertNull(summary.value)
        val chart = summary.chart as MiniChartData.Range
        assertEquals(MiniChartDays, chart.days.size)
        assertTrue(chart.days.all { it == null })
    }

    @Test
    fun `a metric without a mini-chart gets none`() {
        assertNull(MetricSummary.empty(CardiovascularMetrics.EcgTrace).chart)
    }

    @Test
    fun `every chart kind has an all-placeholder empty state, never zeros`() {
        MiniChartKind.entries.forEach { kind ->
            when (val data = MiniChartData.empty(kind)) {
                is MiniChartData.Bars -> assertTrue(data.days.all { it == null })
                is MiniChartData.Line -> assertTrue(data.days.all { it == null })
                is MiniChartData.Range -> assertTrue(data.days.all { it == null })
                is MiniChartData.SeverityDots -> assertTrue(data.days.all { it == null })
                is MiniChartData.CycleStrip -> assertNull(data.day)
            }
        }
    }
}
