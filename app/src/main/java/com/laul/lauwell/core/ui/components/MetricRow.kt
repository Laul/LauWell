package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** Where a metric row's data comes from, shown under its value (PLAN.md → App navigation → Tracker). */
enum class MetricSource {
    /** Sensor-fed through Health Connect: a quiet "Synced" line. */
    Synced,

    /** Logged by hand: an outlined "+ Add" badge. */
    Manual,
}

/**
 * The outlined icon tile at the start of a metric row: 36dp, 1.5dp stroke in the category `fg`,
 * no fill.
 */
@Composable
fun MetricIconTile(@DrawableRes icon: Int, category: Category, modifier: Modifier = Modifier) {
    val fg = LauWellTheme.colors.forCategory(category).fg
    val shape = LauWellTheme.shapes.tile
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(shape)
            .border(LauWellTheme.strokes.control, fg, shape),
        contentAlignment = Alignment.Center,
    ) {
        LauWellIcon(icon = icon, contentDescription = null, tint = fg, size = 19.dp)
    }
}

/**
 * One metric in a list (Tracker hub, Today): icon tile, name, context line, Mono value and its
 * [source] badge. Put rows in a [Panel], separated by [PanelDivider].
 *
 * @param category the metric's own category: it keeps that color on every tab.
 * @param value null when there is nothing yet (shows "—", never 0).
 */
@Composable
fun MetricRow(
    @DrawableRes icon: Int,
    name: String,
    context: String,
    value: String?,
    category: Category,
    source: MetricSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = LauWellTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        MetricIconTile(icon = icon, category = category)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(text = name, style = type.item.copy(color = colors.ink), maxLines = 1)
            BasicText(
                text = context,
                style = type.caption.copy(color = colors.ink2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(text = value ?: "—", style = type.metricSmall.copy(color = colors.ink), maxLines = 1)
            when (source) {
                MetricSource.Synced -> SyncedBadge()
                MetricSource.Manual -> AddBadge()
            }
        }
    }
}

@Composable
private fun SyncedBadge() {
    val ink2 = LauWellTheme.colors.ink2
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        LauWellIcon(icon = R.drawable.ic_sync, contentDescription = null, tint = ink2, size = 13.dp)
        BasicText(text = "Synced", style = LauWellTheme.typography.caption.copy(color = ink2))
    }
}

/** Not a button of its own: the whole row opens the entry. */
@Composable
private fun AddBadge() {
    val ink = LauWellTheme.colors.ink
    val shape = LauWellTheme.shapes.badge
    Row(
        modifier = Modifier
            .height(22.dp)
            .border(LauWellTheme.strokes.hairline, ink, shape)
            .padding(start = 5.dp, end = LauWellTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LauWellIcon(icon = R.drawable.ic_add, contentDescription = null, tint = ink, size = 13.dp)
        BasicText(
            text = "Add",
            style = LauWellTheme.typography.caption.copy(color = ink, fontWeight = FontWeight.SemiBold),
        )
    }
}
