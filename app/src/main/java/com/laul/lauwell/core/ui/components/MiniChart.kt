package com.laul.lauwell.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** Days in a mini-chart: the last seven, oldest first, today last. */
const val MiniChartDays = 7

/**
 * What a [MiniChart] draws. One entry per day, oldest first; null = no data that day, drawn as a
 * faint placeholder and never as zero.
 */
sealed interface MiniChartData {

    data class Bars(val days: List<Double?>, val max: Double, val goal: Double? = null) : MiniChartData

    data class Line(val days: List<Double?>, val min: Double, val max: Double) : MiniChartData

    /** Blood pressure: each day's low–high span (diastolic..systolic). */
    data class Range(val days: List<ClosedFloatingPointRange<Double>?>, val min: Double, val max: Double) : MiniChartData

    /** 0 = none (hollow), 1 until [levels] − 1 = increasing severity. */
    data class SeverityDots(val days: List<Int?>, val levels: Int) : MiniChartData

    /** The whole cycle rather than seven days. [day] is today's cycle day, null when unknown. */
    data class CycleStrip(
        val day: Int?,
        val length: Int = 28,
        val period: IntRange = IntRange.EMPTY,
        val fertile: IntRange = IntRange.EMPTY,
    ) : MiniChartData

    companion object {
        /** Nothing recorded yet: every day a placeholder. */
        fun empty(kind: MiniChartKind): MiniChartData {
            val none = List(MiniChartDays) { null }
            return when (kind) {
                MiniChartKind.Bars -> Bars(none, max = 1.0)
                MiniChartKind.Line -> Line(none, min = 0.0, max = 1.0)
                MiniChartKind.Range -> Range(none, min = 0.0, max = 1.0)
                MiniChartKind.SeverityDots -> SeverityDots(none, levels = 4)
                MiniChartKind.CycleStrip -> CycleStrip(day = null)
            }
        }
    }
}

/**
 * The 7-day chart on a Tracker row (56 × 24dp) or widget (full width × 34dp), in the category
 * [color]. The latest day is drawn in full color, earlier days lighter (`docs/design/UI-spec.md`
 * → Mini-chart rules). Our own drawing, not Vico: Vico stays behind `ChartSpec` for detail charts.
 * Decorative: the row's value and takeaway carry the meaning for accessibility.
 */
@Composable
fun MiniChart(data: MiniChartData, color: Color, modifier: Modifier = Modifier) {
    val colors = LauWellTheme.colors
    val style = MiniChartStyle(fg = color, ink = colors.ink, ink2 = colors.ink2, panel = colors.panel, lineSoft = colors.lineSoft)
    Canvas(modifier) {
        when (data) {
            is MiniChartData.Bars -> drawBars(data, style)
            is MiniChartData.Line -> drawLine(data, style)
            is MiniChartData.Range -> drawRange(data, style)
            is MiniChartData.SeverityDots -> drawDots(data, style)
            is MiniChartData.CycleStrip -> drawCycle(data, style)
        }
    }
}

private class MiniChartStyle(val fg: Color, val ink: Color, val ink2: Color, val panel: Color, val lineSoft: Color)

private const val EarlierDayAlpha = 0.42f
private const val PlaceholderAlpha = 0.45f

private fun DrawScope.slot(count: Int) = size.width / count
private fun DrawScope.centerX(index: Int, count: Int) = index * slot(count) + slot(count) / 2
private fun <T> List<T?>.lastPresent(): Int = indexOfLast { it != null }

/** Value → y inside a [pad] margin, as in the design (`yOf`). */
private fun DrawScope.yOf(value: Double, min: Double, max: Double): Float {
    val pad = 3.dp.toPx()
    val fraction = if (max > min) ((value - min) / (max - min)).toFloat() else 0.5f
    return pad + (1f - fraction.coerceIn(0f, 1f)) * (size.height - 2 * pad)
}

/** The faint dot that stands for a day without data (lines, ranges, dots). */
private fun DrawScope.placeholderDot(index: Int, count: Int, style: MiniChartStyle) {
    drawCircle(style.ink2.copy(alpha = PlaceholderAlpha), 1.3.dp.toPx(), Offset(centerX(index, count), size.height - 2.dp.toPx()))
}

