package com.laul.lauwell.core.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.ui.theme.LauWellTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.CandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.absolute
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import kotlin.math.roundToInt

// The only file that imports Vico (CLAUDE.md → Stack → Charts): ChartSpec in, Vico chart out.
// x = the slot index in ChartSpec.slots, so a slot without data is an x without an entry. The
// design's conventions (Steps detail, UI-spec): bars in the category fg, today's bar outlined,
// below-goal bars dimmed, dashed goal line, a missing day as a dashed empty slot (bars and range
// bars, week only: in a month or 3 months it would be noise, so there it is just a gap); a line
// breaks at a gap instead.

/** Width of a bar, range bar and empty slot per period: a week has room, a month has 30 slots. */
private fun ChartPeriod.barWidth(): Dp = when (this) {
    ChartPeriod.Week -> 28.dp
    ChartPeriod.Month -> 6.dp
    ChartPeriod.ThreeMonths -> 14.dp
}

private fun ChartPeriod.rangeWidth(): Dp = when (this) {
    ChartPeriod.Week -> 10.dp
    ChartPeriod.Month -> 5.dp
    ChartPeriod.ThreeMonths -> 8.dp
}

private fun ChartPeriod.dotSize(): Dp = when (this) {
    ChartPeriod.Week -> 8.dp
    ChartPeriod.Month -> 5.dp
    ChartPeriod.ThreeMonths -> 7.dp
}

private val ChartHeight = 180.dp

/** The header ("14 – 20 Oct" · "Goal 8,000") and [spec] drawn below it, or its empty state. */
@Composable
fun Chart(spec: ChartSpec, modifier: Modifier = Modifier) {
    val colors = LauWellTheme.colors
    val caption = LauWellTheme.typography.caption.copy(color = colors.ink2)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(spec.rangeLabel(), style = caption)
            spec.referenceLabel()?.let { BasicText(it, style = caption) }
        }
        if (spec.hasData) CartesianChart(spec) else EmptyChart()
    }
}

