package com.laul.lauwell.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keeps the code and the data map in `docs/data-structure/` from drifting apart. The CSVs are
 * generated from `docs/data-structure/_src/` and are the reference; when this fails, either the
 * code or the docs are out of date.
 */
class CatalogMatchesDocsTest {

    private val dataDir = File("../docs/data-structure")

    private val dataPoints = readCsv("Patient Data Categories.csv")
    private val codings = readCsv("Patient Data Codings.csv")

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
        val byName = dataPoints.associateBy { it.getValue("Data") }
        MetricRegistry.all.forEach { metric ->
            val row = byName[metric.label]
            assertNotNull("${metric.id}: no data point named \"${metric.label}\"", row)
            row!!
            assertEquals("${metric.id} category", row.getValue("Category"), metric.category.label)
            assertEquals("${metric.id} sub-category", row.getValue("Sub-category"), metric.subCategory.label)
            assertEquals("${metric.id} shape", row.getValue("Shape"), metric.shape.label)
        }
    }

    @Test
    fun `every code in the registry is in the codings table`() {
        MetricRegistry.all.forEach { metric ->
            val inCode = metric.codings.map { it to null } +
                metric.quantities.mapNotNull { q -> q.coding?.let { it to q.unit } }
            inCode.forEach { (coding, unit) ->
                val row = codings.singleOrNull {
                    it["Data"] == metric.label && it["System"] == coding.system.label && it["Code"] == coding.code
                }
                assertNotNull("${metric.id}: ${coding.code} is not in the codings table", row)
                row!!
                assertEquals("${coding.code} role", row.getValue("Role"), coding.role.label)
                assertEquals("${coding.code} display", row.getValue("Display"), coding.display)
                assertEquals("${coding.code} verified", row.getValue("Verified") == "yes", coding.verified)
                if (unit != null) assertEquals("${coding.code} unit", row.getValue("UCUM unit"), unit)
            }
        }
    }

    @Test
    fun `single-quantity metrics use the unit of their primary code`() {
        MetricRegistry.all.filter { it.quantities.size == 1 }.forEach { metric ->
            val primary = metric.codings.singleOrNull { it.role == Coding.Role.Primary } ?: return@forEach
            val row = codings.single { it["Data"] == metric.label && it["Code"] == primary.code }
            assertEquals("${metric.id} unit", row.getValue("UCUM unit"), metric.quantities.single().unit)
        }
    }

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
