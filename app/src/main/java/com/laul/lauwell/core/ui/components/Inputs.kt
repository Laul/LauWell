package com.laul.lauwell.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Range selector on a detail screen (Week / Month / 3 months): one 1.5dp ink outline, the selected
 * segment filled with ink.
 */
@Composable
fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.control
    val stroke = LauWellTheme.strokes.control
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .border(stroke, colors.ink, shape)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) Box(Modifier.width(stroke).fillMaxHeight().background(colors.ink))
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(if (selected) colors.ink else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = option,
                    style = LauWellTheme.typography.body.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.bg else colors.ink2,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The 1–10 rating: ten outlined pips in two rows of five, the chosen one filled in the category
 * `fg`, with labelled ends ("1 · Rested", "10 · Exhausted").
 *
 * @param value the chosen pip, or null before the first tap.
 */
@Composable
fun PipRating(
    value: Int?,
    onValueChange: (Int) -> Unit,
    category: Category,
    lowLabel: String,
    highLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val categoryColor = colors.forCategory(category)
    val shape = LauWellTheme.shapes.control
    val gap = LauWellTheme.spacing.xs
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            (1..10).chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { n ->
                        val selected = n == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(shape)
                                .background(if (selected) categoryColor.fg else Color.Transparent)
                                .border(
                                    LauWellTheme.strokes.control,
                                    if (selected) categoryColor.fg else categoryColor.stroke,
                                    shape,
                                )
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { onValueChange(n) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = n.toString(),
                                style = LauWellTheme.typography.metric.copy(
                                    fontSize = 18.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) colors.bg else colors.ink,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(text = lowLabel, style = LauWellTheme.typography.caption.copy(color = colors.ink2))
            BasicText(text = highLabel, style = LauWellTheme.typography.caption.copy(color = colors.ink2))
        }
    }
}
