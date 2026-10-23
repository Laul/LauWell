package com.laul.lauwell.feature.tracker

import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DeviceType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// The detail screen's source line (design: "Health Connect · Pixel Watch · synced 09:12"): where the
// shown day's records come from, named by device when Health Connect knows it, else by app.

/** "Health Connect · Garmin Forerunner 265 · synced 21:14"; "not synced yet" before the first sync. */
internal fun sourceLine(origins: Set<DataOrigin>, syncedAt: Instant?, zone: ZoneId): String = buildList {
    add("Health Connect")
    addAll(sourceNames(origins))
    add(syncedAt?.let { "synced ${TimeFormat.format(it.atZone(zone))}" } ?: "not synced yet")
}.joinToString(" · ")

/**
 * One name per source, sorted: the device ([deviceName]) where known, else the app ([appName]). An
 * app that wrote some records with their device and some without is named by its devices only.
 */
internal fun sourceNames(origins: Set<DataOrigin>): List<String> {
    val appsWithDevice = origins.filter { deviceName(it) != null }.map { it.appPackage }.toSet()
    return origins
        .mapNotNull { origin -> deviceName(origin) ?: appName(origin.appPackage).takeIf { origin.appPackage !in appsWithDevice } }
        .distinct()
        .sorted()
}

/** Whether the source line starts with the watch icon: a watch among the sources. */
internal fun fromWatch(origins: Set<DataOrigin>): Boolean = origins.any { it.device?.type == DeviceType.Watch }

/**
 * "Garmin Forerunner 265" from manufacturer and model; the model alone when it already starts with
 * the manufacturer, or when there is none. Null without a model: a manufacturer alone names no device.
 */
internal fun deviceName(origin: DataOrigin): String? {
    val model = origin.device?.model?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // Some devices report a lowercase manufacturer ("samsung").
    val maker = origin.device.manufacturer?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() }
    return if (maker == null || model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
}

/** The writing apps LauWell knows by name; any other shows its package. */
internal fun appName(appPackage: String): String = when (appPackage) {
    "com.garmin.android.apps.connectmobile" -> "Garmin Connect"
    "com.sec.android.app.shealth" -> "Samsung Health"
    "com.google.android.apps.fitness" -> "Google Fit"
    "com.fitbit.FitbitMobile" -> "Fitbit"
    "com.google.android.apps.healthdata" -> "Health Connect"
    else -> appPackage
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
