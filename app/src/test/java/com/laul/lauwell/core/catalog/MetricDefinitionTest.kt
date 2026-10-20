package com.laul.lauwell.core.catalog

import androidx.health.connect.client.records.HeartRateRecord
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDefinitionTest {

    private fun quantity(key: String) = Quantity(key = key, label = key, unit = "")

    private fun metric(
        subCategory: SubCategory,
        shape: StorageShape,
        quantities: List<Quantity> = emptyList(),
        chartKind: ChartKind? = null,
        sources: Set<DataSource> = setOf(DataSource.Manual),
        screens: Set<NavScreen> = setOf(NavScreen.Tracker),
    ) = MetricDefinition(
        MetricId("x"), "X", subCategory, shape,
        icon = 0, sources = sources, screens = screens, quantities = quantities, chartKind = chartKind,
    )

    @Test(expected = IllegalArgumentException::class)
    fun `a measurement needs a quantity`() {
        metric(SubCategory.VitalsBiochemistryMetabolic, StorageShape.Measurement)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `quantity keys are unique`() {
        metric(SubCategory.VitalsBiochemistryMetabolic, StorageShape.Measurement, quantities = listOf(quantity("a"), quantity("a")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a chart needs an aggregation`() {
        metric(
            SubCategory.VitalsBiochemistryMetabolic, StorageShape.Measurement,
            quantities = listOf(quantity("a")),
            chartKind = ChartKind.Line,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `metric ids are snake_case`() {
        MetricId("Heart Rate")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a metric needs a source`() {
        metric(SubCategory.SymptomsPain, StorageShape.Event, sources = emptySet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a metric needs a screen`() {
        metric(SubCategory.SymptomsPain, StorageShape.Event, screens = emptySet())
    }

    @Test
    fun `manual when it can be logged by hand`() {
        assertEquals(false, metric(SubCategory.SymptomsPain, StorageShape.Event, sources = setOf(DataSource.Wearable)).isManual)
        assertEquals(true, metric(SubCategory.SymptomsPain, StorageShape.Event, sources = setOf(DataSource.Wearable, DataSource.Manual)).isManual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `only a wearable metric is read from Health Connect`() {
        MetricDefinition(
            MetricId("x"), "X", SubCategory.SymptomsPain, StorageShape.Event,
            icon = 0, sources = setOf(DataSource.Manual), screens = setOf(NavScreen.Tracker),
            healthConnectTypes = setOf(HeartRateRecord::class),
        )
    }

    @Test
    fun `a metric can be pinned when it has a number to summarise`() {
        assertEquals(true, CardiovascularMetrics.HeartRate.canPin)
        assertEquals(false, CardiovascularMetrics.EcgTrace.canPin)
    }

    @Test
    fun `category comes from the sub-category`() {
        val metric = metric(SubCategory.SymptomsPain, StorageShape.Event)
        assertEquals(Category.Symptoms, metric.category)
    }
}
