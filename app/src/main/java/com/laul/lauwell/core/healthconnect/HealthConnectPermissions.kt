package com.laul.lauwell.core.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import com.laul.lauwell.core.catalog.MetricDefinition

/**
 * The Health Connect read permissions [metrics] need, from each one's `healthConnectTypes`.
 *
 * Pass the visible metrics only (`MetricRegistry.visible`): a switched-off metric never asks for
 * anything. `AndroidManifest.xml` declares the permissions of every metric in the registry, visible
 * or not; `HealthConnectPermissionsTest` keeps the two in step.
 */
/**
 * Reading data older than 30 days before the first grant: Health Connect returns nothing earlier
 * without it. Asked with the metrics' permissions when [HealthDataSource.supportsHistory].
 */
const val HistoryPermission: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

fun readPermissions(metrics: Iterable<MetricDefinition>): Set<String> =
    metrics.flatMap { it.healthConnectTypes }
        .mapTo(mutableSetOf()) { HealthPermission.getReadPermission(it) }
