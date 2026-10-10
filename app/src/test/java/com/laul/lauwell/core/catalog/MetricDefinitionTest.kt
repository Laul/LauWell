package com.laul.lauwell.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDefinitionTest {

    private fun quantity(key: String) = Quantity(key = key, label = key, unit = "1", displayUnit = "")

    @Test(expected = IllegalArgumentException::class)
    fun `a measurement needs a quantity`() {
        MetricDefinition(MetricId("x"), "X", SubCategory.VitalsMetabolic, StorageShape.Measurement)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `quantity keys are unique`() {
        MetricDefinition(
            MetricId("x"), "X", SubCategory.VitalsMetabolic, StorageShape.Measurement,
            quantities = listOf(quantity("a"), quantity("a")),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a chart needs an aggregation`() {
        MetricDefinition(
            MetricId("x"), "X", SubCategory.VitalsMetabolic, StorageShape.Measurement,
            quantities = listOf(quantity("a")),
            chartKind = ChartKind.Line,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `at most one primary coding`() {
        val primary = Coding(Coding.System.Loinc, "1-1", "x", Coding.Role.Primary, verified = false)
        MetricDefinition(
            MetricId("x"), "X", SubCategory.RecordsDocuments, StorageShape.Document,
            codings = listOf(primary, primary.copy(code = "2-2")),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `metric ids are snake_case`() {
        MetricId("Heart Rate")
    }

    @Test
    fun `category comes from the sub-category`() {
        val metric = MetricDefinition(MetricId("x"), "X", SubCategory.SymptomsPain, StorageShape.Event)
        assertEquals(Category.Symptoms, metric.category)
    }
}
