package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.Measurement
import java.time.Instant
import java.time.LocalDate

/**
 * Sensor data as the app reads it: the only way the sync and the screens reach Health Connect.
 *
 * Implementation: [HealthConnectDataSource], built once in [com.laul.lauwell.core.di.AppContainer].
 * A metric without `healthConnectTypes` (e.g. the ECG trace) reads as empty.
 *
 * The read calls throw a `SecurityException` when the metric's permission isn't granted: check
 * [grantedPermissions] against `readPermissions(...)` first.
 */
interface HealthDataSource {

    /** Whether Health Connect can be used on this phone right now. */
    fun availability(): HealthConnectAvailability

    /** The Health Connect permissions the user granted to this app. */
    suspend fun grantedPermissions(): Set<String>

    /**
     * Every record of [metric] from [from] (inclusive) to [until] (exclusive), oldest first, all
     * pages read. A heart-rate series becomes one [Measurement] per sample.
     */
    suspend fun readMeasurements(metric: MetricDefinition, from: Instant, until: Instant): List<Measurement>

    /**
     * One summary per local day in [days] that has data, from Health Connect's aggregation: it
     * applies the user's app priority, so a value written by several apps counts once (PLAN.md →
     * Decisions → *Read first*). Days without data are left out.
     */
    suspend fun dailySummaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): List<DailySummary>

    /** A token marking "now" in [metric]'s change log; [changes] later returns what happened since. */
    suspend fun changesToken(metric: MetricDefinition): String

    /** What changed in [metric]'s records since [token], all pages read. */
    suspend fun changes(metric: MetricDefinition, token: String): HealthChanges
}

/** The result of [HealthDataSource.changes]. */
sealed interface HealthChanges {

    /**
     * @param days the local days of records added or edited since the token.
     * @param hadDeletions whether records were deleted: Health Connect only gives their id, not their day.
     * @param nextToken where the next call starts.
     */
    data class Changed(val days: Set<LocalDate>, val hadDeletions: Boolean, val nextToken: String) : HealthChanges

    /** Tokens expire after 30 days unused: read everything again and take a new token. */
    data object TokenExpired : HealthChanges
}

/** Health Connect's SDK status, as the onboarding needs it. */
enum class HealthConnectAvailability {
    Available,

    /** Installed but too old, or not installed where it's an app (Android 13 and below). */
    UpdateRequired,

    /** Not supported on this device. */
    Unavailable,
}
