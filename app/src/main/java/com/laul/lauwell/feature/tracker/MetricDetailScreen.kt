package com.laul.lauwell.feature.tracker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.DayValue
import com.laul.lauwell.core.record.PeriodStats
import com.laul.lauwell.core.record.formatValue
import com.laul.lauwell.core.record.main
import com.laul.lauwell.core.ui.chart.Chart
import com.laul.lauwell.core.ui.components.CategoryLine
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * One metric's detail, shared by every metric (`metric/{id}`): today's value, the last 7 days as
 * a chart (range-bar metrics so far) and their stats, and the source line. A synced metric asks
 * for its Health Connect permission here until the grant screen exists (PLAN.md → M3).
 */
@Composable
fun MetricDetailScreen(metricId: String, onBack: () -> Unit) {
    val metric = runCatching { MetricId(metricId) }.getOrNull()?.let { MetricRegistry[it] }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = metric?.label ?: "Not found",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs),
        )
        if (metric == null) {
            ScreenColumn {
                BasicText(
                    "This metric doesn't exist (any more).",
                    style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
                )
            }
        } else {
            MetricDetail(metric)
        }
    }
}

@Composable
private fun MetricDetail(metric: MetricDefinition) {
    val viewModel = appViewModel {
        MetricDetailViewModel(metric, healthDataSource, healthSync, summaryStore, Clock.systemDefaultZone())
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Back from Health Connect's settings, a permission may have changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    val requestPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshAccess() }

    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    ScreenColumn {
        Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
            CategoryLine(metric.category, "${metric.category.label} · ${metric.subCategory.label}")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
                BasicText(state.today?.let { headline(it, metric) } ?: "—", style = type.metricXl.copy(color = colors.ink))
                metric.quantities.firstOrNull()?.let {
                    BasicText(it.unit, style = type.body.copy(color = colors.ink2), modifier = Modifier.padding(bottom = LauWellTheme.spacing.sm))
                }
            }
            BasicText(if (state.today != null) "Today" else "No data today", style = type.caption.copy(color = colors.ink2))
        }

        when (val access = state.access) {
            is HealthAccess.NeedsPermission -> AccessPanel(
                "LauWell needs your permission to read ${metric.label.lowercase()} from Health Connect.",
            ) {
                FilledButton("Allow access", onClick = { requestPermissions.launch(access.permissions) })
            }
            is HealthAccess.Unavailable -> AccessPanel(
                if (access.updateRequired) "Health Connect needs an update before LauWell can read your data."
                else "Health Connect isn't available on this phone.",
            )
            else -> Unit
        }

        state.chart?.let { chart ->
            Section("Last 7 days") {
                Panel(contentPadding = PaddingValues(LauWellTheme.spacing.sm)) { Chart(chart) }
            }
        }
        state.stats?.takeIf { it.daysWithData > 0 }?.let { StatsRow(it, metric) }
        if (state.access == HealthAccess.Granted) {
            BasicText(
                sourceLine(state.today?.origins.orEmpty(), state.lastSyncedAt),
                style = type.caption.copy(color = colors.ink2),
            )
        }
    }
}

@Composable
private fun AccessPanel(text: String, action: (@Composable () -> Unit)? = null) {
    Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) {
        Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm)) {
            BasicText(text, style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink))
            action?.invoke()
        }
    }
}

/** Lowest · average · highest of the period, values in Mono. */
@Composable
private fun StatsRow(stats: PeriodStats, metric: MetricDefinition) {
    val precision = metric.quantities.first().precision
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) {
        Row {
            listOf(
                "Lowest" to stats.lowest?.value,
                "Average" to stats.average,
                "Highest" to stats.highest?.value,
            ).forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
                    BasicText(label, style = type.caption.copy(color = colors.ink2))
                    BasicText(value?.let { formatValue(it, precision) } ?: "—", style = type.metric.copy(color = colors.ink))
                }
            }
        }
    }
}

/** Today's headline: a range as "52–148", otherwise the day's value. */
private fun headline(day: DailySummary, metric: MetricDefinition): String? {
    val quantity = metric.quantities.firstOrNull() ?: return null
    val value = day.values[quantity.key] ?: return null
    return if (value is DayValue.Range) {
        "${formatValue(value.min, quantity.precision)}–${formatValue(value.max, quantity.precision)}"
    } else {
        formatValue(value.main, quantity.precision)
    }
}

/** "Health Connect · Garmin Connect · synced 21:14". */
private fun sourceLine(origins: Set<DataOrigin>, syncedAt: Instant?): String = buildList {
    add("Health Connect")
    origins.map { appName(it.appPackage) }.sorted().forEach(::add)
    add(syncedAt?.let { "synced ${TimeFormat.format(it.atZone(ZoneId.systemDefault()))}" } ?: "not synced yet")
}.joinToString(" · ")

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** The writing apps LauWell knows by name; any other shows its package. */
internal fun appName(appPackage: String): String = when (appPackage) {
    "com.garmin.android.apps.connectmobile" -> "Garmin Connect"
    "com.sec.android.app.shealth" -> "Samsung Health"
    "com.google.android.apps.fitness" -> "Google Fit"
    "com.google.android.apps.healthdata" -> "Health Connect"
    else -> appPackage
}
