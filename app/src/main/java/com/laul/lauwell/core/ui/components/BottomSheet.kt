package com.laul.lauwell.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.ui.theme.LauWellTheme

private val Scrim = Color.Black.copy(alpha = 0.42f)

/**
 * The quick-log bottom sheet (PLAN.md → App navigation → Today: one tap on a chip opens it).
 * Page-colored, 14dp top corners with a 2dp ink edge, a handle, then [content] stacked with 20dp
 * gaps. Tapping the scrim or Back calls [onDismissRequest].
 *
 * Drawn in the caller's layout, not in a window: place it last in a full-screen [Box] so it
 * covers the screen. No drag-to-dismiss yet.
 */
@Composable
fun BoxScope.LauWellBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismissRequest)
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .semantics { contentDescription = "Close" }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        SheetSurface(content)
    }
}

@Composable
private fun SheetSurface(content: @Composable ColumnScope.() -> Unit) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.sheet
    val spacing = LauWellTheme.spacing
    // The ink edge: an ink layer showing above the page-colored one, thinning out along the
    // corners like the design's CSS border-top.
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.ink)
            .padding(top = LauWellTheme.strokes.rule)
            .clip(shape)
            .background(colors.bg)
            // Swallow taps so they don't reach the scrim.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = spacing.lg, end = spacing.lg, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .alpha(0.8f)
                    .clip(LauWellTheme.shapes.bar)
                    .background(colors.line),
            )
            content()
        }
    }
}
