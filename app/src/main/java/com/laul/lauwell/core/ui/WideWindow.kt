package com.laul.lauwell.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wide enough for the side rail and wide layouts (Android's 600 dp breakpoint): the Fold open (about 884 dp), a phone on its side. */
fun isWideWindow(width: Dp): Boolean = width >= 600.dp

/** Whether the window is wide ([isWideWindow]); set by `LauWellNavHost`, read by screens with a wide layout (Today). */
val LocalWideWindow = staticCompositionLocalOf { false }

/** How wide a phone layout (a screen without a wide one, a bottom sheet) gets on a wide window, centred. */
val PhoneContentMaxWidth: Dp = 600.dp
