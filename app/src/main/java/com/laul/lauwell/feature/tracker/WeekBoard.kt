package com.laul.lauwell.feature.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.summary.WeekRow
import com.laul.lauwell.core.summary.ago
import com.laul.lauwell.core.ui.components.AppHeader
import com.laul.lauwell.core.ui.components.CategoryDot
import com.laul.lauwell.core.ui.components.IconAction
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.MiniChart
import com.laul.lauwell.core.ui.components.MiniChartDays
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tracker on a wide window (the Fold open; design v03 18b, from 16c): the week board on the left,
 * every metric on one shared 7-day axis, and the day pane on the right with everything logged on the
 * selected day. Tapping a metric (its name on the board, or its row in the day pane) opens its detail.
 * The header keeps Tracker's title, search and gear; the week and its arrows take the toolbar row.
 * Medications and "Log for this day" join the day pane with their features.
 */
@Composable
internal fun WeekBoardScreen(
    groups: List<TrackerGroup>?,
    board: WeekBoard?,
    onMoveWeek: (Int) -> Unit,
    onOpenMetric: (MetricId) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = LauWellTheme.spacing
    // The selected day, as an index in the week (the last = the week's end); today at first.
    var selected by rememberSaveable { mutableIntStateOf(MiniChartDays - 1) }
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Tracker", onOpenSettings = onOpenSettings) {
            Spacer(Modifier.weight(1f))
            board?.let { WeekSwitcher(it, onMoveWeek) }
        }
        if (groups == null || board == null) return@Column
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // The day pane takes a third of the width, at least the design's 290 dp (the Fold opens wider than its 884 dp).
            val paneWidth = maxOf(MinDayPaneWidth, maxWidth / 3)
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Board(groups, board, selected, onSelect = { selected = it }, onOpenMetric = onOpenMetric)
                    BasicText(
                        text = "Tap a day to see everything logged on it. Rows share one time axis, so patterns line up vertically.",
                        style = LauWellTheme.typography.caption.copy(color = LauWellTheme.colors.ink2),
                    )
                }
                DayPane(
                    metrics = groups.flatMap { it.metrics },
                    board = board,
                    selected = selected,
                    onOpenMetric = onOpenMetric,
                    modifier = Modifier.width(paneWidth).fillMaxHeight(),
                )
            }
        }
    }
}

/** "‹ 10 – 16 Oct ›": the week shown; the forward arrow is off on the current week. */
@Composable
private fun WeekSwitcher(board: WeekBoard, onMoveWeek: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAction(R.drawable.ic_chevron_left, "Previous week", onClick = { onMoveWeek(-1) })
        BasicText(
            text = weekLabel(board.end),
            style = LauWellTheme.typography.control.copy(color = LauWellTheme.colors.ink),
            maxLines = 1,
        )
        IconAction(R.drawable.ic_chevron_right, "Next week", onClick = { onMoveWeek(1) }, enabled = !board.isCurrent)
    }
}

/**
 * The board: a "Metric" column, then one column per day, as wide as the room allows. The selected
 * day's column is shaded behind every row; tapping a day header or a lane selects that day.
 */
