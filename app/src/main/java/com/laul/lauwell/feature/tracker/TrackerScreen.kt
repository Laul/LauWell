package com.laul.lauwell.feature.tracker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.settings.TrackerView
import com.laul.lauwell.core.summary.MetricSummary
import com.laul.lauwell.core.ui.components.AppHeader
import com.laul.lauwell.core.ui.components.IconToggle
import com.laul.lauwell.core.ui.components.IconToggleOption
import com.laul.lauwell.core.ui.components.MetricRow
import com.laul.lauwell.core.ui.components.MetricWidget
import com.laul.lauwell.core.ui.components.MiniChartData
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.ToolbarButton
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock

/**
 * The Tracker hub (`docs/design/UI-spec.md` → Tracker): every visible metric, grouped by category,
 * read from the registry, as a list with sparklines (15a, default) or 2-up widgets (15b). The
 * header toggle switches views and the choice is stored. A row or widget opens the metric's detail.
 */
@Composable
fun TrackerScreen(onOpenMetric: (MetricId) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel = appViewModel {
        TrackerViewModel(toggleRepository, displayPreferences, summaryStore, healthSync, Clock.systemDefaultZone())
    }
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val view by viewModel.view.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.sync() }

    TrackerContent(
        groups = groups,
        view = view,
        onViewChange = viewModel::setView,
        onOpenMetric = onOpenMetric,
        onOpenSettings = onOpenSettings,
        summaryOf = { summaries[it.id] ?: MetricSummary.empty(it) },
    )
}

@Composable
private fun TrackerContent(
    groups: List<TrackerGroup>?,
    view: TrackerView?,
    onViewChange: (TrackerView) -> Unit,
    onOpenMetric: (MetricId) -> Unit,
    onOpenSettings: () -> Unit,
    summaryOf: (MetricDefinition) -> MetricSummary,
) {
    val spacing = LauWellTheme.spacing
    Column(Modifier.fillMaxSize()) {
        // Toolbar (design 20a): what is shown (Filter, Sort) on the left, how (view) on the right.
        // Filter and Sort stay disabled until their options are defined (Notion: *Tracker Filter & Sort*).
        AppHeader(title = "Tracker", onAvatarClick = onOpenSettings) {
            ToolbarButton(R.drawable.ic_filter_list, "Filter", onClick = null, label = "All metrics")
            ToolbarButton(R.drawable.ic_sort, "Sort", onClick = null)
            Spacer(Modifier.weight(1f))
            if (view != null) {
                IconToggle(
                    options = ViewOptions,
                    selectedIndex = TrackerView.entries.indexOf(view),
                    onSelect = { onViewChange(TrackerView.entries[it]) },
                )
            }
        }
        TrackerList(groups, view, onOpenMetric, summaryOf, Modifier.weight(1f))
    }
}

@Composable
private fun TrackerList(
    groups: List<TrackerGroup>?,
    view: TrackerView?,
    onOpenMetric: (MetricId) -> Unit,
    summaryOf: (MetricDefinition) -> MetricSummary,
    modifier: Modifier = Modifier,
) {
    val spacing = LauWellTheme.spacing
    // Each category is one keyed item, so switching views keeps the same category in view.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.xs, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (groups == null || view == null) return@LazyColumn
        if (groups.isEmpty()) {
            item(key = "empty") {
                BasicText(
                    text = "Nothing to track: every metric is switched off.",
                    style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
                )
            }
        }
        items(groups, key = { it.category.key }) { group ->
            Section(label = group.category.label, category = group.category) {
                when (view) {
                    TrackerView.List -> MetricList(group, summaryOf, onOpenMetric)
                    TrackerView.Widgets -> MetricGrid(group, summaryOf, onOpenMetric)
                }
            }
        }
    }
}

/** Same order as [TrackerView.entries]. */
private val ViewOptions = listOf(
    IconToggleOption(R.drawable.ic_view_list, "List view"),
    IconToggleOption(R.drawable.ic_grid_view, "Widgets view"),
)

@Composable
private fun MetricList(
    group: TrackerGroup,
    summaryOf: (MetricDefinition) -> MetricSummary,
    onOpenMetric: (MetricId) -> Unit,
) {
    Panel {
        group.metrics.forEachIndexed { index, metric ->
            if (index > 0) PanelDivider()
            val summary = summaryOf(metric)
            MetricRow(
                icon = metric.icon,
                name = metric.label,
                takeaway = summary.takeaway,
                value = summary.value,
                category = metric.category,
                chart = summary.chart,
                onClick = { onOpenMetric(metric.id) },
            )
        }
    }
}

/** Two widgets per row; with an odd count the last one spans the full width. */
@Composable
private fun MetricGrid(
    group: TrackerGroup,
    summaryOf: (MetricDefinition) -> MetricSummary,
    onOpenMetric: (MetricId) -> Unit,
) {
    val gap = LauWellTheme.spacing.xs
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        group.metrics.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                pair.forEach { metric ->
                    val summary = summaryOf(metric)
                    MetricWidget(
                        icon = metric.icon,
                        name = metric.label,
                        takeaway = summary.takeaway,
                        value = summary.value,
                        unit = metric.displayUnit,
                        category = metric.category,
                        chart = summary.chart,
                        onClick = { onOpenMetric(metric.id) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/** The unit shown after a widget's value, when all of the metric's quantities share one. */
private val MetricDefinition.displayUnit: String?
    get() = quantities.map { it.unit }.distinct().singleOrNull()

// Previews use the design's sample week (UI-spec 15a/15b) so the charts can be checked by eye.

private val SampleSummaries: Map<String, MetricSummary> = mapOf(
    "heart_rate" to MetricSummary(
        value = "72",
        takeaway = "52–148 today",
        chart = MiniChartData.Range(
            listOf(54.0..139.0, 52.0..121.0, 55.0..152.0, 51.0..117.0, 53.0..144.0, 56.0..128.0, 52.0..148.0),
            min = 40.0,
            max = 160.0,
        ),
    ),
    "resting_heart_rate" to MetricSummary(
        value = "64",
        takeaway = "Avg 64 · in range",
        chart = MiniChartData.Line(listOf(66.0, 63.0, 65.0, 62.0, 64.0, 67.0, 64.0), min = 56.0, max = 72.0),
    ),
    "blood_pressure" to MetricSummary(
        value = "118/76",
        takeaway = "3 readings · in range",
        chart = MiniChartData.Range(
            listOf(79.0..121.0, null, 81.0..124.0, 76.0..118.0, null, null, null),
            min = 60.0,
            max = 135.0,
        ),
    ),
)

@Composable
private fun TrackerPreview(initial: TrackerView) {
    LauWellTheme {
        var view by remember { mutableStateOf(initial) }
        Column(Modifier.fillMaxSize().background(LauWellTheme.colors.bg)) {
            TrackerContent(
                groups = trackerGroups(MetricRegistry.all),
                view = view,
                onViewChange = { view = it },
                onOpenMetric = {},
                onOpenSettings = {},
                summaryOf = { SampleSummaries[it.id.value] ?: MetricSummary.empty(it) },
            )
        }
    }
}

@Preview(name = "List light", widthDp = 360, heightDp = 640)
@Preview(name = "List dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackerListPreview() = TrackerPreview(TrackerView.List)

@Preview(name = "Widgets light", widthDp = 360, heightDp = 640)
@Preview(name = "Widgets dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackerWidgetsPreview() = TrackerPreview(TrackerView.Widgets)
