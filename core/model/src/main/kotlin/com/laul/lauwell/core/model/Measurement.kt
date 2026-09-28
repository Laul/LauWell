package com.laul.lauwell.core.model

import java.time.Instant

/** Where a measurement came from, so re-syncs update it instead of duplicating it. */
data class Source(
    /** Stable system name, e.g. [HEALTH_CONNECT], [FHIR] or [MANUAL]. */
    val system: String,
    /** The record's id in that system, if it has one. */
    val recordId: String?,
) {
    companion object {
        const val HEALTH_CONNECT = "health_connect"
        const val FHIR = "fhir"
        const val MANUAL = "manual"
    }
}

/**
 * One value of one metric. The single shape every source maps into and every screen reads from.
 *
 * A point reading has [start] == [end]; an interval (steps in an hour) has [end] after [start].
 * Missing data is the absence of a [Measurement], never a zero value.
 */
data class Measurement(
    val metric: Metric,
    val start: Instant,
    val end: Instant = start,
    val value: Double,
    val source: Source,
) {
    init {
        require(!end.isBefore(start)) { "end ($end) is before start ($start)" }
        require(value.isFinite()) { "value must be finite, was $value" }
    }
}
