package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * A row in a [Panel] that leads somewhere (Settings, design 20b): an `ink2` icon, a label over a
 * one-line [detail], and a chevron. Without [onClick] it is information only (no chevron); when not
 * [enabled] it is drawn at [DisabledAlpha], for rows whose screen doesn't exist yet. Separate rows
 * with [PanelDivider].
 */
@Composable
fun LinkRow(
    @DrawableRes icon: Int,
    label: String,
    detail: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
    ) {
        LauWellIcon(icon = icon, contentDescription = null, tint = colors.ink2, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(text = label, style = type.item.copy(color = colors.ink), maxLines = 1)
            BasicText(
                text = detail,
                style = type.caption.copy(color = colors.ink2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) LauWellIcon(R.drawable.ic_chevron_right, contentDescription = null, tint = colors.ink2, size = 18.dp)
    }
}
