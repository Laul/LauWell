package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** How many grid columns and rows a widget covers (design 17a: 1 × 1, 2 × 1, 2 × 2, 4 × 1). */
data class WidgetSpan(val columns: Int, val rows: Int) {
    init {
        require(columns >= 1 && rows >= 1) { "A widget covers at least one cell" }
    }

    val large: Boolean get() = rows >= 2
}

/** The top-left cell of a placed widget. */
data class WidgetCell(val column: Int, val row: Int)

/**
 * Places [spans], in order, on a grid [columns] wide, each in the first free spot reading row by row
 * (like a home screen): small widgets fill the gaps beside a large one. A span wider than the grid is
 * narrowed to it.
 */
fun packWidgets(spans: List<WidgetSpan>, columns: Int): List<WidgetCell> {
    val taken = mutableSetOf<Pair<Int, Int>>()
    return spans.map { span ->
        val width = span.columns.coerceAtMost(columns)
        fun fits(column: Int, row: Int) =
            (column until column + width).all { x -> (row until row + span.rows).all { y -> (x to y) !in taken } }
        val (column, row) = generateSequence(0) { it + 1 }
            .firstNotNullOf { row -> (0..columns - width).firstOrNull { fits(it, row) }?.let { it to row } }
        for (x in column until column + width) for (y in row until row + span.rows) taken += x to y
        WidgetCell(column, row)
    }
}

/**
 * The dashboard grid of the Fold's open screen (design v03, 18a / 17a): [columns] equal columns,
 * rows [WidgetRowHeight] high, [WidgetGap] between cells. Child `i` covers `spans[i]`, placed by
 * [packWidgets]; as tall as its rows, so it scrolls with the screen.
 */
@Composable
fun WidgetGrid(
    spans: List<WidgetSpan>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    content: @Composable () -> Unit,
) {
    val cells = packWidgets(spans, columns)
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = WidgetGap.roundToPx()
        val rowHeight = WidgetRowHeight.roundToPx()
        val columnWidth = (constraints.maxWidth - gap * (columns - 1)) / columns
        val placeables = measurables.zip(spans).map { (measurable, span) ->
            val w = span.columns.coerceAtMost(columns)
            measurable.measure(
                Constraints.fixed(w * columnWidth + (w - 1) * gap, span.rows * rowHeight + (span.rows - 1) * gap),
            )
        }
        val rows = cells.zip(spans).maxOfOrNull { (cell, span) -> cell.row + span.rows } ?: 0
        val height = if (rows == 0) 0 else rows * rowHeight + (rows - 1) * gap
        layout(constraints.maxWidth, height) {
            placeables.zip(cells).forEach { (placeable, cell) ->
                placeable.place(cell.column * (columnWidth + gap), cell.row * (rowHeight + gap))
            }
        }
    }
}

/** One widget's frame: the panel color and outline, 12 dp padding; tappable when [onClick] is set. */
@Composable
fun WidgetFrame(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.panel
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(colors.panel)
            .border(LauWellTheme.strokes.hairline, colors.panelLine, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(LauWellTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * A metric on the dashboard (design 17a): icon and name, the Mono value (larger on a 2 × 2 widget),
 * its 7-day [chart] filling the space left, and the takeaway line.
 *
 * @param value null when there is nothing yet (shows "—", never 0).
 */
@Composable
fun MetricWidget(
    @DrawableRes icon: Int,
    name: String,
    value: String?,
    unit: String,
    takeaway: String,
    category: Category,
    chart: MiniChartData?,
    large: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(category).fg
    WidgetFrame(modifier = modifier.semantics(mergeDescendants = true) {}, onClick = onClick) {
        WidgetTitle(icon = icon, title = name, tint = fg)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = value ?: "—",
                style = (if (large) type.metricLarge else type.metric).copy(color = colors.ink),
                maxLines = 1,
            )
            if (unit.isNotEmpty()) BasicText(text = unit, style = type.caption.copy(color = colors.ink2), maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (chart != null) MiniChart(data = chart, color = fg, modifier = Modifier.fillMaxSize())
        }
        BasicText(
            text = takeaway,
            style = type.caption.copy(color = colors.ink2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A widget's title line: a 16 dp icon (category-colored, or ink2) and the name in ink2. */
@Composable
fun WidgetTitle(@DrawableRes icon: Int, title: String, tint: Color = LauWellTheme.colors.ink2) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        LauWellIcon(icon = icon, contentDescription = null, tint = tint, size = 16.dp)
        BasicText(
            text = title,
            style = LauWellTheme.typography.caption.copy(color = LauWellTheme.colors.ink2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Grid row height and gap (design 17a: 118 px rows, 10 px gaps). */
private val WidgetRowHeight = 118.dp
private val WidgetGap = 10.dp
