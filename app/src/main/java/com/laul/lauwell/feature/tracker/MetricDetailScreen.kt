package com.laul.lauwell.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.ui.components.CategoryLine
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * One metric's detail, shared by every metric (`metric/{id}`). For now: where it is filed and an
 * empty value; the chart, stats and history come with M3 (Steps detail as designed).
 */
@Composable
fun MetricDetailScreen(metricId: String, onBack: () -> Unit) {
    val metric = runCatching { MetricId(metricId) }.getOrNull()?.let { MetricRegistry[it] }
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = metric?.label ?: "Not found",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs),
        )
        ScreenColumn {
            if (metric == null) {
                BasicText("This metric doesn't exist (any more).", style = type.body.copy(color = colors.ink2))
                return@ScreenColumn
            }
            Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
                CategoryLine(metric.category, "${metric.category.label} · ${metric.subCategory.label}")
                BasicText("—", style = type.metricXl.copy(color = colors.ink))
                BasicText("No data yet", style = type.body.copy(color = colors.ink2))
            }
        }
    }
}
