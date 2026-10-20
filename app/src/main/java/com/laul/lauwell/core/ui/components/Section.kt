package com.laul.lauwell.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * A section label sitting on the 2dp ink rule, outside any panel (PLAN.md → Visual design → Look):
 * "QUICK LOG", "PINNED", or a category name on the Tracker hub (then with its [category] dot).
 *
 * @param trailing right-aligned content, e.g. the edit-pinned icon or "2/3 taken".
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    category: Category? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LauWellTheme.colors
    val rule = LauWellTheme.strokes.rule
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .drawBehind {
                val y = size.height - rule.toPx() / 2
                drawLine(colors.line, Offset(0f, y), Offset(size.width, y), strokeWidth = rule.toPx())
            }
            .padding(bottom = LauWellTheme.spacing.xs + rule),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs),
    ) {
        if (category != null) CategoryDot(colors.forCategory(category).fg)
        BasicText(
            text = label.uppercase(),
            style = LauWellTheme.typography.label.copy(color = colors.ink),
            maxLines = 1,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** [SectionHeader] followed by its content, the design's standard block on Today and Tracker. */
@Composable
fun Section(
    label: String,
    modifier: Modifier = Modifier,
    category: Category? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs)) {
        SectionHeader(label = label, category = category, trailing = trailing)
        content()
    }
}

/**
 * The light content panel: [LauWellTheme] `panel` fill, 1dp hairline, 12dp corners.
 * Lists use the default padding (rows bring their own height); grids pass a vertical padding too.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = LauWellTheme.spacing.panel),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.panel
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(LauWellTheme.strokes.hairline, colors.panelLine, shape)
            .padding(contentPadding),
        content = content,
    )
}

/** The 1dp divider between rows inside a [Panel]. */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = LauWellTheme.strokes.hairline)
            .background(LauWellTheme.colors.panelLine),
    )
}

/** "Symptoms · Systemic" in the category color, above a sheet or detail-screen title. */
@Composable
fun CategoryLine(category: Category, text: String, modifier: Modifier = Modifier) {
    val fg = LauWellTheme.colors.forCategory(category).fg
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryDot(fg)
        BasicText(
            text = text,
            style = LauWellTheme.typography.caption.copy(color = fg, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun CategoryDot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}
