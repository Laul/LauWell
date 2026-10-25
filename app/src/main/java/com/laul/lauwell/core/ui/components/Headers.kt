package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * The header of the four tabs (design 20a): the title — on Today, the date [eyebrow] over the
 * greeting — then search and the avatar on the right, and an optional [toolbar] row under it
 * (Tracker). It stays put above the tab's scrolling content. Search is drawn disabled while
 * [onSearch] is null: global search isn't built yet.
 */
@Composable
fun AppHeader(
    title: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onSearch: (() -> Unit)? = null,
    toolbar: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val spacing = LauWellTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = spacing.lg, end = spacing.lg, top = spacing.xs, bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (eyebrow != null) {
                    BasicText(text = eyebrow.uppercase(), style = type.label.copy(color = colors.ink2), maxLines = 1)
                }
                BasicText(
                    text = title,
                    style = (if (eyebrow != null) type.greeting else type.headline).copy(color = colors.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            IconAction(R.drawable.ic_search, "Search", onClick = onSearch ?: {}, enabled = onSearch != null)
            Avatar(onClick = onAvatarClick)
        }
        if (toolbar != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                content = toolbar,
            )
        }
    }
}

/**
 * The account avatar: an ink disc, 36dp in a 48dp touch target. The design shows the first
 * name's initial; a person glyph stands in until the app knows the name (Notion: *Decide Today
 * greeting name*).
 */
@Composable
private fun Avatar(onClick: () -> Unit) {
    val colors = LauWellTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            LauWellIcon(R.drawable.ic_person, contentDescription = "Settings", tint = colors.onAccent, size = 20.dp)
        }
    }
}

/** The 48dp bar of a pushed screen: back arrow and title (Steps detail, Settings). */
@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xs),
    ) {
        IconAction(R.drawable.ic_arrow_back, "Back", onBack)
        BasicText(
            text = title,
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.ink),
            maxLines = 1,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** A 24dp icon in a 48dp touch target; at [DisabledAlpha] and not clickable when not [enabled]. */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LauWellTheme.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(LauWellTheme.shapes.control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LauWellIcon(icon = icon, contentDescription = contentDescription, tint = colors.ink)
    }
}

/** The standard scrolling body of a screen: 20dp side margins, blocks 20dp apart. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = LauWellTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.lg, end = spacing.lg, top = spacing.xs, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        content = content,
    )
}
