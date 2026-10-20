package com.laul.lauwell.core.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Something the user can switch off: a whole category, a sub-category, or a single metric
 * (`docs/architecture/feature-modules-and-toggles.md`). The app stores the switched-off set, so an
 * empty set means everything is visible and metrics added later appear on their own.
 */
sealed interface ToggleKey {
    /** Stable string form, for when the set is persisted (DataStore, with the Settings toggles). */
    val storageKey: String

    data class OfCategory(val category: Category) : ToggleKey {
        override val storageKey: String get() = "category:${category.key}"
    }

    data class OfSubCategory(val subCategory: SubCategory) : ToggleKey {
        override val storageKey: String get() = "subcategory:${subCategory.storageKey}"
    }

    data class OfMetric(val metricId: MetricId) : ToggleKey {
        override val storageKey: String get() = "metric:${metricId.value}"
    }
}

/**
 * The only place that decides whether a metric is shown. Home, category screens, the permission
 * flow and background sync all go through it (or [MetricRegistry.visible]); none of them reads a
 * toggle directly.
 */
fun isVisible(metric: MetricDefinition, switchedOff: Set<ToggleKey>): Boolean =
    ToggleKey.OfMetric(metric.id) !in switchedOff &&
        ToggleKey.OfSubCategory(metric.subCategory) !in switchedOff &&
        ToggleKey.OfCategory(metric.category) !in switchedOff

/** Source of the switched-off set. */
interface ToggleRepository {
    val switchedOff: Flow<Set<ToggleKey>>
}

/**
 * MVP: nothing can be switched off yet, so everything is visible. Replaced by a DataStore-backed
 * implementation when the Settings toggles land (PLAN.md → backlog).
 */
object EverythingOnToggleRepository : ToggleRepository {
    override val switchedOff: Flow<Set<ToggleKey>> = flowOf(emptySet())
}
