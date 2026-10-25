package com.laul.lauwell.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Today, the home tab: the header (date, greeting, and the avatar that opens Settings), and the pinned tiles (empty
 * values until M3 reads Health Connect). Quick log comes with manual entries (M4); medications,
 * the symptom check and upcoming appointments with M5–M6.
 */
@Composable
fun TodayScreen(onOpenSettings: () -> Unit) {
    val viewModel = appViewModel {
        TodayViewModel(toggleRepository, pinnedMetrics, summaryStore, healthSync, Clock.systemDefaultZone())
    }
    val pinned by viewModel.pinned.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.sync() }
    val now = remember { LocalDateTime.now() }

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = greeting(now.hour), eyebrow = now.format(DateFormat), onAvatarClick = onOpenSettings)
        ScreenColumn(Modifier.weight(1f)) {
            pinned?.takeIf { it.isNotEmpty() }?.let { metrics ->
                PinnedGrid(metrics) { summaries[it.id] ?: MetricSummary.empty(it) }
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