@Composable
private fun Board(
    groups: List<TrackerGroup>,
    board: WeekBoard,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenMetric: (MetricId) -> Unit,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val column = (maxWidth - MetricColumnWidth) / MiniChartDays
        Column(
            Modifier.drawBehind {
                val inset = 2.dp.toPx()
                drawRoundRect(
                    color = colors.surface2,
                    topLeft = Offset((MetricColumnWidth + column * selected).toPx() + inset, 0f),
                    size = Size(column.toPx() - 2 * inset, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .drawBehind {
                        val rule = 2.dp.toPx()
                        drawLine(colors.line, Offset(0f, size.height - rule / 2), Offset(size.width, size.height - rule / 2), rule)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "METRIC",
                    style = type.label.copy(color = colors.ink),
                    modifier = Modifier.width(MetricColumnWidth),
                )
                weekDates(board.end).forEachIndexed { index, date ->
                    DayHeader(date, index == selected, Modifier.width(column).clickable(role = Role.Tab) { onSelect(index) })
                }
            }
            groups.forEach { group ->
                Row(
                    modifier = Modifier.height(32.dp).padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    CategoryDot(colors.forCategory(group.category).fg)
                    BasicText(
                        text = group.category.label.uppercase(),
                        style = type.label.copy(color = colors.ink2),
                        maxLines = 1,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                group.metrics.forEach { metric ->
                    BoardRow(metric, board.rows[metric.id], column, onSelect, onOpenMetric)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, selected: Boolean, modifier: Modifier) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val color = if (selected) colors.ink else colors.ink2
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        BasicText(text = date.format(DayLetter).take(1), style = type.label.copy(color = color))
        BasicText(text = date.dayOfMonth.toString(), style = type.caption.copy(color = color, fontFamily = type.metric.fontFamily))
    }
}

/** One metric: icon, name and latest value (tap: its detail), then its week drawn across the day columns. */
@Composable
private fun BoardRow(
    metric: MetricDefinition,
    row: WeekRow?,
    column: Dp,
    onSelect: (Int) -> Unit,
    onOpenMetric: (MetricId) -> Unit,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(metric.category).fg
    Row(
        modifier = Modifier
            .height(46.dp)
            .drawBehind {
                drawLine(colors.lineSoft, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .width(MetricColumnWidth)
                .fillMaxHeight()
                .clickable(role = Role.Button) { onOpenMetric(metric.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LauWellIcon(icon = metric.icon, contentDescription = null, tint = fg, size = 18.dp)
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = metric.label,
                    style = type.control.copy(color = colors.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = row?.latest ?: "—",
                    style = type.caption.copy(color = colors.ink2, fontFamily = type.metric.fontFamily),
                    maxLines = 1,
                )
            }
        }
        Box(
            Modifier
                .width(column * MiniChartDays)
                .height(30.dp)
                .pointerInput(column) {
                    detectTapGestures { onSelect((it.x / column.toPx()).toInt().coerceIn(0, MiniChartDays - 1)) }
                },
        ) {
            row?.chart?.let { MiniChart(data = it, color = fg, modifier = Modifier.fillMaxSize()) }
        }
    }
}

/** The selected day: when it was, its date, and every metric's value that day (tap: its detail). */
@Composable
private fun DayPane(
    metrics: List<MetricDefinition>,
    board: WeekBoard,
    selected: Int,
    onOpenMetric: (MetricId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val spacing = LauWellTheme.spacing
    val date = weekDates(board.end)[selected]
    Column(
        modifier = modifier
            .background(colors.surface)
            .drawBehind { drawLine(colors.panelLine, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx()) }
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = ago(date, LocalDate.now()).uppercase(),
                style = type.label.copy(color = colors.ink2),
            )
            BasicText(
                text = date.format(DayTitle),
                style = type.greeting.copy(color = colors.ink),
                modifier = Modifier.semantics { heading() },
            )
        }
        Panel(contentPadding = PaddingValues(horizontal = spacing.sm)) {
            metrics.forEachIndexed { index, metric ->
                if (index > 0) PanelDivider()
                val value = board.rows[metric.id]?.days?.getOrNull(selected)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .clickable(role = Role.Button) { onOpenMetric(metric.id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LauWellIcon(metric.icon, contentDescription = null, tint = colors.forCategory(metric.category).fg, size = 17.dp)
                    BasicText(
                        text = metric.label,
                        style = type.control.copy(color = colors.ink),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(
                        text = value ?: "Not logged",
                        style = if (value != null) type.metricSmall.copy(color = colors.ink) else type.caption.copy(color = colors.ink2),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The [MiniChartDays] dates of the week ending [end], oldest first. */
internal fun weekDates(end: LocalDate): List<LocalDate> = (MiniChartDays - 1 downTo 0).map { end.minusDays(it.toLong()) }

/** "10 – 16 Oct", or "28 Sep – 4 Oct" across two months. */
internal fun weekLabel(end: LocalDate): String {
    val start = end.minusDays(MiniChartDays - 1L)
    return if (start.month == end.month) "${start.dayOfMonth} – ${end.format(DayMonth)}"
    else "${start.format(DayMonth)} – ${end.format(DayMonth)}"
}

private val MetricColumnWidth = 152.dp
private val MinDayPaneWidth = 290.dp
// English names: the screens around them are English (as in DayList).
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DayTitle = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH)
private val DayLetter = DateTimeFormatter.ofPattern("EEEEE", Locale.ENGLISH)