@Composable
private fun CartesianChart(spec: ChartSpec) {
    val bounds = spec.yBounds() ?: return
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(spec.category).fg

    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(spec) {
        val present = spec.slots.withIndex().filter { it.value.point != null }
        val x = present.map { it.index }
        val points = present.map { it.value.point!! }
        producer.runTransaction {
            when (spec.kind) {
                ChartKind.Bar -> columnSeries { series(x = x, y = points.map { it.value }) }
                ChartKind.StackedBar -> columnSeries {
                    // Series 0: slots without a breakdown, as one plain bar; then one series per part.
                    // Every series has every slot (0 where it has nothing), as stacking expects.
                    series(x = x, y = points.map { if (it.parts.isEmpty()) it.value else 0.0 })
                    spec.parts.indices.forEach { part ->
                        series(x = x, y = points.map { it.parts.getOrElse(part) { 0.0 } })
                    }
                }
                ChartKind.Line -> lineSeries {
                    // One series per run of consecutive slots, so the line breaks at a gap.
                    runs(x).forEach { run -> series(x = run, y = run.map { spec.slots[it].point!!.value }) }
                }
                ChartKind.RangeBar -> {
                    // A range bar is a candle whose body runs from low to high; it has no wicks.
                    candlestickSeries(
                        x = x,
                        opening = points.map { it.low },
                        closing = points.map { it.high },
                        low = points.map { it.low },
                        high = points.map { it.high },
                    )
                    lineSeries { series(x = x, y = points.map { it.value }) }
                }
            }
        }
    }

    // Every layer shares one x and y range: all slots of the period, and the padded bounds.
    val range = remember(spec.slots.size, bounds) {
        CartesianLayerRangeProvider.fixed(
            minX = 0.0,
            maxX = spec.slots.lastIndex.toDouble(),
            minY = bounds.start,
            maxY = bounds.endInclusive,
        )
    }
    val layers: List<CartesianLayer<*>> = when (spec.kind) {
        ChartKind.Bar -> listOf(barLayer(spec, fg, range))
        ChartKind.StackedBar -> listOf(stackedLayer(spec, fg, range))
        ChartKind.Line -> listOf(lineLayer(spec, fg, range))
        ChartKind.RangeBar -> listOfNotNull(rangeLayer(spec, fg, range), averageLayer(spec, range).takeIf { spec.markAverage })
    }

    val axisLabel = rememberTextComponent(style = type.caption.copy(color = colors.ink2))
    val gridLine = rememberLineComponent(Fill(colors.lineSoft), thickness = 1.dp)
    val bandFill = rememberShapeComponent(Fill(colors.forCategory(spec.category).tint))
    val decorations = remember(spec, bandFill, colors) {
        listOfNotNull(
            spec.band?.let { NormalBand(it, bandFill) },
            spec.goal?.let { GoalLine(it.value, colors.ink2.copy(alpha = 0.55f)) },
            // Bars and range bars only: a line already shows a missing day as a break.
            EmptySlots(spec.slots.indices.filter { spec.slots[it].point == null }, spec.period.barWidth(), colors.lineSoft)
                .takeIf { spec.period == ChartPeriod.Week && spec.kind != ChartKind.Line },
        )
    }
    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = type.caption.copy(color = colors.onAccent),
            padding = Insets(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
            background = rememberShapeComponent(Fill(colors.accent), RoundedCornerShape(6.dp)),
        ),
        // From the spec by slot, never from the order of the marked entries (TrackAid's tooltip
        // assumed three entries = min, max, avg and broke when a layer was added).
        valueFormatter = remember(spec) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                targets.firstOrNull()?.let { spec.tooltip(it.x.roundToInt()) }.orEmpty()
            }
        },
        guideline = rememberLineComponent(Fill(colors.ink2), thickness = 1.dp),
    )
    val ink = colors.ink

    CartesianChartHost(
        chart = rememberCartesianChart(
            *layers.toTypedArray(),
            // Bars read against their goal line, as in the design; values without a goal need a scale.
            startAxis = if (spec.kind == ChartKind.Bar || spec.kind == ChartKind.StackedBar) null else VerticalAxis.rememberStart(
                label = axisLabel,
                line = null,
                tick = null,
                guideline = gridLine,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                line = rememberLineComponent(Fill(colors.panelLine), thickness = 1.dp),
                tick = null,
                guideline = null,
                // Vico refuses blank labels: which slots are labeled is the placer's job.
                itemPlacer = remember(spec.labelSpacing, spec.labelOffset) {
                    HorizontalAxis.ItemPlacer.aligned(spacing = { spec.labelSpacing }, offset = { spec.labelOffset })
                },
                valueFormatter = remember(spec, ink) {
                    CartesianValueFormatter { _, x, _ ->
                        val index = x.roundToInt()
                        val text = spec.slotLabel(index)
                        // Today (or this week) in bold ink, like the design's day labels.
                        if (index == spec.currentIndex) {
                            buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ink)) { append(text) } }
                        } else {
                            text
                        }
                    }
                },
            ),
            marker = marker,
            decorations = decorations,
        ),
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth().height(ChartHeight),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** Bars in the category color: the current slot outlined, slots below the goal dimmed. */
@Composable
private fun barLayer(spec: ChartSpec, fg: Color, range: CartesianLayerRangeProvider): CartesianLayer<*> {
    val width = spec.period.barWidth()
    val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    val filled = rememberLineComponent(Fill(fg), width, shape)
    val dimmed = rememberLineComponent(Fill(fg.copy(alpha = 0.55f)), width, shape)
    val outlined = rememberLineComponent(Fill.Transparent, width, shape, strokeFill = Fill(fg), strokeThickness = 2.dp)
    val provider = remember(spec, filled, dimmed, outlined) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(entry: ColumnCartesianLayerModel.Entry, extraStore: ExtraStore): LineComponent {
                val index = entry.x.roundToInt()
                return when {
                    index == spec.currentIndex -> outlined
                    spec.meetsGoal(index) == false -> dimmed
                    else -> filled
                }
            }

            override fun getWidestSeriesColumn(seriesKey: Any, seriesIndex: Int, extraStore: ExtraStore) = filled
        }
    }
    return rememberColumnCartesianLayer(columnProvider = provider, rangeProvider = range)
}

/**
 * Stacked bars: each part in the category color at its [partAlpha], a slot without a breakdown in
 * the plain color. Not dimmed below the goal, nor outlined today: the parts' shades carry the meaning.
 */
@Composable
private fun stackedLayer(spec: ChartSpec, fg: Color, range: CartesianLayerRangeProvider): CartesianLayer<*> {
    val width = spec.period.barWidth()
    val shape = RoundedCornerShape(2.dp)
    val plain = rememberLineComponent(Fill(fg), width, shape)
    val parts = spec.parts.indices.map { rememberLineComponent(Fill(fg.copy(alpha = partAlpha(it))), width, shape) }
    return rememberColumnCartesianLayer(
        columnProvider = remember(plain, parts) { ColumnCartesianLayer.ColumnProvider.series(listOf(plain) + parts) },
        mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
        rangeProvider = range,
    )
}

/** A 2dp line in the category color with a dot per slot; [runs] of the model break it at gaps. */
@Composable
private fun lineLayer(spec: ChartSpec, fg: Color, range: CartesianLayerRangeProvider): CartesianLayer<*> {
    val dot = LineCartesianLayer.Point(rememberShapeComponent(Fill(fg), CircleShape), size = spec.period.dotSize())
    return rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.rememberLine(
                fill = remember(fg) { LineCartesianLayer.LineFill.single(Fill(fg)) },
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                pointProvider = LineCartesianLayer.PointProvider.single(dot),
            ),
        ),
        rangeProvider = range,
    )
}

