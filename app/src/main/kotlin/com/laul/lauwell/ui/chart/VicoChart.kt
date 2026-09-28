package com.laul.lauwell.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.charts.ChartSpec
import com.laul.lauwell.core.charts.Series
import com.laul.lauwell.core.charts.XAxis
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.candlestickModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.compose.cartesian.layer.CandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.absolute
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * The only file that knows about Vico. It turns a library-neutral [ChartSpec] into Vico layers
 * and models. A Vico upgrade or a library swap should touch nothing else.
 *
 * Range bars are drawn as candlesticks with opening = closing = average: the wick spans min to
 * max and the flat body marks the average.
 */
@Composable
fun MetricChart(spec: ChartSpec, modifier: Modifier = Modifier, compact: Boolean = false) {
    val data = spec.series
    if (data.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(spec) {
        producer.runTransaction {
            when (data) {
                is Series.RangeBars -> candlestickModel(
                    x = data.bars.map { it.x },
                    opening = data.bars.map { it.mid },
                    closing = data.bars.map { it.mid },
                    low = data.bars.map { it.low },
                    high = data.bars.map { it.high },
                )
                is Series.Bars -> columnModel { series(x = data.bars.map { it.x }, y = data.bars.map { it.value }) }
                is Series.Line -> lineModel { series(x = data.points.map { it.x }, y = data.points.map { it.y }) }
            }
        }
    }

    val color = MaterialTheme.colorScheme.primary
    val rangeProvider = remember(spec.yRange) {
        spec.yRange?.let { CartesianLayerRangeProvider.fixed(minY = it.start, maxY = it.endInclusive) }
            ?: CartesianLayerRangeProvider.auto()
    }

    val layer = key(data::class) {
        when (data) {
            is Series.RangeBars -> {
                val candle = CandlestickCartesianLayer.Candle(
                    body = rememberLineComponent(Fill(color), thickness = 10.dp),
                    topWick = rememberLineComponent(Fill(color.copy(alpha = 0.6f)), thickness = 4.dp),
                )
                rememberCandlestickCartesianLayer(
                    candleProvider = CandlestickCartesianLayer.CandleProvider.absolute(candle, candle, candle),
                    rangeProvider = rangeProvider,
                )
            }
            is Series.Bars -> rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(Fill(color), thickness = 10.dp, shape = RoundedCornerShape(2.dp)),
                ),
                rangeProvider = rangeProvider,
            )
            is Series.Line -> rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(LineCartesianLayer.LineFill.single(Fill(color))),
                ),
                rangeProvider = rangeProvider,
            )
        }
    }

    // Target band behind the data, e.g. glucose 4–9 mmol/L.
    val bandFill = rememberShapeComponent(Fill(color.copy(alpha = 0.10f)))
    val decorations = remember(spec.targetBand, spec.yRange, bandFill) {
        val band = spec.targetBand
        val y = spec.yRange
        if (band == null || y == null) {
            emptyList()
        } else {
            listOf(HorizontalBox(y = { (band.low ?: y.start)..(band.high ?: y.endInclusive) }, box = bandFill))
        }
    }

    val isTimeline = data is Series.Line
    val xStep = if (isTimeline) STEPS_PER_HOUR else 1.0
    val chart = rememberCartesianChart(
        layer,
        startAxis = if (compact) null else VerticalAxis.rememberStart(
            valueFormatter = remember(spec.metric) { CartesianValueFormatter.decimal(decimalCount = spec.metric.decimals) },
        ),
        bottomAxis = if (compact) null else HorizontalAxis.rememberBottom(
            valueFormatter = remember(spec.xAxis) { CartesianValueFormatter { _, x, _ -> label(spec.xAxis, x) } },
            itemPlacer = remember(isTimeline) {
                HorizontalAxis.ItemPlacer.aligned(spacing = { if (isTimeline) LABEL_EVERY_HOURS else 1 })
            },
        ),
        decorations = decorations,
        // Explicit step: Vico's default is the GCD of x deltas, which breaks on irregular timestamps.
        getXStep = { _, _, _ -> xStep },
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(
            scrollEnabled = !compact && isTimeline,
            initialScroll = Scroll.Absolute.End,
        ),
        zoomState = rememberVicoZoomState(zoomEnabled = !compact),
    )
}

private fun Series.isEmpty(): Boolean = when (this) {
    is Series.RangeBars -> bars.isEmpty()
    is Series.Bars -> bars.isEmpty()
    is Series.Line -> points.isEmpty()
}

private fun label(axis: XAxis, x: Double): String =
    if (axis.unit == Duration.ofDays(1)) {
        axis.dateAt(x).format(DAY)
    } else {
        axis.timeAt(x).atZone(axis.zone).format(DAY_TIME)
    }

private const val STEPS_PER_HOUR = 60.0 // timeline x is in minutes
private const val LABEL_EVERY_HOURS = 6
private val DAY = DateTimeFormatter.ofPattern("EEE")
private val DAY_TIME = DateTimeFormatter.ofPattern("EEE HH:mm")
