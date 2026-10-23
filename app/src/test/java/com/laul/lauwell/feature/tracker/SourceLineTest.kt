package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DeviceType
import com.laul.lauwell.core.record.SourceDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SourceLineTest {

    private val garmin = "com.garmin.android.apps.connectmobile"
    private val samsung = "com.sec.android.app.shealth"
    private fun watch(maker: String?, model: String?) = SourceDevice(maker, model, DeviceType.Watch)

    @Test
    fun `a device is named by manufacturer and model`() {
        assertEquals("Garmin Forerunner 265", deviceName(DataOrigin(garmin, watch("Garmin", "Forerunner 265"))))
    }

    @Test
    fun `a model that already starts with the manufacturer is not doubled`() {
        assertEquals("Google Pixel Watch 3", deviceName(DataOrigin("x", watch("google", "Google Pixel Watch 3"))))
        assertEquals("Galaxy Watch7", deviceName(DataOrigin(samsung, watch(null, "Galaxy Watch7"))))
    }

    @Test
    fun `a lowercase manufacturer is capitalised`() {
        val phone = SourceDevice("samsung", "SM-F971W", DeviceType.Phone)
        assertEquals("Samsung SM-F971W", deviceName(DataOrigin(samsung, phone)))
    }

    @Test
    fun `without a model there is no device name`() {
        assertEquals(null, deviceName(DataOrigin(garmin, watch("Garmin", " "))))
        assertEquals(null, deviceName(DataOrigin(garmin)))
    }

    @Test
    fun `sources are named by device, else by app, sorted and without repeats`() {
        val origins = setOf(
            DataOrigin(samsung),
            DataOrigin(garmin, watch("Garmin", "Forerunner 265")),
            DataOrigin(garmin, SourceDevice("Garmin", "Forerunner 265", DeviceType.Unknown)),
        )
        assertEquals(listOf("Garmin Forerunner 265", "Samsung Health"), sourceNames(origins))
    }

    @Test
    fun `an app with a known device is not also named on its own`() {
        val origins = setOf(DataOrigin(garmin), DataOrigin(garmin, watch("Garmin", "Venu 3")))
        assertEquals(listOf("Garmin Venu 3"), sourceNames(origins))
    }

    @Test
    fun `the line reads Health Connect, the sources, then the sync time`() {
        val synced = Instant.parse("2026-10-21T19:14:00Z")
        val line = sourceLine(setOf(DataOrigin(garmin, watch("Garmin", "Forerunner 265"))), synced, ZoneOffset.ofHours(2))
        assertEquals("Health Connect · Garmin Forerunner 265 · synced 21:14", line)
        assertEquals("Health Connect · not synced yet", sourceLine(emptySet(), null, ZoneOffset.UTC))
    }

    @Test
    fun `the watch icon shows only for a watch`() {
        assertTrue(fromWatch(setOf(DataOrigin(garmin, watch("Garmin", "Venu 3")))))
        assertFalse(fromWatch(setOf(DataOrigin(samsung, SourceDevice("Samsung", "Galaxy S25", DeviceType.Phone)))))
    }
}
