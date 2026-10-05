package com.laul.lauwell.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * App text style tokens — a small custom set, not Material3's Typography scale. Kept independent
 * so adopting a real design system later doesn't mean unwinding Material assumptions everywhere.
 * LauWell's accessibility principle (large targets, clear text) should drive these sizes once a
 * real screen ships.
 */
data class LauWellTypography(
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
)

val DefaultLauWellTypography = LauWellTypography(
    title = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    label = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

val LocalLauWellTypography = staticCompositionLocalOf { DefaultLauWellTypography }
