package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * The outlined icon tile at the start of a metric row: 36dp, 1.5dp stroke in the category `fg`,
 * no fill.
 */
@Composable
fun MetricIconTile(@DrawableRes icon: Int, category: Category, modifier: Modifier = Modifier) {
    val fg = LauWellTheme.colors.forCategory(category).fg
    val shape = LauWellTheme.shapes.tile
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(shape)
            .border(LauWellTheme.strokes.control, fg, shape),
        contentAlignment = Alignment.Center,
    ) {
        LauWellIcon(icon = icon, contentDescription = null, tint = fg, size = 19.dp)
    }
}

/**
 * One metric in a list, Tracker's default view (15a): icon tile · name and a one-line takeaway ·
 * 7-day [MiniChart] (56 × 24dp) · Mono value, right-aligned. Put rows in a [Panel], separated by
 * [PanelDivider].
 *
 * @param category the metric's own category: it keeps that color on every tab.
 * @param takeaway one line that reads the chart ("60% of goal", "Up from 4 on Mon").
 * @param value null when there is nothing yet (shows "—", never 0).
 * @param chart null for a metric without a mini-chart; its slot stays empty so values line up.
 */
@Composable
fun MetricRow(
    @DrawableRes icon: Int,
    name: String,
    takeaway: String,
    value: String?,
    category: Category,
    chart: MiniChartData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = LauWellTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        MetricIconTile(icon = icon, category = category)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(
                text = name,
                style = type.item.copy(color = colors.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = takeaway,
                style = type.caption.copy(color = colors.ink2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.size(width = 56.dp, height = 24.dp)) {
            if (chart != null) {
                MiniChart(chart, colors.forCategory(category).fg, Modifier.fillMaxSize())
            }
        }
        BasicText(
            text = value ?: "—",
            style = type.metricSmall.copy(color = colors.ink, textAlign = TextAlign.End),
            maxLines = 1,
            modifier = Modifier.widthIn(min = 56.dp),
        )
    }
}

/**
 * One metric as a widget, Tracker's grid view (15b): icon and name · value and unit (Mono 20) ·
 * full-width [MiniChart], 34dp high · takeaway. Its own panel: 12dp padding and corners.
 * Fills the height it is given, so the two tiles of a grid row match.
 */
@Composable
fun MetricWidget(
    @DrawableRes icon: Int,
    name: String,
    takeaway: String,
    value: String?,
    unit: String?,
    category: Category,
    chart: MiniChartData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(category).fg
    val shape = LauWellTheme.shapes.panel
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.panel)
            .border(LauWellTheme.strokes.hairline, colors.panelLine, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(LauWellTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LauWellIcon(icon = icon, contentDescription = null, tint = fg, size = 16.dp)
            BasicText(
                text = name,
                style = type.caption.copy(color = colors.ink2, fontSize = 12.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = value ?: "—",
                style = type.metric.copy(color = colors.ink),
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            if (value != null && unit != null) {
                BasicText(
                    text = unit,
                    style = type.caption.copy(color = colors.ink2),
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(34.dp)) {
            if (chart != null) MiniChart(chart, fg, Modifier.fillMaxSize())
        }
        Spacer(Modifier.weight(1f))
        BasicText(
            text = takeaway,
            style = type.caption.copy(color = colors.ink2, fontSize = 11.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
