package com.laul.lauwell.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.settings.DisplayPreferences
import com.laul.lauwell.core.settings.TrackerView
import com.laul.lauwell.core.summary.DailySummarySource
import com.laul.lauwell.core.summary.MetricSummary
import com.laul.lauwell.core.summary.DayHistory
import com.laul.lauwell.core.summary.WeekRow
import com.laul.lauwell.core.summary.metricSummaries
import com.laul.lauwell.core.summary.weekRow
import com.laul.lauwell.core.ui.components.MiniChartDays
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The week board's data: the week ending [end], each metric's row by ID; [isCurrent] when it ends today. */
data class WeekBoard(val end: LocalDate, val rows: Map<MetricId, WeekRow>, val isCurrent: Boolean)

/** One block of the Tracker hub: a category and its visible metrics, in registry order. */
data class TrackerGroup(val category: Category, val metrics: List<MetricDefinition>)

/**
 * The order of category blocks on the Tracker hub (PLAN.md → App navigation → Tracker). Which
 * metrics appear is decided by their `Nav. Screens` tag, not by this list; a tagged metric of a
 * category missing here would be left out, so add its category when that happens.
 */
internal val TrackerCategories = listOf(Category.Vitals, Category.Lifestyle, Category.Symptoms, Category.Ostomy)

/** Groups the visible metrics tagged for Tracker; categories with nothing to show are left out. */
internal fun trackerGroups(visible: List<MetricDefinition>): List<TrackerGroup> =
    TrackerCategories.mapNotNull { category ->
        visible.filter { it.category == category && NavScreen.Tracker in it.screens }
            .takeIf { it.isNotEmpty() }
            ?.let { TrackerGroup(category, it) }
    }

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    toggleRepository: ToggleRepository,
    private val displayPreferences: DisplayPreferences,
    summarySource: DailySummarySource,
    private val healthSync: HealthSync,
    private val dayHistory: DayHistory,
    private val clock: Clock,
) : ViewModel() {

    /** Null until the switched-off set is first read, so hidden metrics never flash. */
    val groups: StateFlow<List<TrackerGroup>?> = toggleRepository.switchedOff
        .map { switchedOff -> trackerGroups(MetricRegistry.visible(switchedOff)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null until the stored preference is read, so the list never flashes before the widgets. */
    val view: StateFlow<TrackerView?> = displayPreferences.trackerView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Each shown metric's value, takeaway and mini chart, from its daily summaries. */
    val summaries: StateFlow<Map<MetricId, MetricSummary>> =
        metricSummaries(groups.map { groups -> groups.orEmpty().flatMap { it.metrics } }, summarySource, clock)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Syncs the shown metrics whose permission is granted; called whenever the screen resumes. */
    fun sync() {
        viewModelScope.launch {
            healthSync.sync(groups.filterNotNull().first().flatMap { it.metrics })
        }
    }

    /** The last day of the week the board shows (Fold open): today until you go back. */
    private val weekEnd = MutableStateFlow(LocalDate.now(clock))

    /**
     * The week board (Fold open, design v03 18b): each shown metric's [WeekRow] for the week ending
     * [WeekBoard.end]. Null until read. Only read while the board is on screen.
     */
    val board: StateFlow<WeekBoard?> =
        combine(groups.filterNotNull().map { groups -> groups.flatMap { it.metrics } }, weekEnd, ::Pair)
            .flatMapLatest { (metrics, end) ->
                if (metrics.isEmpty()) return@flatMapLatest flowOf(WeekBoard(end, emptyMap(), isCurrent(end)))
                val days = end.minusDays(MiniChartDays - 1L)..end
                combine(metrics.map { metric -> dayHistory.days(metric, days).map { metric.id to weekRow(metric, it, end) } }) {
                    WeekBoard(end, it.toMap(), isCurrent(end))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Moves the board a week back ([weeks] = -1) or forward (+1), never past today. */
    fun moveWeek(weeks: Int) {
        val today = LocalDate.now(clock)
        val end = weekEnd.value.plusWeeks(weeks.toLong()).coerceAtMost(today)
        weekEnd.value = end
        // Days older than the synced window are read from Health Connect once, then kept.
        viewModelScope.launch {
            groups.filterNotNull().first().flatMap { it.metrics }.forEach { dayHistory.load(it, end.minusDays(MiniChartDays - 1L)) }
        }
    }

    private fun isCurrent(end: LocalDate) = !end.isBefore(LocalDate.now(clock))

    fun setView(view: TrackerView) {
        viewModelScope.launch { displayPreferences.setTrackerView(view) }
    }
}
