package com.laul.lauwell.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keeps the code and the data map in `docs/data-structure/health-data-model/` from drifting apart. The CSVs are
 * generated from `docs/data-structure/health-data-model/_src/` and are the reference; when this fails, either the
 * code or the docs are out of date.
 */
class CatalogMatchesDocsTest {

    private val dataDir = File("../docs/data-structure/health-data-model")

    private val dataPoints = readCsv("Patient Data Categories.csv")

    @Test
    fun `categories match the data map`() {
        assertEquals(
            dataPoints.map { it.getValue("Category") }.toSet(),
            Category.entries.map { it.label }.toSet(),
        )
    }

    @Test
    fun `sub-categories match the data map`() {
        assertEquals(
            dataPoints.map { it.getValue("Category") to it.getValue("Sub-category") }.toSet(),
            SubCategory.entries.map { it.category.label to it.label }.toSet(),
        )
    }

    @Test
    fun `every registered metric is a data point, filed where the data map files it`() {
        val byName = dataPoints.associateBy { it.getValue("Data").lowercase() }
        MetricRegistry.all.forEach { metric ->
            val row = byName[metric.label.lowercase()]
            assertNotNull("${metric.id}: no data point named \"${metric.label}\"", row)
            row!!
            assertEquals("${metric.id} category", row.getValue("Category"), metric.category.label)
            assertEquals("${metric.id} sub-category", row.getValue("Sub-category"), metric.subCategory.label)
            assertEquals("${metric.id} shape", row.getValue("Shape"), metric.shape.label)
            assertEquals("${metric.id} screens", row.list("Nav. Screens"), metric.screens.map { it.name }.toSet())
            assertEquals("${metric.id} sources", row.list("Data Sources"), metric.sources.map { it.name }.toSet())
        }
    }

    /** A list column, written by the generator as "A, B". */
    private fun Map<String, String>.list(column: String): Set<String> =
        getValue(column).split(", ").filter { it.isNotEmpty() }.toSet()

    private fun readCsv(name: String): List<Map<String, String>> {
        val file = File(dataDir, name)
        assertTrue("Missing ${file.absolutePath}", file.exists())
        val lines = file.readLines().filter { it.isNotBlank() }
        val header = parseLine(lines.first())
        return lines.drop(1).map { header.zip(parseLine(it)).toMap() }
    }

    /** RFC 4180 line: fields may be quoted, quotes inside are doubled. No embedded newlines here. */
    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { fields += field.toString(); field.clear() }
                else -> field.append(c)
            }
            i++
        }
        fields += field.toString()
        return fields
    }
}
