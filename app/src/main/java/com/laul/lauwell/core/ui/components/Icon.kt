package com.laul.lauwell.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A Material Symbols glyph bundled as a vector drawable (`res/drawable/ic_*.xml`, FILL 0 / wght 400;
 * `_filled` variants are FILL 1 / wght 500 for the active tab). Drawn black and tinted here.
 *
 * @param contentDescription null when the icon only decorates a labelled element.
 */
@Composable
fun LauWellIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
