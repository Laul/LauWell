package com.laul.lauwell.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.healthconnect.BackgroundPermission
import com.laul.lauwell.core.healthconnect.BackgroundSync
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.HistoryPermission
import com.laul.lauwell.core.healthconnect.readPermissions
import com.laul.lauwell.core.settings.Onboarding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One metric on the grant screen: [granted] already (shown as allowed), else [selected] to ask for. */
data class GrantRow(val metric: MetricDefinition, val granted: Boolean, val selected: Boolean)

data class GrantState(
    /** Null while checking. */
    val availability: HealthConnectAvailability? = null,
    val rows: List<GrantRow> = emptyList(),
    val supportsHistory: Boolean = false,
    val historyGranted: Boolean = false,
    val supportsBackground: Boolean = false,
    val backgroundGranted: Boolean = false,
) {
    /** The metrics the button asks for: selected and not granted yet. */
    val toAsk: List<MetricDefinition> get() = rows.filter { it.selected && !it.granted }.map { it.metric }

    /** Whether the prompt also asks for reading history (data from before the first grant). */
    val historyToAsk: Boolean get() = supportsHistory && !historyGranted && rows.any { it.selected }

    /** Whether the prompt also asks for reading in the background (the periodic sync). */
    val backgroundToAsk: Boolean get() = supportsBackground && !backgroundGranted && rows.any { it.selected }
}

/**
 * The Health Connect grant screen (design: "Connect Health Connect"): one row per visible metric
 * read from Health Connect, on by default; "Allow N permissions" asks for the selected ones, plus
 * reading history and reading in the background when Health Connect supports them, so the charts
 * can go back past 30 days and are up to date when the app opens.
 * Partial grants are fine: each metric works with its own permission.
 */
class HealthConnectGrantViewModel(
    private val toggleRepository: ToggleRepository,
    private val source: HealthDataSource,
    private val onboarding: Onboarding,
    private val backgroundSync: BackgroundSync,
) : ViewModel() {

    private val _state = MutableStateFlow(GrantState())
    val state: StateFlow<GrantState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads availability and grants: on start, after the prompt, and when the screen resumes. */
    fun refresh() {
        viewModelScope.launch {
            val availability = source.availability()
            if (availability != HealthConnectAvailability.Available) {
                _state.value = GrantState(availability = availability)
                return@launch
            }
            val granted = source.grantedPermissions()
            val metrics = MetricRegistry.visible(toggleRepository.switchedOff.first()).filter { it.healthConnectTypes.isNotEmpty() }
            val selectedBefore = _state.value.rows.associate { it.metric.id to it.selected }
            _state.value = GrantState(
                availability = availability,
                rows = metrics.map { metric ->
                    GrantRow(
                        metric = metric,
                        granted = granted.containsAll(readPermissions(listOf(metric))),
                        selected = selectedBefore[metric.id] ?: true,
                    )
                },
                supportsHistory = source.supportsHistory(),
                historyGranted = HistoryPermission in granted,
                supportsBackground = source.supportsBackgroundReads(),
                backgroundGranted = BackgroundPermission in granted,
            )
        }
    }

    fun toggle(metric: MetricId) {
        _state.update { state ->
            state.copy(rows = state.rows.map { if (it.metric.id == metric) it.copy(selected = !it.selected) else it })
        }
    }

    /** What the Health Connect prompt asks for. */
    fun permissionsToRequest(): Set<String> {
        val state = _state.value
        return readPermissions(state.toAsk) +
            listOfNotNull(
                HistoryPermission.takeIf { state.historyToAsk },
                BackgroundPermission.takeIf { state.backgroundToAsk },
            )
    }

    /**
     * After the prompt: remember the screen was answered, and start the periodic sync if background
     * reads were just allowed. Syncing what is now allowed (the whole window when past data just
     * became readable) is `HealthSync`'s job, on the next screen's resume, so it doesn't depend on
     * this screen staying open.
     */
    fun onPermissionsResult() {
        viewModelScope.launch {
            onboarding.markHealthConnectPromptSeen()
            backgroundSync.updateSchedule()
        }
    }

    fun notNow() {
        viewModelScope.launch { onboarding.markHealthConnectPromptSeen() }
    }
}