@Composable
private fun rangeLayer(spec: ChartSpec, fg: Color, range: CartesianLayerRangeProvider): CartesianLayer<*> {
    val bar = CandlestickCartesianLayer.Candle(
        body = rememberLineComponent(Fill(fg), thickness = spec.period.rangeWidth(), shape = RoundedCornerShape(50)),
    )
    return rememberCandlestickCartesianLayer(
        candleProvider = CandlestickCartesianLayer.CandleProvider.absolute(bullish = bar, neutral = bar, bearish = bar),
        rangeProvider = range,
    )
}

/** The average of each range bar as a hollow dot: a line layer with points only. */
@Composable
private fun averageLayer(spec: ChartSpec, range: CartesianLayerRangeProvider): CartesianLayer<*> {
    val colors = LauWellTheme.colors
    val dot = LineCartesianLayer.Point(
        rememberShapeComponent(Fill(colors.panel), CircleShape, strokeFill = Fill(colors.ink), strokeThickness = 2.dp),
        size = spec.period.dotSize(),
    )
    return rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.rememberLine(
                fill = remember { LineCartesianLayer.LineFill.single(Fill.Transparent) },
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 0.dp),
                pointProvider = LineCartesianLayer.PointProvider.single(dot),
            ),
        ),
        rangeProvider = range,
    )
}

/** Consecutive runs of [x] ([0, 1, 3, 4, 5] → [0, 1], [3, 4, 5]). */
internal fun runs(x: List<Int>): List<List<Int>> =
    x.fold(mutableListOf<MutableList<Int>>()) { acc, i ->
        if (acc.isNotEmpty() && acc.last().last() == i - 1) acc.last() += i else acc += mutableListOf(i)
        acc
    }

/** The canvas x of slot [x]'s center, as the layers place it. */
private fun CartesianDrawingContext.slotCenterX(x: Int): Float {
    val start = if (isLtr) layerBounds.left else layerBounds.right
    return start + layerDimensions.startPadding * layoutDirectionMultiplier - scroll +
        layoutDirectionMultiplier * layerDimensions.xSpacing * ((x - ranges.minX) / ranges.xStep).toFloat()
}

private fun CartesianDrawingContext.canvasY(value: Double): Float {
    val yRange = ranges.getYRange(null)
    return (layerBounds.bottom - (value - yRange.minY) / yRange.length * layerBounds.height).toFloat()
}

/** The quantity's normal range as a tinted band behind everything. */
private class NormalBand(private val y: ClosedFloatingPointRange<Double>, private val box: ShapeComponent) : Decoration {
    override fun drawUnderLayers(context: CartesianDrawingContext) {
        with(context) { box.draw(context, layerBounds.left, canvasY(y.endInclusive), layerBounds.right, canvasY(y.start)) }
    }
}

/** The goal as a 1.5dp dashed line across the chart, behind the bars. */
private class GoalLine(private val value: Double, private val color: Color) : Decoration {
    override fun drawUnderLayers(context: CartesianDrawingContext) {
        with(context) {
            val y = canvasY(value)
            val paint = Paint().apply {
                this.color = this@GoalLine.color
                style = PaintingStyle.Stroke
                strokeWidth = 1.5.dp.pixels
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.pixels, 4.dp.pixels))
            }
            canvas.drawLine(Offset(layerBounds.left, y), Offset(layerBounds.right, y), paint)
        }
    }
}

/** A day without data as a full-height dashed slot ("watch not worn"), never as a zero bar. */
private class EmptySlots(private val slots: List<Int>, private val width: Dp, private val color: Color) : Decoration {
    override fun drawUnderLayers(context: CartesianDrawingContext) {
        if (slots.isEmpty()) return
        with(context) {
            val half = width.pixels / 2
            val radius = 6.dp.pixels
            val paint = Paint().apply {
                this.color = this@EmptySlots.color
                style = PaintingStyle.Stroke
                strokeWidth = 1.5.dp.pixels
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.pixels, 3.dp.pixels))
            }
            slots.forEach { x ->
                val center = slotCenterX(x)
                val left = (center - half).coerceAtLeast(layerBounds.left)
                val right = (center + half).coerceAtMost(layerBounds.right)
                canvas.drawRoundRect(left, layerBounds.top, right, layerBounds.bottom, radius, radius, paint)
            }
        }
    }
}

@Composable
private fun EmptyChart() {
    Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
        BasicText("No data in this period", style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2))
    }
}
