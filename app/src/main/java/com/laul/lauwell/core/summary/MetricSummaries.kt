package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.ui.components.MiniChartDays
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * The [MetricSummary] of each of [metrics], kept up to date from the [store] (Tracker and Today).
 * Metrics without synced data get [MetricSummary.empty]. The days are read once per list of
 * metrics; "today" is taken again on every update, so a summary written after midnight shows on
 * the new day.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun metricSummaries(
    metrics: Flow<List<MetricDefinition>>,
    store: SummaryStore,
    clock: Clock,
): Flow<Map<MetricId, MetricSummary>> = metrics.flatMapLatest { list ->
    if (list.isEmpty()) return@flatMapLatest flowOf(emptyMap())
    val today = LocalDate.now(clock)
    // One extra day back, so the window still holds a full week if the date changes while it is open.
    val days = today.minusDays(MiniChartDays.toLong())..today.plusDays(1)
    combine(list.map { metric -> store.summaries(metric.id, days).map { metric to it } }) { pairs ->
        val now = LocalDate.now(clock)
        pairs.associate { (metric, summaries) -> metric.id to metricSummary(metric, summaries, now) }
    }
}
