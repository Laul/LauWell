package com.laul.lauwell.core.ui.chart

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.ui.theme.LauWellTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.CandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.absolute
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlin.math.roundToInt

// The only file that imports Vico (CLAUDE.md → Stack → Charts): ChartSpec in, Vico chart out.
// x = the slot index in ChartSpec.days, so a missing day is simply an x without an entry.

/** Draws [spec], 220dp tall, or its empty state when no day has data. */
@Composable
fun Chart(spec: ChartSpec, modifier: Modifier = Modifier) {
    when (spec) {
        is ChartSpec.RangeBars -> RangeBarsChart(spec, modifier)
    }
}

private val ChartHeight = 220.dp

@Composable
private fun RangeBarsChart(spec: ChartSpec.RangeBars, modifier: Modifier) {
    val bounds = spec.yBounds() ?: return EmptyChart(modifier)
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val categoryColor = colors.forCategory(spec.category)

    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(spec) {
        val present = spec.bars.withIndex().filter { it.value != null }
        val x = present.map { it.index }
        val bars = present.map { it.value!! }
        producer.runTransaction {
            // A range bar is a candle whose body runs from low to high; it has no wicks.
            candlestickSeries(
                x = x,
                opening = bars.map { it.low },
                closing = bars.map { it.high },
                low = bars.map { it.low },
                high = bars.map { it.high },
            )
            lineSeries { series(x = x, y = bars.map { it.average }) }
        }
    }

    // Both layers share one x and y range: every slot of the period, and the padded bounds.
    val range = remember(spec.days.size, bounds) {
        CartesianLayerRangeProvider.fixed(
            minX = 0.0,
            maxX = (spec.days.size - 1).toDouble(),
            minY = bounds.start,
            maxY = bounds.endInclusive,
        )
    }
    val bar = CandlestickCartesianLayer.Candle(
        body = rememberLineComponent(Fill(categoryColor.fg), thickness = 10.dp, shape = RoundedCornerShape(5.dp)),
    )
    val bars = rememberCandlestickCartesianLayer(
        candleProvider = CandlestickCartesianLayer.CandleProvider.absolute(bullish = bar, neutral = bar, bearish = bar),
        rangeProvider = range,
    )
    val averageDot = LineCartesianLayer.Point(
        rememberShapeComponent(Fill(colors.panel), CircleShape, strokeFill = Fill(colors.ink), strokeThickness = 2.dp),
        size = 8.dp,
    )
    val averages = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.rememberLine(
                fill = remember { LineCartesianLayer.LineFill.single(Fill.Transparent) },
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 0.dp),
                pointProvider = LineCartesianLayer.PointProvider.single(averageDot),
            ),
        ),
        rangeProvider = range,
    )

    val axisLabel = rememberTextComponent(style = type.caption.copy(color = colors.ink2))
    val gridLine = rememberLineComponent(Fill(colors.lineSoft), thickness = 1.dp)
    val bandFill = rememberShapeComponent(Fill(categoryColor.tint))
    val band = remember(spec.band, bandFill) { spec.band?.let { NormalBand(it, bandFill) } }
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

    CartesianChartHost(
        chart = rememberCartesianChart(
            bars,
            averages,
            startAxis = VerticalAxis.rememberStart(label = axisLabel, line = null, tick = null, guideline = gridLine),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                line = rememberLineComponent(Fill(colors.panelLine), thickness = 1.dp),
                tick = null,
                guideline = null,
                valueFormatter = remember(spec) { CartesianValueFormatter { _, x, _ -> spec.dayLabel(x.roundToInt()) } },
            ),
            marker = marker,
            decorations = listOfNotNull(band),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(ChartHeight),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/** The quantity's normal range as a tinted band behind the bars ([Decoration.drawUnderLayers]). */
private class NormalBand(
    private val y: ClosedFloatingPointRange<Double>,
    private val box: ShapeComponent,
) : Decoration {
    override fun drawUnderLayers(context: CartesianDrawingContext) {
        with(context) {
            val yRange = ranges.getYRange(null)
            fun canvasY(value: Double) =
                (layerBounds.bottom - (value - yRange.minY) / yRange.length * layerBounds.height).toFloat()
            box.draw(context, layerBounds.left, canvasY(y.endInclusive), layerBounds.right, canvasY(y.start))
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier) {
    Box(modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
        BasicText("No data in this period", style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2))
    }
}

