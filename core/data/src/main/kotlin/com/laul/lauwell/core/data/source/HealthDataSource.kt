package com.laul.lauwell.core.data.source

import com.laul.lauwell.core.model.Measurement
import com.laul.lauwell.core.model.Metric
import java.time.Instant

/**
 * An outside system LauWell syncs from: Health Connect today, FHIR and manual import later.
 * Each source maps its own records into [Measurement] and nothing else leaks out.
 */
interface HealthDataSource {
    /** Stable name stored with each measurement, e.g. [com.laul.lauwell.core.model.Source.HEALTH_CONNECT]. */
    val system: String

    fun supports(metric: Metric): Boolean

    /**
     * Every value of [metric] in [from, to), fully paged. Each returned measurement must carry a
     * stable record id so a re-sync replaces it instead of duplicating it.
     */
    suspend fun read(metric: Metric, from: Instant, to: Instant): List<Measurement>
}
