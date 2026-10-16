package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * One cell of Today's pinned grid (3 × 2 inside a [Panel]): icon and name, Mono value with unit,
 * a goal bar when there is a goal, and a context line ("of 8,000", "3 days ago").
 *
 * @param value null when there is nothing yet (shows "—", never 0).
 * @param progress share of the goal reached, 0..1; null when the metric has no goal (the bar's
 *   space is kept so the tiles line up).
 */
@Composable
fun PinnedTile(
    @DrawableRes icon: Int,
    name: String,
    value: String?,
    unit: String,
    context: String,
    category: Category,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val fg = colors.forCategory(category).fg
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LauWellIcon(icon = icon, contentDescription = null, tint = fg, size = 16.dp)
            BasicText(text = name, style = type.caption.copy(color = colors.ink2), maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = value ?: "—",
                style = type.metric.copy(color = colors.ink, lineHeight = type.metric.fontSize),
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                BasicText(text = unit, style = type.caption.copy(color = colors.ink2), maxLines = 1)
            }
        }
        GoalBar(progress = progress, color = fg)
        BasicText(text = context, style = type.caption.copy(color = colors.ink2), maxLines = 1)
    }
}

@Composable
private fun GoalBar(progress: Float?, color: Color) {
    val shape = LauWellTheme.shapes.bar
    if (progress == null) {
        Box(Modifier.fillMaxWidth().height(4.dp))
        return
    }
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(LauWellTheme.colors.trackBg)
            .clearAndSetSemantics { contentDescription = "$percent% of goal" },
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color),
        )
    }
}
