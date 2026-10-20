package com.laul.lauwell.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Root theme wrapper. No Material3 dependency — see Color.kt/Type.kt for why. Wrap the app (or
 * a preview) in this to get [LauWellTheme] token access below. Follows the system light/dark setting.
 */
@Composable
fun LauWellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkLauWellColors else LightLauWellColors
    CompositionLocalProvider(
        LocalLauWellColors provides colors,
        LocalLauWellTypography provides DefaultLauWellTypography,
        content = content,
    )
}

/**
 * Static accessors, e.g. `LauWellTheme.colors.ink`, `LauWellTheme.typography.metric`,
 * `LauWellTheme.colors.forCategory(category).fg`, `LauWellTheme.spacing.md`.
 */
object LauWellTheme {
    val colors: LauWellColors
        @Composable get() = LocalLauWellColors.current

    val typography: LauWellTypography
        @Composable get() = LocalLauWellTypography.current

    val shapes: LauWellShapes
        @Composable get() = LocalLauWellShapes.current

    val strokes: LauWellStrokes
        @Composable get() = LocalLauWellStrokes.current

    val spacing: LauWellSpacing
        @Composable get() = LocalLauWellSpacing.current
}
