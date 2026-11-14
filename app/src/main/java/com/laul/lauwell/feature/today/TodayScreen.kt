package com.laul.lauwell.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.ui.LocalWideWindow
import com.laul.lauwell.core.ui.components.MetricWidget
import com.laul.lauwell.core.ui.components.WidgetFrame
import com.laul.lauwell.core.ui.components.WidgetGrid
import com.laul.lauwell.core.ui.components.WidgetSpan
import com.laul.lauwell.core.ui.components.WidgetTitle
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.summary.MetricSummary
import com.laul.lauwell.core.ui.components.AppHeader
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PinnedTile
import com.laul.lauwell.core.ui.components.QuickLogChip
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.theme.LauWellTheme
import com.laul.lauwell.feature.log.LogEntrySheet
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Today, the home tab: the header (date, greeting, search and the gear that opens Settings), the
 * quick-log chips (each opens its entry sheet) and the pinned metrics, each opening its detail
 * ([onOpenMetric]). On a phone: chips, then pinned tiles. On a wide window (the Fold open): the
 * dashboard of design v03 18a, the same metrics as widgets. Medications, the symptom check and
 * upcoming appointments come later.
 */
@Composable
fun TodayScreen(onOpenMetric: (MetricId) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel = appViewModel {
        TodayViewModel(toggleRepository, pinnedMetrics, dailySummaries, healthSync, Clock.systemDefaultZone())
    }
    val pinned by viewModel.pinned.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val quickLog by viewModel.quickLog.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.sync() }
    val now = remember { LocalDateTime.now() }
    var logging by remember { mutableStateOf<MetricDefinition?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = greeting(now.hour), eyebrow = now.format(DateFormat), onOpenSettings = onOpenSettings)
        val summaryOf = { metric: MetricDefinition -> summaries[metric.id] ?: MetricSummary.empty(metric) }
        ScreenColumn(Modifier.weight(1f)) {
            if (LocalWideWindow.current) {
                // Null until read, so no widget flashes.
                pinned?.let { Dashboard(it, summaryOf, quickLog, onLog = { logging = it }, onOpenMetric = onOpenMetric) }
            } else {
                if (quickLog.isNotEmpty()) QuickLogRow(quickLog, onLog = { logging = it })
                pinned?.takeIf { it.isNotEmpty() }?.let { metrics -> PinnedGrid(metrics, summaryOf, onOpenMetric) }
            }
        }
    }
    LogEntrySheet(metric = logging, onDismiss = { logging = null })
}

/** The quick-log chips (design: Today), wrapping onto a second line when they don't fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogRow(metrics: List<MetricDefinition>, onLog: (MetricDefinition) -> Unit) {
    val spacing = LauWellTheme.spacing
    Section("Quick log") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            metrics.forEach { metric ->
                QuickLogChip(icon = metric.icon, label = metric.label, category = metric.category, onClick = { onLog(metric) })
            }
        }
    }
}

/**
 * The pinned tiles, three per row; a short last row keeps the tile width. The design's edit icon in
 * the header waits for the pinned editor (M6).
 */
@Composable
private fun PinnedGrid(
    metrics: List<MetricDefinition>,
    summaryOf: (MetricDefinition) -> MetricSummary,
    onOpenMetric: (MetricId) -> Unit,
) {
    val spacing = LauWellTheme.spacing
    Section("Pinned") {
        Panel(contentPadding = PaddingValues(spacing.panel)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                metrics.chunked(PinnedColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        row.forEach { metric ->
                            val summary = summaryOf(metric)
                            PinnedTile(
                                icon = metric.icon,
                                name = metric.label,
                                value = summary.value,
                                unit = metric.quantities.map { it.unit }.distinct().singleOrNull().orEmpty(),
                                context = summary.context,
                                category = metric.category,
                                progress = summary.progress,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenMetric(metric.id) },
                            )
                        }
                        repeat(PinnedColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private const val PinnedColumns = 3

/**
 * The Fold-open dashboard (design v03 18a, from 17a): the pinned metrics as widgets ([dashboardSpan]
 * sizes them), then Quick log across the full width. Medications join it with their feature; editing
 * (remove, resize, add) is parked (Notion: *Today dashboard (Fold open): Edit mode*).
 */
@Composable
private fun Dashboard(
    pinned: List<MetricDefinition>,
    summaryOf: (MetricDefinition) -> MetricSummary,
    quickLog: List<MetricDefinition>,
    onLog: (MetricDefinition) -> Unit,
    onOpenMetric: (MetricId) -> Unit,
) {
    val spans = pinned.map { dashboardSpan(it.id) }
    WidgetGrid(spans = spans + listOfNotNull(QuickLogSpan.takeIf { quickLog.isNotEmpty() })) {
        pinned.zip(spans).forEach { (metric, span) ->
            val summary = summaryOf(metric)
            MetricWidget(
                icon = metric.icon,
                name = metric.label,
                value = summary.value,
                unit = metric.quantities.map { it.unit }.distinct().singleOrNull().orEmpty(),
                takeaway = summary.takeaway,
                category = metric.category,
                chart = summary.chart,
                large = span.large,
                onClick = { onOpenMetric(metric.id) },
            )
        }
        if (quickLog.isNotEmpty()) QuickLogWidget(quickLog, onLog)
    }
}

/** Quick log as a full-width widget: the chips in one row, scrolling sideways if they don't fit. */
@Composable
private fun QuickLogWidget(metrics: List<MetricDefinition>, onLog: (MetricDefinition) -> Unit) {
    WidgetFrame {
        WidgetTitle(icon = R.drawable.ic_bolt, title = "Quick log")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs),
        ) {
            metrics.forEach { metric ->
                QuickLogChip(icon = metric.icon, label = metric.label, category = metric.category, onClick = { onLog(metric) })
            }
        }
    }
}

/**
 * A pinned metric's widget size on the dashboard, as design 17a draws them: steps 2 × 2, fatigue
 * 2 × 1, the others 1 × 1. Fixed until editing lets you choose.
 */
internal fun dashboardSpan(id: MetricId): WidgetSpan = when (id.value) {
    "steps" -> WidgetSpan(2, 2)
    "fatigue" -> WidgetSpan(2, 1)
    else -> WidgetSpan(1, 1)
}

private val QuickLogSpan = WidgetSpan(4, 1)

private val DateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

internal fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
