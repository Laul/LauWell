package com.laul.lauwell.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PackWidgetsTest {

    @Test
    fun `small widgets fill the space beside a large one, then the next row`() {
        val spans = listOf(
            WidgetSpan(2, 2), // steps
            WidgetSpan(1, 1), WidgetSpan(1, 1),
            WidgetSpan(2, 1), // fatigue
            WidgetSpan(1, 1),
            WidgetSpan(4, 1), // quick log
        )
        assertEquals(
            listOf(WidgetCell(0, 0), WidgetCell(2, 0), WidgetCell(3, 0), WidgetCell(2, 1), WidgetCell(0, 2), WidgetCell(0, 3)),
            packWidgets(spans, columns = 4),
        )
    }

    @Test
    fun `a widget wider than the grid is narrowed to it`() {
        assertEquals(listOf(WidgetCell(0, 0), WidgetCell(0, 1)), packWidgets(listOf(WidgetSpan(1, 1), WidgetSpan(6, 1)), columns = 1))
    }
}
