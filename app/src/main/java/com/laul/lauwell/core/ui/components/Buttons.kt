package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** How every disabled control is drawn: at half opacity. */
internal const val DisabledAlpha = 0.5f

/**
 * The filled ink button: only for a screen's main action (Save, Allow, Continue with Google).
 * Full width, 52dp. [content] can replace the plain [text], e.g. for "Save · 6/10" with a Mono value.
 */
@Composable
fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.control
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(shape)
            .background(colors.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = LauWellTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        content?.invoke() ?: BasicText(text = text, style = buttonText(16).copy(color = colors.onAccent))
    }
}

/** Every other action: a 1.5dp ink outline, 36dp tall, sized to its label. */
@Composable
fun StrokeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.control
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(shape)
            .border(LauWellTheme.strokes.control, colors.ink, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = LauWellTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = buttonText(14).copy(color = colors.ink))
    }
}

/**
 * A toolbar control (Tracker's Filter and Sort, design 20a): 34dp tall, a 1.5dp `lineSoft` outline,
 * an icon and an optional [label] ("All metrics"); square when it has no label. Disabled while
 * [onClick] is null.
 */
@Composable
fun ToolbarButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.control
    Row(
        modifier = modifier
            .height(34.dp)
            .then(if (label == null) Modifier.width(34.dp) else Modifier)
            .alpha(if (onClick != null) 1f else DisabledAlpha)
            .clip(shape)
            .border(LauWellTheme.strokes.control, colors.lineSoft, shape)
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() })
            .padding(start = if (label == null) 0.dp else 8.dp, end = if (label == null) 0.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs, Alignment.CenterHorizontally),
    ) {
        LauWellIcon(icon = icon, contentDescription = contentDescription, tint = colors.ink, size = if (label == null) 18.dp else 17.dp)
        if (label != null) BasicText(text = label, style = LauWellTheme.typography.control.copy(color = colors.ink), maxLines = 1)
    }
}

/** Text only, for the dismissive action under a [FilledButton] ("Not now"). 48dp touch target. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(LauWellTheme.shapes.control)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = buttonText(15).copy(color = LauWellTheme.colors.ink))
    }
}

/**
 * A Today quick-log shortcut: outlined in the category's mid-tone `stroke`, icon in its `fg`,
 * label in ink. Opens the entry sheet.
 */
@Composable
fun QuickLogChip(
    @DrawableRes icon: Int,
    label: String,
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val categoryColor = colors.forCategory(category)
    val shape = LauWellTheme.shapes.chip
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .border(LauWellTheme.strokes.control, categoryColor.stroke, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 11.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LauWellIcon(icon = icon, contentDescription = null, tint = categoryColor.fg, size = 19.dp)
        BasicText(
            text = label,
            style = LauWellTheme.typography.item.copy(color = colors.ink, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

@Composable
private fun buttonText(size: Int) =
    LauWellTheme.typography.body.copy(fontSize = size.sp, fontWeight = FontWeight.SemiBold)
