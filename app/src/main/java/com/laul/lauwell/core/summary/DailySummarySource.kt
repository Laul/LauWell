package com.laul.lauwell.core.summary

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.daysFromReadings
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.RecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/**
 * Where a metric's daily summaries come from: the Health Connect cache (Room) for a synced metric,
 * your own entries (Room, synced to Postgres) for one you only log by hand (mood). Screens read summaries through
 * this, never knowing which.
 *
 * A metric with both sources (blood pressure, later) reads Health Connect for now; how manual and
 * device values combine is its own decision (Notion: *Manual vs device rule*).
 */
class DailySummarySource(
    private val store: SummaryStore,
    private val records: RecordRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun summaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): Flow<List<DailySummary>> =
        if (metric.healthConnectTypes.isNotEmpty() || !metric.isManual) {
            store.summaries(metric.id, days)
        } else {
            // Each day is summarised from its entries as the metric's aggregation asks; an unreadable list shows as no data.
            records.measurements(metric.id).map { result ->
                val entries = (result as? AppResult.Success)?.data.orEmpty()
                daysFromReadings(metric, entries, days, zone())
            }
        }
}
