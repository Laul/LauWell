package com.laul.lauwell.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Root theme wrapper. No Material3 dependency — see Color.kt/Type.kt for why. Wrap the app (or
 * a preview) in this to get [LauWellTheme] token access below.
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

/** Static accessors, e.g. `LauWellTheme.colors.primary` / `LauWellTheme.typography.title`. */
object LauWellTheme {
    val colors: LauWellColors
        @Composable get() = LocalLauWellColors.current

    val typography: LauWellTypography
        @Composable get() = LocalLauWellTypography.current
}
