package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

/** One segment of an [IconToggle]. */
data class IconToggleOption(@DrawableRes val icon: Int, val label: String)

/**
 * A compact icon-only switch between views, e.g. Tracker's list | widgets: a `surface2` track, the
 * selected segment raised on `surface` with an ink icon (a 1dp shadow in light mode only), the
 * others in ink2. 34 × 28dp segments, as designed. [IconToggleOption.label] is read by TalkBack.
 */
@Composable
fun IconToggle(
    options: List<IconToggleOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val lightMode = colors.bg.luminance() > 0.5f
    Row(
        modifier = modifier
            .clip(LauWellTheme.shapes.toggleTrack)
            .background(colors.surface2)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val shape = LauWellTheme.shapes.control
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 28.dp)
                    .then(if (selected && lightMode) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (selected) colors.surface else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                LauWellIcon(
                    icon = option.icon,
                    contentDescription = option.label,
                    tint = if (selected) colors.ink else colors.ink2,
                    size = 19.dp,
                )
            }
        }
    }
}

/**
 * A labelled segmented control filling its row (Settings › Appearance, design 20b): the same
 * `surface2` track as [IconToggle], equal 36dp segments with an icon and a label, the selected one
 * on `surface`.
 */
@Composable
fun SegmentedControl(
    options: List<IconToggleOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val lightMode = colors.bg.luminance() > 0.5f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LauWellTheme.shapes.toggleTrack)
            .background(colors.surface2)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val shape = LauWellTheme.shapes.control
            val tint = if (selected) colors.ink else colors.ink2
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(if (selected && lightMode) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (selected) colors.surface else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            ) {
                LauWellIcon(icon = option.icon, contentDescription = null, tint = tint, size = 17.dp)
                BasicText(text = option.label, style = LauWellTheme.typography.control.copy(color = tint), maxLines = 1)
            }
        }
    }
}

/**
 * An on/off switch as designed (Health Connect grant rows): a 44 × 26dp track, ink when on and
 * `surface2` when off, with a 20dp knob. Drawing only: make the whole row `toggleable`, so the
 * touch target is the row and TalkBack reads it as one control.
 */
@Composable
fun Switch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LauWellTheme.colors
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) colors.accent else colors.surface2)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(colors.onAccent))
    }
}
