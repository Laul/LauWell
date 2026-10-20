package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleKey
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HealthConnectPermissionsTest {

    @Test
    fun `the manifest declares exactly the health permissions the registry reads`() {
        val declared = Regex("""android:name="(android\.permission\.health\.[A-Z_]+)"""")
            .findAll(File("src/main/AndroidManifest.xml").readText())
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(readPermissions(MetricRegistry.all), declared)
    }

    @Test
    fun `heart rate asks for its samples and the resting rate`() {
        assertEquals(
            setOf("android.permission.health.READ_HEART_RATE", "android.permission.health.READ_RESTING_HEART_RATE"),
            readPermissions(listOf(CardiovascularMetrics.HeartRate)),
        )
    }

    @Test
    fun `a switched-off metric asks for nothing`() {
        val visible = MetricRegistry.visible(setOf(ToggleKey.OfMetric(CardiovascularMetrics.HeartRate.id)))
        assertTrue(readPermissions(visible).none { it.contains("HEART_RATE") })
    }
}
