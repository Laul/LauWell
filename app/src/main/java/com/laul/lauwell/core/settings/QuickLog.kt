package com.laul.lauwell.core.settings

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId

/**
 * The default quick-log set on Today (Notion → Decisions Tracker: *App navigation*): fatigue, blood
 * pressure, ostomy output, mood, symptom. Kept by ID like [DefaultPinned], so each chip appears on
 * its own once its metric can be logged here. Choosing and ordering them: Notion *Today quick-log set*.
 */
val DefaultQuickLog: List<MetricId> = listOf(
    "fatigue", "blood_pressure", "ostomy_output_volume", "mood", "symptom",
).map(::MetricId)

/** The quick-log chips: the metrics of [ids] that are [visible] and have an entry sheet (a rating, for now), in order. */
fun quickLogChips(ids: List<MetricId>, visible: List<MetricDefinition>): List<MetricDefinition> {
    val byId = visible.associateBy { it.id }
    return ids.distinct().mapNotNull { byId[it] }.filter { it.isManual && it.prompt != null }
}
