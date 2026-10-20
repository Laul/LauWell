package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** A tab's title row ("Tracker"), with optional actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        BasicText(
            text = title,
            style = LauWellTheme.typography.headline.copy(color = LauWellTheme.colors.ink),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        trailing?.invoke(this)
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

/** A 24dp icon in a 48dp touch target. */
@Composable
fun IconAction(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(LauWellTheme.shapes.control)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LauWellIcon(icon = icon, contentDescription = contentDescription, tint = LauWellTheme.colors.ink)
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
