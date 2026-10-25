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

class ThemeModeTest {

    @Test
    fun `stored keys round-trip and never change`() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromKey(it.key)) }
        assertEquals(listOf("light", "dark", "system"), ThemeMode.entries.map { it.key })
    }

    @Test
    fun `missing or unknown key follows the phone`() {
        assertEquals(ThemeMode.System, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.System, ThemeMode.fromKey("sepia"))
    }
}
