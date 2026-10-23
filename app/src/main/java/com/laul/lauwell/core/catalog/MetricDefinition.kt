package com.laul.lauwell.core.catalog

import androidx.annotation.DrawableRes
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

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
 * [label] is what the user reads, in sentence case; it matches the "Data" column of
 * `docs/data-structure/health-data-model/Patient Data Categories.csv` ignoring case (the Notion names mix cases).
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
    /** The tabs that show it: the "Nav. Screens" column. Today is not a tag, it reads from all of them. */
    val screens: Set<NavScreen>,
    /** What one record holds. One entry for a single value, several for e.g. blood pressure. */
    val quantities: List<Quantity> = emptyList(),
    /**
     * Parts of the first quantity's value, kept in the same daily summary: a night's sleep stages.
     * The detail screen shows them for the latest day, and a [ChartKind.StackedBar] stacks those
     * [Quantity.inTotal]. Not shown on Tracker rows or tiles.
     */
    val breakdown: List<Quantity> = emptyList(),
    /** How records are summarised per day; null when a daily summary means nothing. */
    val aggregation: Aggregation? = null,
    val chartKind: ChartKind? = null,
    /** The 7-day mini-chart on its Tracker row and widget (`docs/design/UI-spec.md`); null for none. */
    val miniChart: MiniChartKind? = null,
    /** Default personal goal (Lifestyle). Clinical ranges live on each [Quantity] instead. */
    val goal: Goal? = null,
    /**
     * The Health Connect record types it is read from. Their read permissions are what the metric
     * asks for, and only while it is visible (`core.healthconnect.readPermissions`).
     */
    val healthConnectTypes: Set<KClass<out Record>> = emptySet(),
    /**
     * A night's value (sleep): it counts on the day it ends, and that day runs from 18:00 the
     * evening before to 18:00, so a night is never cut at midnight.
     */
    val nightly: Boolean = false,
) {
    val category: Category get() = subCategory.category

    init {
        require(shape != StorageShape.Measurement || quantities.isNotEmpty()) {
            "$id: a Measurement needs at least one quantity"
        }
        require((quantities + breakdown).map { it.key }.distinct().size == quantities.size + breakdown.size) {
            "$id: quantity keys must be unique"
        }
        require(breakdown.isEmpty() || quantities.size == 1) { "$id: a breakdown splits a single quantity" }
        require(chartKind != ChartKind.StackedBar || breakdown.any { it.inTotal }) { "$id: a stacked bar needs parts" }
        require(chartKind == null || aggregation != null) { "$id: a chart needs an aggregation" }
        require(sources.isNotEmpty()) { "$id: a metric needs at least one source" }
        require(screens.isNotEmpty()) { "$id: a metric needs at least one screen" }
        require(healthConnectTypes.isEmpty() || DataSource.Wearable in sources) {
            "$id: only a wearable metric is read from Health Connect"
        }
    }

    /** Can be logged by hand: offered in Quick log and on its detail screen. */
    val isManual: Boolean get() = DataSource.Manual in sources

    /**
     * Has a latest value to show on a pinned tile (value and unit; goal progress when it has a
     * [goal]). A file or a fact without a number can't be pinned.
     */
    val canPin: Boolean get() = quantities.isNotEmpty() && aggregation != null
}

/** Where a metric's records come from. */
enum class DataSource {
    /** Logged in the app. */
    Manual,

    /** Read from Health Connect (watch, phone, connected devices). */
    Wearable,

    /** Derived from other records, e.g. wear time from consecutive appliance changes. */
    Computed,
}

/** A bottom tab a metric is shown on (PLAN.md → Decisions → App navigation). Stored once, shown on each. */
enum class NavScreen { Tracker, Records, Care }

/**
 * One numeric value inside a record.
 *
 * @param key stable within its metric, e.g. "systolic".
 * @param unit what the user reads, e.g. "bpm", "mmHg".
 * @param precision decimals shown.
 * @param format how the value reads: a number, or minutes shown as "7h 12m" (then [unit] is empty).
 * @param inTotal for a breakdown part: whether it adds up to the whole (deep sleep), or sits beside
 *   it (time awake during the night).
 * @param normalRange generic adult clinical range (Vitals); null when there is no meaningful one.
 *   User-configurable targets are backlog.
 */
data class Quantity(
    val key: String,
    val label: String,
    val unit: String,
    val precision: Int = 0,
    val normalRange: ClosedFloatingPointRange<Double>? = null,
    val format: ValueFormat = ValueFormat.Number,
    val inTotal: Boolean = true,
) {
    init {
        require(precision >= 0) { "precision must be >= 0" }
        require(format != ValueFormat.Duration || unit.isEmpty()) { "$key: a duration reads \"7h 12m\", without a unit" }
    }
}

enum class ValueFormat {
    /** "8,260", followed by the unit. */
    Number,

    /** A number of minutes, read "7h 12m". */
    Duration,
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
enum class ChartKind {
    Line,
    Bar,
    RangeBar,

    /** A bar split into the metric's [MetricDefinition.breakdown] parts: a night's sleep stages. */
    StackedBar,
}

/**
 * The small chart on a Tracker row (7 days, 56 × 24dp) and widget (34dp high), drawn by
 * `core.ui.components.MiniChart`, not Vico. Rules: `docs/design/UI-spec.md` → Mini-chart rules.
 */
enum class MiniChartKind {
    /** One bar per day, dashed goal line when there is a goal: steps, sleep, ostomy output. */
    Bars,

    /** Line over a soft area, dot on the latest value: heart rate, mood, fatigue. */
    Line,

    /** One low–high bar per day: blood pressure (diastolic to systolic). */
    Range,

    /** One dot per day, higher = more severe, hollow = none: brain fog. */
    SeverityDots,

    /** The 28-day cycle with period and fertile window tinted, today tall in ink. */
    CycleStrip,
}

