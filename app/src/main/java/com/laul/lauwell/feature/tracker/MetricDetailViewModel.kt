package com.laul.lauwell.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.readPermissions
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.PeriodStats
import com.laul.lauwell.core.record.formatValue
import com.laul.lauwell.core.summary.ago
import com.laul.lauwell.core.record.periodStats
import com.laul.lauwell.core.ui.chart.ChartSpec
import com.laul.lauwell.core.ui.chart.ChartPeriod
import com.laul.lauwell.core.ui.chart.chartSpec
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
 * The detail screen of a synced metric: today's value, and the chart and stats of the selected
 * period (week, month, 3 months, all ending today). The per-day list comes with the metric's
 * detail as designed (PLAN.md → M3).
 */
data class MetricDetailState(
    val access: HealthAccess = HealthAccess.Checking,
    val period: ChartPeriod = ChartPeriod.Week,
    /** The latest day with data, up to today; its value is the screen's headline. */
    val latest: DailySummary? = null,
    val latestWhen: String? = null,
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

    private val access = MutableStateFlow<HealthAccess>(HealthAccess.Checking)

    private val period = MutableStateFlow(ChartPeriod.Week)

    /**
     * Every day the longest period can show (plus one, in case the date changes while the screen is
     * open), read once; each period is cut from it, so switching period needs no new query.
     */
    private val summaries = LocalDate.now(clock).let { today ->
        store.summaries(metric.id, today.minusDays(ChartPeriod.entries.maxOf { it.dayCount }.toLong())..today.plusDays(1))
    }

    val state: StateFlow<MetricDetailState> = combine(
        access,
        period,
        summaries,
        store.lastSyncedAt(metric.id),
    ) { access, period, summaries, syncedAt ->
        val today = LocalDate.now(clock)
        MetricDetailState(
            access = access,
            period = period,
            latest = summaries.filter { it.date <= today }.maxByOrNull { it.date },
            latestWhen = summaries.filter { it.date <= today }.maxOfOrNull { it.date }
                ?.let { ago(it, today).replaceFirstChar { c -> c.uppercase() } },
            chart = chartSpec(metric, summaries, period, today),
            stats = metric.quantities.firstOrNull()?.let { periodStats(metric, summaries, period.days(today)) },
            lastSyncedAt = syncedAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricDetailState())

    fun setPeriod(value: ChartPeriod) {
        period.value = value
    }

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

/**
 * The stats row: for a metric with a goal (steps), "Daily average · Best day · At goal" as in the
 * design; otherwise "Lowest · Average · Highest". A value without data reads "—".
 */
internal fun statItems(metric: MetricDefinition, stats: PeriodStats): List<Pair<String, String>> {
    val precision = metric.quantities.first().precision
    fun f(value: Double?) = value?.let { formatValue(it, precision) } ?: "—"
    return if (metric.goal != null) {
        listOf(
            "Daily average" to f(stats.average),
            "Best day" to f(stats.highest?.value),
            "At goal" to "${stats.daysAtGoal ?: 0} of ${stats.daysWithData}",
        )
    } else {
        listOf("Lowest" to f(stats.lowest?.value), "Average" to f(stats.average), "Highest" to f(stats.highest?.value))
    }
}
