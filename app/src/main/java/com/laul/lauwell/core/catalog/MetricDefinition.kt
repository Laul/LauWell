package com.laul.lauwell.core.catalog

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
 * [label] matches the "Data" column of `docs/data-structure/Patient Data Categories.csv`.
 */
data class MetricDefinition(
    val id: MetricId,
    val label: String,
    val subCategory: SubCategory,
    val shape: StorageShape,
    /** What one record holds. One entry for a single value, several for e.g. blood pressure. */
    val quantities: List<Quantity> = emptyList(),
    /** How records are summarised per day; null when a daily summary means nothing. */
    val aggregation: Aggregation? = null,
    val chartKind: ChartKind? = null,
    /** Default personal goal (Lifestyle). Clinical ranges live on each [Quantity] instead. */
    val goal: Goal? = null,
    /** Standard codes for the metric as a whole; per-quantity codes live on [Quantity.coding]. */
    val codings: List<Coding> = emptyList(),
) {
    val category: Category get() = subCategory.category

    init {
        require(shape != StorageShape.Measurement || quantities.isNotEmpty()) {
            "$id: a Measurement needs at least one quantity"
        }
        require(quantities.map { it.key }.distinct().size == quantities.size) {
            "$id: quantity keys must be unique"
        }
        require(codings.count { it.role == Coding.Role.Primary } <= 1) {
            "$id: at most one primary coding"
        }
        require(chartKind == null || aggregation != null) { "$id: a chart needs an aggregation" }
    }
}

/**
 * One numeric value inside a record.
 *
 * @param key stable within its metric, e.g. "systolic".
 * @param unit UCUM code, e.g. "/min", "mm[Hg]" — what is stored.
 * @param displayUnit what the user reads, e.g. "bpm", "mmHg".
 * @param precision decimals shown.
 * @param normalRange generic adult clinical range (Vitals); null when there is no meaningful one.
 *   User-configurable targets are backlog.
 */
data class Quantity(
    val key: String,
    val label: String,
    val unit: String,
    val displayUnit: String,
    val precision: Int = 0,
    val normalRange: ClosedFloatingPointRange<Double>? = null,
    val coding: Coding? = null,
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

/**
 * A standard terminology code (FHIR `Coding` shape). Codes are a layer on top: app logic keys on
 * [MetricId], never on a code (PLAN.md → Decisions → Terminology codes). Source of truth:
 * `docs/data-structure/Patient Data Codings.csv`.
 *
 * @param verified true once code and display were confirmed in the LOINC search.
 */
data class Coding(
    val system: System,
    val code: String,
    val display: String,
    val role: Role,
    val verified: Boolean,
) {
    enum class System(val label: String, val uri: String) {
        Loinc("LOINC", "http://loinc.org"),
        SnomedCt("SNOMED CT", "http://snomed.info/sct"),
    }

    enum class Role(val label: String) {
        Primary("primary"),
        Component("component"),
        Variant("variant"),
    }
}
