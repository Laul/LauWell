package com.laul.lauwell.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner shapes from the locked design: panels 12dp; chips, controls and icon tiles 8dp. */
@Immutable
data class LauWellShapes(
    val panel: RoundedCornerShape = RoundedCornerShape(12.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** Buttons, segmented selector, text fields. */
    val control: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** The outlined icon tile at the start of a metric row. */
    val tile: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** The small "+ Add" badge on a manual metric row. */
    val badge: RoundedCornerShape = RoundedCornerShape(6.dp),
    /** Top corners of a bottom sheet. */
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    /** Progress tracks and the sheet handle. */
    val bar: RoundedCornerShape = RoundedCornerShape(2.dp),
)

/** Border widths from the locked design. */
@Immutable
data class LauWellStrokes(
    /** Panel outline and in-panel dividers ([LauWellColors.panelLine]). */
    val hairline: Dp = 1.dp,
    /** Stroke buttons, chips, icon tiles, rating pips. */
    val control: Dp = 1.5.dp,
    /** The ink rule under a section label ([LauWellColors.line]). */
    val rule: Dp = 2.dp,
)

/** Spacing scale: 4 / 8 / 12 / 16 / 20 / 24 / 32 dp. */
@Immutable
data class LauWellSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Inner padding of a panel; the design's one value off the scale. */
    val panel: Dp = 14.dp,
)

val LocalLauWellShapes = staticCompositionLocalOf { LauWellShapes() }
val LocalLauWellStrokes = staticCompositionLocalOf { LauWellStrokes() }
val LocalLauWellSpacing = staticCompositionLocalOf { LauWellSpacing() }
