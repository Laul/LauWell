package com.laul.lauwell.feature.tracker

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.components.MetricRow
import com.laul.lauwell.core.ui.components.MetricSource
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.ScreenHeader
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * The Tracker hub: every visible metric, grouped by category, read from the registry. A row
 * opens the metric's detail. Values come with M3 (Health Connect) and M4 (manual entries).
 */
@Composable
fun TrackerScreen(onOpenMetric: (MetricId) -> Unit) {
    val viewModel = appViewModel { TrackerViewModel(toggleRepository) }
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    ScreenColumn {
        // The design's search icon waits until there is something to search.
        ScreenHeader("Tracker")
        val loaded = groups ?: return@ScreenColumn
        if (loaded.isEmpty()) {
            BasicText(
                text = "Nothing to track: every metric is switched off.",
                style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
            )
        }
        loaded.forEach { group ->
            Section(label = group.category.label, category = group.category) {
                Panel {
                    group.metrics.forEachIndexed { index, metric ->
                        if (index > 0) PanelDivider()
                        MetricRow(
                            icon = metric.icon,
                            name = metric.label,
                            context = "No data yet",
                            value = null,
                            category = metric.category,
                            source = if (metric.isManual) MetricSource.Manual else MetricSource.Synced,
                            onClick = { onOpenMetric(metric.id) },
                        )
                    }
                }
            }
        }
    }
}
