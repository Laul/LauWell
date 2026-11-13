package com.laul.lauwell.core.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WideWindowTest {

    @Test
    fun `the rail shows from 600 dp`() {
        assertFalse(isWideWindow(412.dp)) // a phone, or the Fold closed, upright
        assertFalse(isWideWindow(599.dp))
        assertTrue(isWideWindow(600.dp))
        assertTrue(isWideWindow(884.dp)) // the Fold open
    }
}
