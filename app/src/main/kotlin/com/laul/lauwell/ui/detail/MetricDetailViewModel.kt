package com.laul.lauwell.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laul.lauwell.AppContainer
import com.laul.lauwell.core.charts.ChartSpec
import com.laul.lauwell.core.charts.ChartSpecs
import com.laul.lauwell.core.domain.DateSpan
import com.laul.lauwell.core.domain.PeriodStats
import com.laul.lauwell.core.domain.dailyRanges
import com.laul.lauwell.core.domain.dailyTotals
import com.laul.lauwell.core.domain.latest
import com.laul.lauwell.core.domain.rangeStats
import com.laul.lauwell.core.domain.timeline
import com.laul.lauwell.core.domain.totalStats
import com.laul.lauwell.core.model.Aggregation
import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MetricDetailUiState(
    val metric: Metric,
    val latest: Measurement? = null,
    val stats: PeriodStats? = null,
    val daily: ChartSpec? = null,
    /** Every reading, for point metrics. Null for counts such as steps. */
    val timeline: ChartSpec? = null,
)

class MetricDetailViewModel(metric: Metric, container: AppContainer) : ViewModel() {
    private val zone = container.zone
    private val span = DateSpan.lastDays(7, container.clock.instant().atZone(zone).toLocalDate())

    val state: StateFlow<MetricDetailUiState> =
        container.repository.observe(metric, span.startIn(zone), span.endIn(zone))
            .map { values ->
                MetricDetailUiState(
                    metric = metric,
                    latest = values.latest(),
                    stats = when (metric.aggregation) {
                        Aggregation.RANGE -> dailyRanges(values, span, zone).rangeStats()
                        Aggregation.SUM -> dailyTotals(values, span, zone).totalStats()
                    },
                    daily = ChartSpecs.daily(metric, values, span, zone),
                    timeline = if (metric.aggregation == Aggregation.RANGE) {
                        ChartSpecs.timeline(metric, values.timeline(), origin = span.startIn(zone), zone = zone)
                    } else {
                        null
                    },
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricDetailUiState(metric))

    companion object {
        fun factory(metric: Metric, container: AppContainer) = viewModelFactory {
            initializer { MetricDetailViewModel(metric, container) }
        }
    }
}
