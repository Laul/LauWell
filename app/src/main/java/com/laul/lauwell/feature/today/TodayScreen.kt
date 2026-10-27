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
import com.laul.lauwell.core.catalog.MetricDefinition
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
 * Today, the home tab: the header (date, greeting, and the gear that opens Settings), the quick-log
 * chips (each opens its entry sheet) and the pinned tiles. Medications, the symptom check and
 * upcoming appointments come with M5–M6.
 */
@Composable
fun TodayScreen(onOpenSettings: () -> Unit) {
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
        ScreenColumn(Modifier.weight(1f)) {
            if (quickLog.isNotEmpty()) QuickLogRow(quickLog, onLog = { logging = it })
            pinned?.takeIf { it.isNotEmpty() }?.let { metrics ->
                PinnedGrid(metrics) { summaries[it.id] ?: MetricSummary.empty(it) }
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
private fun PinnedGrid(metrics: List<MetricDefinition>, summaryOf: (MetricDefinition) -> MetricSummary) {
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

private val DateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

internal fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
