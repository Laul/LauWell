package com.laul.lauwell.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.readPermissions
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.PeriodStats
import com.laul.lauwell.core.record.daySeries
import com.laul.lauwell.core.record.periodStats
import com.laul.lauwell.core.ui.chart.ChartSpec
import com.laul.lauwell.core.ui.chart.rangeBarsSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Whether the detail screen can read the metric from Health Connect. */
sealed interface HealthAccess {
    data object Checking : HealthAccess

    /** The metric isn't read from Health Connect (logged in the app, or a document). */
    data object NotSynced : HealthAccess

    data class Unavailable(val updateRequired: Boolean) : HealthAccess

    /** [permissions] are what the grant prompt asks for. */
    data class NeedsPermission(val permissions: Set<String>) : HealthAccess

    data object Granted : HealthAccess
}

/**
 * The detail screen of a synced metric, over the last 7 days. For now (M3 chart validation test):
 * the chart exists for range-bar metrics (heart rate) only; the week / month / 3 months selector,
 * the per-day list and the other chart kinds come with the reusable chart component.
 */
data class MetricDetailState(
    val access: HealthAccess = HealthAccess.Checking,
    val today: DailySummary? = null,
    val chart: ChartSpec? = null,
    val stats: PeriodStats? = null,
    val lastSyncedAt: Instant? = null,
)

/** How often the open screen asks Health Connect for changes. */
private const val RefreshMillis = 60_000L

class MetricDetailViewModel(
    private val metric: MetricDefinition,
    private val source: HealthDataSource,
    private val sync: HealthSync,
    store: SummaryStore,
    clock: Clock,
) : ViewModel() {

    private val week: ClosedRange<LocalDate> = LocalDate.now(clock).let { it.minusDays(6)..it }

    private val access = MutableStateFlow<HealthAccess>(HealthAccess.Checking)

    val state: StateFlow<MetricDetailState> = combine(
        access,
        store.summaries(metric.id, week),
        store.lastSyncedAt(metric.id),
    ) { access, summaries, syncedAt ->
        val series = daySeries(metric, summaries, week)
        MetricDetailState(
            access = access,
            today = series.last().summary,
            chart = series.takeIf { metric.chartKind == ChartKind.RangeBar && metric.aggregation == Aggregation.MinAverageMax }
                ?.let { rangeBarsSpec(metric, it) },
            stats = metric.quantities.firstOrNull()?.let { periodStats(metric, summaries, week) },
            lastSyncedAt = syncedAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricDetailState())

    init {
        refreshAccess()
        // While access is granted: sync now, then every minute. A change of access cancels the loop.
        viewModelScope.launch {
            access.collectLatest { current ->
                while (current == HealthAccess.Granted) {
                    sync.sync(listOf(metric))
                    delay(RefreshMillis)
                }
            }
        }
    }

    /** Re-reads availability and grants: on start, after the grant prompt, and when the screen resumes. */
    fun refreshAccess() {
        viewModelScope.launch {
            access.value = when {
                metric.healthConnectTypes.isEmpty() -> HealthAccess.NotSynced
                else -> when (source.availability()) {
                    HealthConnectAvailability.Unavailable -> HealthAccess.Unavailable(updateRequired = false)
                    HealthConnectAvailability.UpdateRequired -> HealthAccess.Unavailable(updateRequired = true)
                    HealthConnectAvailability.Available -> {
                        val needed = readPermissions(listOf(metric))
                        if (source.grantedPermissions().containsAll(needed)) HealthAccess.Granted
                        else HealthAccess.NeedsPermission(needed)
                    }
                }
            }
        }
    }
}
