package com.laul.lauwell.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laul.lauwell.AppContainer
import com.laul.lauwell.core.charts.ChartSpec
import com.laul.lauwell.core.charts.ChartSpecs
import com.laul.lauwell.core.data.repository.SyncFailure
import com.laul.lauwell.core.domain.DateSpan
import com.laul.lauwell.core.domain.latest
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MetricCard(
    val metric: Metric,
    val latest: Measurement?,
    val chart: ChartSpec,
    /** False until the user grants this metric's permission. Other cards still work. */
    val canRead: Boolean,
)

data class OverviewUiState(
    val cards: List<MetricCard> = emptyList(),
    val syncing: Boolean = false,
    val failures: List<SyncFailure> = emptyList(),
    val healthConnectAvailable: Boolean = true,
)

private data class SyncStatus(val running: Boolean = false, val failures: List<SyncFailure> = emptyList())

/**
 * Overview state as one immutable [StateFlow]. The screen only renders it; Room pushes every
 * change through, so charts update on first load without a manual refresh.
 */
class OverviewViewModel(private val container: AppContainer) : ViewModel() {
    private val zone = container.zone
    private val span = DateSpan.lastDays(7, container.clock.instant().atZone(zone).toLocalDate())
    private val granted = MutableStateFlow<Set<Metric>>(emptySet())
    private val sync = MutableStateFlow(SyncStatus())

    private val perMetric = combine(
        METRICS.map { metric ->
            container.repository.observe(metric, span.startIn(zone), span.endIn(zone)).map { metric to it }
        },
    ) { it.toList() }

    val state: StateFlow<OverviewUiState> =
        combine(perMetric, granted, sync) { values, canRead, status ->
            OverviewUiState(
                cards = values.map { (metric, measurements) ->
                    MetricCard(
                        metric = metric,
                        latest = measurements.latest(),
                        chart = ChartSpecs.daily(metric, measurements, span, zone),
                        canRead = metric in canRead,
                    )
                },
                syncing = status.running,
                failures = status.failures,
                healthConnectAvailable = container.healthConnect != null,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                OverviewUiState(healthConnectAvailable = container.healthConnect != null),
            )

    init {
        refresh()
    }

    /** Re-reads granted permissions, then syncs the metrics the user allowed. */
    fun refresh() {
        viewModelScope.launch {
            val healthConnect = container.healthConnect ?: return@launch
            val canRead = healthConnect.grantedMetrics()
            granted.value = canRead
            if (canRead.isEmpty()) return@launch
            sync.value = SyncStatus(running = true)
            val failures = container.repository.sync(canRead, since = span.startIn(zone))
            sync.value = SyncStatus(running = false, failures = failures)
        }
    }

    companion object {
        val METRICS = listOf(Metric.BLOOD_GLUCOSE, Metric.HEART_RATE, Metric.STEPS)

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { OverviewViewModel(container) }
        }
    }
}
