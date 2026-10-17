package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerGroupsTest {

    private fun metric(id: String, subCategory: SubCategory) = MetricDefinition(
        MetricId(id), id, subCategory, StorageShape.Event, icon = 0, sources = setOf(DataSource.Manual),
    )

    @Test
    fun `groups follow the hub order, metrics keep registry order`() {
        val fatigue = metric("fatigue", SubCategory.SymptomsSystemic)
        val heartRate = metric("heart_rate", SubCategory.VitalsCardiovascular)
        val steps = metric("steps", SubCategory.LifestyleActivity)
        val bloodPressure = metric("blood_pressure", SubCategory.VitalsCardiovascular)

        val groups = trackerGroups(listOf(fatigue, heartRate, steps, bloodPressure))

        assertEquals(listOf(Category.Vitals, Category.Lifestyle, Category.Symptoms), groups.map { it.category })
        assertEquals(listOf(heartRate, bloodPressure), groups.first().metrics)
    }

    @Test
    fun `empty categories and categories outside the hub are left out`() {
        val change = metric("appliance_change", SubCategory.TreatmentsOstomy)
        val diagnosis = metric("diagnosis", SubCategory.RecordsConditions)
        val pain = metric("pain", SubCategory.SymptomsPain)

        val groups = trackerGroups(listOf(change, diagnosis, pain))

        assertEquals(listOf(Category.Symptoms), groups.map { it.category })
    }
}
