package com.laul.lauwell.core.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.Measurement
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import kotlin.reflect.KClass

/**
 * [HealthDataSource] backed by the Health Connect client. The mapping to our records lives in
 * `HealthConnectMapping.kt`; this class only makes the calls.
 *
 * Never logs: records and their values stay inside the returned objects.
 */
class HealthConnectDataSource(private val context: Context) : HealthDataSource {

    /** Created on first use: [HealthConnectClient.getOrCreate] throws when Health Connect is unavailable. */
    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
        else -> HealthConnectAvailability.Unavailable
    }

    override suspend fun grantedPermissions(): Set<String> =
        client.permissionController.getGrantedPermissions()

    override suspend fun readMeasurements(metric: MetricDefinition, from: Instant, until: Instant): List<Measurement> {
        val range = TimeRangeFilter.between(from, until)
        return metric.healthConnectTypes
            .flatMap { type -> readAll(type, range) }
            .flatMap { toMeasurements(metric.id, it) }
            .sortedBy { it.time }
    }

    override suspend fun dailySummaries(metric: MetricDefinition, days: ClosedRange<LocalDate>): List<DailySummary> {
        val metrics = aggregateMetricsFor(metric)
        if (metrics.isEmpty()) return emptyList()
        // Local times: each record is placed on its own local day, wherever it was recorded.
        val range = TimeRangeFilter.between(days.start.atStartOfDay(), days.endInclusive.plusDays(1).atStartOfDay())
        return client.aggregateGroupByPeriod(AggregateGroupByPeriodRequest(metrics, range, Period.ofDays(1)))
            .mapNotNull { dailySummary(metric, it) }
    }

    private suspend fun <T : Record> readAll(type: KClass<T>, range: TimeRangeFilter): List<T> =
        readAllPages { token ->
            val response = client.readRecords(ReadRecordsRequest(type, range, pageToken = token))
            response.records to response.pageToken
        }
}