private fun DrawScope.drawBars(data: MiniChartData.Bars, style: MiniChartStyle) {
    val count = data.days.size
    val last = data.days.lastPresent()
    val usable = size.height - 2.dp.toPx()
    if (data.goal != null && data.max > 0) {
        val y = size.height - (data.goal / data.max).toFloat().coerceIn(0f, 1f) * usable
        drawLine(
            color = style.ink2.copy(alpha = 0.6f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())),
        )
    }
    data.days.forEachIndexed { i, value ->
        val cx = centerX(i, count)
        if (value == null) {
            val w = slot(count) * 0.56f
            drawRoundRect(
                color = style.ink2.copy(alpha = PlaceholderAlpha),
                topLeft = Offset(cx - w / 2, size.height - 3.dp.toPx()),
                size = Size(w, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx()),
                style = Stroke(1.2.dp.toPx()),
            )
            return@forEachIndexed
        }
        val w = slot(count) * 0.6f
        val h = maxOf(2.dp.toPx(), (value / data.max).toFloat().coerceIn(0f, 1f) * usable)
        drawRoundRect(
            color = style.fg.copy(alpha = if (i == last) 1f else EarlierDayAlpha),
            topLeft = Offset(cx - w / 2, size.height - h),
            size = Size(w, h),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
    }
}

private fun DrawScope.drawLine(data: MiniChartData.Line, style: MiniChartStyle) {
    val count = data.days.size
    val points = data.days.mapIndexedNotNull { i, v -> v?.let { Offset(centerX(i, count), yOf(it, data.min, data.max)) } }
    if (points.isEmpty()) {
        repeat(count) { placeholderDot(it, count, style) }
        return
    }
    val line = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val area = Path().apply {
        addPath(line)
        lineTo(points.last().x, size.height)
        lineTo(points.first().x, size.height)
        close()
    }
    drawPath(area, style.fg.copy(alpha = 0.12f))
    drawPath(line, style.fg, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    val end = points.last()
    drawCircle(style.panel, 2.8.dp.toPx() + 0.75.dp.toPx(), end)
    drawCircle(style.fg, 2.8.dp.toPx(), end)
}

private fun DrawScope.drawRange(data: MiniChartData.Range, style: MiniChartStyle) {
    val count = data.days.size
    val last = data.days.lastPresent()
    data.days.forEachIndexed { i, span ->
        if (span == null) {
            placeholderDot(i, count, style)
            return@forEachIndexed
        }
        val x = centerX(i, count)
        drawLine(
            color = style.fg.copy(alpha = if (i == last) 1f else EarlierDayAlpha),
            start = Offset(x, yOf(span.endInclusive, data.min, data.max)),
            end = Offset(x, yOf(span.start, data.min, data.max)),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawDots(data: MiniChartData.SeverityDots, style: MiniChartStyle) {
    val count = data.days.size
    val last = data.days.lastPresent()
    val pad = 3.dp.toPx()
    val top = (data.levels - 1).coerceAtLeast(1)
    fun yLevel(level: Int) = pad + 2.dp.toPx() + (1f - level.toFloat() / top) * (size.height - 2 * pad - 4.dp.toPx())
    data.days.forEachIndexed { i, level ->
        val x = centerX(i, count)
        when {
            level == null -> placeholderDot(i, count, style)
            level <= 0 -> drawCircle(style.ink2.copy(alpha = 0.7f), 2.4.dp.toPx(), Offset(x, yLevel(0)), style = Stroke(1.2.dp.toPx()))
            else -> drawCircle(style.fg.copy(alpha = if (i == last) 1f else 0.5f), 3.2.dp.toPx(), Offset(x, yLevel(level)))
        }
    }
}

private fun DrawScope.drawCycle(data: MiniChartData.CycleStrip, style: MiniChartStyle) {
    val step = size.width / data.length
    val w = step * 0.56f
    for (d in 1..data.length) {
        val today = d == data.day
        val h = if (today) size.height else size.height * 0.5f
        val color = when {
            today -> style.ink
            d in data.period -> style.fg
            d in data.fertile -> style.fg.copy(alpha = 0.38f)
            else -> style.lineSoft
        }
        drawRoundRect(
            color = color,
            topLeft = Offset((d - 1) * step + (step - w) / 2, (size.height - h) / 2),
            size = Size(w, h),
            cornerRadius = CornerRadius(w / 2),
        )
    }
}
