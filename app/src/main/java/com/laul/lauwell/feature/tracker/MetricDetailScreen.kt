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
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.record.main
import com.laul.lauwell.core.ui.chart.Chart
import com.laul.lauwell.core.ui.chart.ChartPeriod
import com.laul.lauwell.core.ui.components.CategoryLine
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.SegmentedSelector
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock
import java.time.ZoneId
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import com.laul.lauwell.core.ui.components.PanelDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.laul.lauwell.core.ui.chart.partAlpha

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
                BasicText(state.latest?.let { valueText(metric, it) } ?: "—", style = type.metricXl.copy(color = colors.ink))
                metric.quantities.firstOrNull()?.let {
                    BasicText(it.unit, style = type.body.copy(color = colors.ink2), modifier = Modifier.padding(bottom = LauWellTheme.spacing.sm))
                }
            }
            BasicText(state.latestWhen ?: "No data yet", style = type.caption.copy(color = colors.ink2))
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

        if (state.breakdown.isNotEmpty()) Breakdown(state.breakdown, metric, state.latestWhen.orEmpty())

        state.chart?.let { chart ->
            SegmentedSelector(
                options = ChartPeriod.entries.map { it.label },
                selectedIndex = state.period.ordinal,
                onSelect = { viewModel.setPeriod(ChartPeriod.entries[it]) },
            )
            Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) { Chart(chart) }
        }
        state.stats?.takeIf { it.daysWithData > 0 }?.let { StatsRow(statItems(metric, it)) }
        if (state.days.isNotEmpty()) DayList(state.days, metric)
        if (state.access == HealthAccess.Granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
                if (fromWatch(state.origins)) LauWellIcon(R.drawable.ic_watch, contentDescription = null, tint = colors.ink2, size = 16.dp)
                BasicText(
                    sourceLine(state.origins, state.lastSyncedAt, ZoneId.systemDefault()),
                    style = type.caption.copy(color = colors.ink2),
                )
            }
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

/**
 * The latest day's parts (a night's stages; a provisional design, PLAN.md → M3): one bar split in
 * the chart's shades, so it doubles as the chart's legend, then a row per part with its share and
 * value. A part beside the total (awake) is in the neutral line colour, without a share.
 */
@Composable
private fun Breakdown(rows: List<BreakdownRow>, metric: MetricDefinition, title: String) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(metric.category).fg
    fun colorOf(row: BreakdownRow) = row.partIndex?.let { fg.copy(alpha = partAlpha(it)) } ?: colors.lineSoft
    Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) {
        Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm)) {
            BasicText(title, style = type.caption.copy(color = colors.ink2))
            Row(
                Modifier.fillMaxWidth().height(10.dp).clip(LauWellTheme.shapes.bar),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rows.filter { (it.share ?: 0f) > 0f }.forEach { row ->
                    Box(Modifier.weight(row.share!!).fillMaxHeight().background(colorOf(row)))
                }
            }
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colorOf(row)))
                    BasicText(row.label, style = type.item.copy(color = colors.ink), modifier = Modifier.weight(1f))
                    row.shareText?.let { BasicText(it, style = type.caption.copy(color = colors.ink2)) }
                    BasicText(
                        row.value,
                        style = type.metricSmall.copy(color = colors.ink, textAlign = TextAlign.End),
                        modifier = Modifier.widthIn(min = 72.dp),
                    )
                }
            }
        }
    }
}

/** The period's three stats, values in Mono. */
@Composable
private fun StatsRow(items: List<Pair<String, String>>) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Panel(contentPadding = PaddingValues(LauWellTheme.spacing.panel)) {
        Row {
            items.forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
                    BasicText(label, style = type.caption.copy(color = colors.ink2))
                    BasicText(value, style = type.metric.copy(color = colors.ink))
                }
            }
        }
    }
}

/**
 * The period's days as designed: day, note, value in Mono (category colour at goal, "—" without
 * data), hairlines between rows, no panel.
 */
@Composable
private fun DayList(days: List<DayRow>, metric: MetricDefinition) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val categoryColor = colors.forCategory(metric.category).fg
    Column {
        days.forEach { day ->
            PanelDivider()
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
            ) {
                BasicText(day.label, style = type.item.copy(color = colors.ink), modifier = Modifier.weight(1f))
                day.note?.let { BasicText(it, style = type.caption.copy(color = colors.ink2)) }
                BasicText(
                    day.value ?: "—",
                    style = type.metricSmall.copy(
                        color = when {
                            day.value == null -> colors.ink2
                            day.atGoal -> categoryColor
                            else -> colors.ink
                        },
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.widthIn(min = 54.dp),
                )
            }
        }
        PanelDivider()
    }
}
