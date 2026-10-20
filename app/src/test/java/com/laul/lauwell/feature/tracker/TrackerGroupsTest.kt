package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerGroupsTest {

    private fun metric(id: String, subCategory: SubCategory, vararg screens: NavScreen = arrayOf(NavScreen.Tracker)) = MetricDefinition(
        MetricId(id), id, subCategory, StorageShape.Event, icon = 0, screens = screens.toSet(), sources = setOf(DataSource.Manual),
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
    fun `only metrics tagged for Tracker are shown`() {
        val change = metric("appliance_change", SubCategory.OstomyReplacement, NavScreen.Care)
        val skin = metric("peristomal_skin", SubCategory.OstomyDailyRoutines, NavScreen.Tracker, NavScreen.Care)
        val diagnosis = metric("diagnosis", SubCategory.RecordsMedicalHistory, NavScreen.Records)
        val pain = metric("pain", SubCategory.SymptomsPain)

        val groups = trackerGroups(listOf(change, skin, diagnosis, pain))

        assertEquals(listOf(Category.Symptoms, Category.Ostomy), groups.map { it.category })
        assertEquals(listOf(skin), groups.last().metrics)
    }
}
