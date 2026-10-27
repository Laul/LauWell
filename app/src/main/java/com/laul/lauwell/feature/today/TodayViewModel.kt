package com.laul.lauwell.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.settings.DefaultQuickLog
import com.laul.lauwell.core.settings.PinnedMetrics
import com.laul.lauwell.core.settings.pinnedTiles
import com.laul.lauwell.core.settings.quickLogChips
import com.laul.lauwell.core.summary.DailySummarySource
import com.laul.lauwell.core.summary.MetricSummary
import com.laul.lauwell.core.summary.metricSummaries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

class TodayViewModel(
    toggleRepository: ToggleRepository,
    pinnedMetrics: PinnedMetrics,
    summarySource: DailySummarySource,
    private val healthSync: HealthSync,
    clock: Clock,
) : ViewModel() {

    /** Null until both the pinned set and the switched-off set are read, so no tile flashes. */
    val pinned: StateFlow<List<MetricDefinition>?> =
        combine(pinnedMetrics.pinned, toggleRepository.switchedOff) { stored, switchedOff ->
            pinnedTiles(stored, MetricRegistry.visible(switchedOff))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The quick-log chips, in the default order; empty until the switched-off set is read. */
    val quickLog: StateFlow<List<MetricDefinition>> = toggleRepository.switchedOff
        .map { switchedOff -> quickLogChips(DefaultQuickLog, MetricRegistry.visible(switchedOff)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Each pinned metric's value, context line and goal progress, from its daily summaries. */
    val summaries: StateFlow<Map<MetricId, MetricSummary>> =
        metricSummaries(pinned.filterNotNull(), summarySource, clock)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Syncs the pinned metrics whose permission is granted; called whenever the screen resumes. */
    fun sync() {
        viewModelScope.launch { healthSync.sync(pinned.filterNotNull().first()) }
    }
}
