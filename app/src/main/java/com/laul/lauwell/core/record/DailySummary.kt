package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.MetricId
import java.time.Instant
import java.time.LocalDate

/**
 * One metric on one local day, as charts, Tracker rows and pinned tiles read it. Built in M3,
 * from Health Connect's aggregation for synced data (it applies the user's app priority, so steps
 * written by several apps are not counted twice; PLAN.md → Decisions → *Read first*).
 *
 * A day with no records has no summary: missing data is absent, never 0.
 *
 * @param values one entry per `Quantity.key`, all of the kind the metric's [Aggregation] gives.
 * @param origins the apps the day's records came from, for the source line.
 */
data class DailySummary(
    val metric: MetricId,
    val date: LocalDate,
    val values: Map<String, DayValue>,
    val origins: Set<DataOrigin>,
) {
    init {
        require(values.isNotEmpty()) { "$metric $date: a summary needs a value; leave the day out instead" }
        require(values.values.map { it.aggregation }.distinct().size == 1) {
            "$metric $date: every quantity of a day is summarised the same way"
        }
    }

    val aggregation: Aggregation get() = values.values.first().aggregation
}

/** A day's value, one subtype per [Aggregation]. */
sealed interface DayValue {
    val aggregation: Aggregation

    /** Total over the day, e.g. steps. */
    data class Total(val value: Double) : DayValue {
        override val aggregation get() = Aggregation.Sum
    }

    /**
     * Mean of the day's records. [count] is null when the source doesn't say: Health Connect's
     * aggregation gives none for blood pressure or resting heart rate.
     */
    data class Mean(val value: Double, val count: Int? = null) : DayValue {
        init {
            require(count == null || count > 0) { "a mean needs at least one record" }
        }

        override val aggregation get() = Aggregation.Average
    }

    /** Lowest, mean and highest: a range bar. */
    data class Range(val min: Double, val average: Double, val max: Double) : DayValue {
        init {
            require(min <= average && average <= max) { "expected min <= average <= max: $min, $average, $max" }
        }

        override val aggregation get() = Aggregation.MinAverageMax
    }

    /** The day's last record. */
    data class Last(val value: Double, val at: Instant) : DayValue {
        override val aggregation get() = Aggregation.Latest
    }
}
