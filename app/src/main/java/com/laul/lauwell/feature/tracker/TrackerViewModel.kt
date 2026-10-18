package com.laul.lauwell.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.settings.DisplayPreferences
import com.laul.lauwell.core.settings.TrackerView
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class TrackerViewModel(
    toggleRepository: ToggleRepository,
    private val displayPreferences: DisplayPreferences,
) : ViewModel() {

    /** Null until the switched-off set is first read, so hidden metrics never flash. */
    val groups: StateFlow<List<TrackerGroup>?> = toggleRepository.switchedOff
        .map { switchedOff -> trackerGroups(MetricRegistry.visible(switchedOff)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null until the stored preference is read, so the list never flashes before the widgets. */
    val view: StateFlow<TrackerView?> = displayPreferences.trackerView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setView(view: TrackerView) {
        viewModelScope.launch { displayPreferences.setTrackerView(view) }
    }
}
