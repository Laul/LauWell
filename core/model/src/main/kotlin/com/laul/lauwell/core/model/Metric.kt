package com.laul.lauwell.core.model

/** How raw values of a metric combine over a day. */
enum class Aggregation {
    /** Point readings (heart rate, glucose): a day is summarised as min, max and average. */
    RANGE,

    /** Counts over intervals (steps): a day is summarised as a total. */
    SUM,
}

/** Inclusive target band. Either edge may be open. */
data class TargetRange(val low: Double?, val high: Double?) {
    init {
        require(low == null || high == null || low <= high) { "low ($low) must be <= high ($high)" }
    }

    operator fun contains(value: Double): Boolean =
        (low == null || value >= low) && (high == null || value <= high)
}

/**
 * Every metric LauWell knows about, typed. Code dispatches on this, never on a display name.
 *
 * Default targets come from TrackAid and are placeholders: targets are personal and should be
 * user-configurable (stored in settings, not here).
 */
enum class Metric(
    val displayName: String,
    val unit: String,
    val decimals: Int,
    val aggregation: Aggregation,
    val defaultTarget: TargetRange?,
) {
    HEART_RATE("Heart rate", "bpm", 0, Aggregation.RANGE, TargetRange(70.0, 120.0)),
    BLOOD_GLUCOSE("Glucose", "mmol/L", 1, Aggregation.RANGE, TargetRange(4.0, 9.0)),
    STEPS("Steps", "steps", 0, Aggregation.SUM, TargetRange(1700.0, null)),

    // Blood pressure is two metrics sharing one source record id.
    BLOOD_PRESSURE_SYSTOLIC("Systolic", "mmHg", 0, Aggregation.RANGE, null),
    BLOOD_PRESSURE_DIASTOLIC("Diastolic", "mmHg", 0, Aggregation.RANGE, null),
}
