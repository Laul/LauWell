package com.laul.lauwell.core.navigation

import androidx.annotation.DrawableRes
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricId

/**
 * The four bottom-nav tabs (PLAN.md → Decisions → App navigation). Each tab is a nested graph
 * ([graphRoute]) whose first screen is [route], so screens pushed inside a tab (a metric detail)
 * keep the bottom bar with that tab selected, and each tab keeps its own back stack.
 *
 * A new tracker is a registry entry, never a new entry here.
 */
enum class TopLevelTab(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    Today("today", "Today", R.drawable.ic_sunny, R.drawable.ic_sunny_filled),
    Tracker("tracker", "Tracker", R.drawable.ic_target, R.drawable.ic_target_active),
    Care("care", "Care", R.drawable.ic_medical_services, R.drawable.ic_medical_services_filled),
    Records("records", "Records", R.drawable.ic_folder, R.drawable.ic_folder_active),
    ;

    val graphRoute: String get() = "${route}_tab"
}

/** Screens outside the tab roots. */
object Routes {
    /** Opened from the avatar on Today; shown without the bottom bar. */
    const val SETTINGS = "settings"

    /** The Health Connect grant screen: once after sign-in, then from Settings; no bottom bar. */
    const val HEALTH_CONNECT = "health-connect"

    const val METRIC_ID_ARG = "metricId"
    const val METRIC_DETAIL = "metric/{$METRIC_ID_ARG}"

    fun metricDetail(id: MetricId): String = "metric/${id.value}"
}
