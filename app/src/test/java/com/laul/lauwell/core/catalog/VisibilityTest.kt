package com.laul.lauwell.core.catalog

import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.BloodPressure
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics.HeartRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibilityTest {

    @Test
    fun `nothing switched off shows everything`() {
        assertEquals(MetricRegistry.all, MetricRegistry.visible(emptySet()))
    }

    @Test
    fun `switching off a metric hides only that metric`() {
        val off = setOf(ToggleKey.OfMetric(HeartRate.id))
        assertFalse(isVisible(HeartRate, off))
        assertTrue(isVisible(BloodPressure, off))
    }

    @Test
    fun `switching off a sub-category hides its metrics`() {
        val off = setOf(ToggleKey.OfSubCategory(SubCategory.VitalsCardiovascular))
        assertTrue(MetricRegistry.visible(off).none { it.subCategory == SubCategory.VitalsCardiovascular })
    }

    @Test
    fun `switching off a category hides its metrics`() {
        val off = setOf(ToggleKey.OfCategory(Category.Vitals))
        assertTrue(MetricRegistry.visible(off).none { it.category == Category.Vitals })
    }

    @Test
    fun `switching off something else leaves the metric visible`() {
        val off = setOf(
            ToggleKey.OfCategory(Category.Lifestyle),
            ToggleKey.OfSubCategory(SubCategory.VitalsRespiratory),
            ToggleKey.OfMetric(MetricId("steps")),
        )
        assertTrue(isVisible(HeartRate, off))
    }

    @Test
    fun `same label in two categories are different toggles`() {
        val off = setOf(ToggleKey.OfSubCategory(SubCategory.SymptomsRespiratory))
        assertFalse(ToggleKey.OfSubCategory(SubCategory.VitalsRespiratory) in off)
        assertEquals("subcategory:vitals/respiratory", ToggleKey.OfSubCategory(SubCategory.VitalsRespiratory).storageKey)
        assertEquals("subcategory:symptoms/respiratory", ToggleKey.OfSubCategory(SubCategory.SymptomsRespiratory).storageKey)
    }

    @Test
    fun `storage keys are unique across every toggle`() {
        val keys = Category.entries.map { ToggleKey.OfCategory(it).storageKey } +
            SubCategory.entries.map { ToggleKey.OfSubCategory(it).storageKey } +
            MetricRegistry.all.map { ToggleKey.OfMetric(it.id).storageKey }
        assertEquals(keys.size, keys.toSet().size)
    }
}
