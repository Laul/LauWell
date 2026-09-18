package com.laul.lauwell.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * App color tokens. Deliberately not modeled after Material3's ColorScheme — this project isn't
 * committed to Material3 (or any specific design system) yet (see Claude-LauWell/PLAN.md,
 * 2026-07-10). Treat these as throwaway placeholder values until a real palette is chosen.
 */
data class LauWellColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
)

val LightLauWellColors = LauWellColors(
    primary = Color(0xFF3A5A78),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1A1A1A),
)

val DarkLauWellColors = LauWellColors(
    primary = Color(0xFF9CC3E5),
    onPrimary = Color(0xFF00304D),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
)

val LocalLauWellColors = staticCompositionLocalOf { LightLauWellColors }
