package com.laul.lauwell.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.settings.PinnedMetrics
import com.laul.lauwell.core.settings.pinnedTiles
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TodayViewModel(toggleRepository: ToggleRepository, pinnedMetrics: PinnedMetrics) : ViewModel() {

    /** Null until both the pinned set and the switched-off set are read, so no tile flashes. */
    val pinned: StateFlow<List<MetricDefinition>?> =
        combine(pinnedMetrics.pinned, toggleRepository.switchedOff) { stored, switchedOff ->
            pinnedTiles(stored, MetricRegistry.visible(switchedOff))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
