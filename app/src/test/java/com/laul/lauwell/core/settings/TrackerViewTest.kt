package com.laul.lauwell.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerViewTest {

    @Test
    fun `stored keys round-trip`() {
        TrackerView.entries.forEach { assertEquals(it, TrackerView.fromKey(it.key)) }
    }

    @Test
    fun `missing or unknown key falls back to the list`() {
        assertEquals(TrackerView.List, TrackerView.fromKey(null))
        assertEquals(TrackerView.List, TrackerView.fromKey("carousel"))
    }

    @Test
    fun `stored keys never change`() {
        assertEquals("list", TrackerView.List.key)
        assertEquals("widgets", TrackerView.Widgets.key)
    }
}
