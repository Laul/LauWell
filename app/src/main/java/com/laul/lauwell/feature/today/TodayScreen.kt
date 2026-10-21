package com.laul.lauwell.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.summary.MetricSummary
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PinnedTile
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Today, the home tab: date, greeting, the avatar that opens Settings, and the pinned tiles (empty
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
    val colors = LauWellTheme.colors

    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
                BasicText(
                    text = now.format(DateFormat).uppercase(),
                    style = LauWellTheme.typography.label.copy(color = colors.ink2),
                )
                BasicText(
                    text = greeting(now.hour),
                    style = LauWellTheme.typography.headline.copy(color = colors.ink),
                    modifier = Modifier.semantics { heading() },
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(LauWellTheme.shapes.tile)
                    .background(colors.surface2)
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                LauWellIcon(R.drawable.ic_person, contentDescription = "Settings", tint = colors.ink, size = 22.dp)
            }
        }
        pinned?.takeIf { it.isNotEmpty() }?.let { metrics ->
            PinnedGrid(metrics) { summaries[it.id] ?: MetricSummary.empty(it) }
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
