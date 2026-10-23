package com.laul.lauwell.core.catalog

import com.laul.lauwell.core.catalog.metrics.ActivityMetrics
import com.laul.lauwell.core.catalog.metrics.CardiovascularMetrics
import com.laul.lauwell.core.catalog.metrics.SleepMetrics

/**
 * Every metric the app knows about. Adding a tracker means adding its [MetricDefinition] to one
 * of the lists below — no new screen, no new navigation destination.
 *
 * Metrics land as their features are built; the full list of data points is in
 * `docs/data-structure/health-data-model/Patient Data Categories.csv`.
 */
object MetricRegistry {

    val all: List<MetricDefinition> = CardiovascularMetrics.all + ActivityMetrics.all + SleepMetrics.all

    private val byId: Map<MetricId, MetricDefinition> = all.associateBy { it.id }

    init {
        require(byId.size == all.size) { "Duplicate MetricId in the registry" }
    }

    operator fun get(id: MetricId): MetricDefinition? = byId[id]

    /** The metrics shown to the user, given what they switched off. */
    fun visible(switchedOff: Set<ToggleKey>): List<MetricDefinition> =
        all.filter { isVisible(it, switchedOff) }
}
