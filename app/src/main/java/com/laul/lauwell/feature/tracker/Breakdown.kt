package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.display
import com.laul.lauwell.core.record.main
import kotlin.math.roundToInt

/**
 * One part of the latest day's breakdown on the detail screen ("Deep · 21% · 1h 32m").
 *
 * @param partIndex the part's place in the stacked chart, for its shade (`partAlpha`); null for a
 *   part beside the total (time awake), drawn neutral.
 * @param share the part's share of the total, 0..1; null beside the total.
 */
data class BreakdownRow(val label: String, val value: String, val share: Float?, val partIndex: Int?) {
    /** "21%". */
    val shareText: String? get() = share?.let { "${(it * 100).roundToInt()}%" }
}

/**
 * [day]'s breakdown parts (a night's stages) that it has values for, in the metric's order; empty
 * when it has none (an app that doesn't tell stages apart).
 */
fun breakdownRows(metric: MetricDefinition, day: DailySummary): List<BreakdownRow> {
    val stacked = metric.breakdown.filter { it.inTotal }
    val inTotal = stacked.mapNotNull { day.values[it.key]?.main }.sum()
    if (stacked.none { it.key in day.values }) return emptyList()
    return metric.breakdown.mapNotNull { part ->
        val value = day.values[part.key]?.main ?: return@mapNotNull null
        BreakdownRow(
            label = part.label,
            value = part.display(value),
            share = if (part.inTotal && inTotal > 0) (value / inTotal).toFloat() else null,
            partIndex = stacked.indexOf(part).takeIf { it >= 0 },
        )
    }
}
