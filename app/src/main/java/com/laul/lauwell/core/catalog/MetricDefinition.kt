package com.laul.lauwell.core.catalog

import androidx.annotation.DrawableRes

/**
 * Stable identifier of a metric. Ends up in Firestore paths, Room rows and stored settings, so it
 * must never change once shipped. Lowercase snake_case.
 */
@JvmInline
value class MetricId(val value: String) {
    init {
        require(value.matches(Regex("[a-z][a-z0-9_]*"))) { "MetricId must be lowercase snake_case: $value" }
    }

    override fun toString(): String = value
}

/**
 * Everything the app needs to know about one tracked data point, so that adding a tracker is an
 * entry in [MetricRegistry] and never a new screen (PLAN.md → Decisions).
 *
 * [label] matches the "Data" column of `docs/data-structure/health-data-model/Patient Data Categories.csv`.
 */
data class MetricDefinition(
    val id: MetricId,
    val label: String,
    val subCategory: SubCategory,
    val shape: StorageShape,
    /** Material Symbols glyph (`R.drawable.ic_*`) on its icon tile, chip and pinned tile. */
    @DrawableRes val icon: Int,
    /** Where its records come from: the "Data Sources" column of the Notion data model. */
    val sources: Set<DataSource>,
    /** What one record holds. One entry for a single value, several for e.g. blood pressure. */
    val quantities: List<Quantity> = emptyList(),
    /** How records are summarised per day; null when a daily summary means nothing. */
    val aggregation: Aggregation? = null,
    val chartKind: ChartKind? = null,
    /** Default personal goal (Lifestyle). Clinical ranges live on each [Quantity] instead. */
    val goal: Goal? = null,
) {
    val category: Category get() = subCategory.category

    init {
        require(shape != StorageShape.Measurement || quantities.isNotEmpty()) {
            "$id: a Measurement needs at least one quantity"
        }
        require(quantities.map { it.key }.distinct().size == quantities.size) {
            "$id: quantity keys must be unique"
        }
        require(chartKind == null || aggregation != null) { "$id: a chart needs an aggregation" }
        require(sources.isNotEmpty()) { "$id: a metric needs at least one source" }
    }

    /** Can be logged by hand. Tracker rows show "+ Add" for these and "Synced" for sensor-only ones. */
    val isManual: Boolean get() = DataSource.Manual in sources
}

/** Where a metric's records come from. */
enum class DataSource {
    /** Logged in the app. */
    Manual,

    /** Read from Health Connect (watch, phone, connected devices). */
    Wearable,
}

/**
 * One numeric value inside a record.
 *
 * @param key stable within its metric, e.g. "systolic".
 * @param unit what the user reads, e.g. "bpm", "mmHg".
 * @param precision decimals shown.
 * @param normalRange generic adult clinical range (Vitals); null when there is no meaningful one.
 *   User-configurable targets are backlog.
 */
data class Quantity(
    val key: String,
    val label: String,
    val unit: String,
    val precision: Int = 0,
    val normalRange: ClosedFloatingPointRange<Double>? = null,
) {
    init {
        require(precision >= 0) { "precision must be >= 0" }
    }
}

/** A personal target: Lifestyle data is measured against a goal you set, not a clinical range. */
data class Goal(val value: Double, val direction: Direction) {
    enum class Direction { AtLeast, AtMost }
}

enum class Aggregation {
    /** Total over the day, e.g. steps. */
    Sum,

    /** Mean of the day's records. */
    Average,

    /** Lowest, mean and highest: the shape of a range bar. */
    MinAverageMax,

    /** The day's last record, for values that barely move, e.g. height. */
    Latest,
}

/** Mapped to an actual chart by the `ChartSpec` layer (M3), the only place that knows Vico. */
enum class ChartKind { Line, Bar, RangeBar }

