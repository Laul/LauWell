package com.laul.lauwell.core.ui.components

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.laul.lauwell.core.ui.theme.LauWellTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Scrim = Color.Black.copy(alpha = 0.42f)

/** Dragged down past this share of its height, the sheet closes on release. */
private const val DismissFraction = 0.3f

/** A downward fling at least this fast closes the sheet, however short the drag. */
private val DismissVelocity = 900.dp

/**
 * The quick-log bottom sheet (PLAN.md → App navigation → Today: one tap on a chip opens it).
 * Page-colored, 14dp top corners with a 2dp ink edge, a handle, then [content] stacked with 20dp
 * gaps. Tapping the scrim, Back, or dragging it down calls [onDismissRequest].
 *
 * It opens in its own window over the whole screen, bottom bar included, as the design draws it;
 * call it from anywhere. The keyboard pushes it up, so a note field stays in view.
 */
@Composable
fun LauWellBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    // Stays composed until the exit animation has finished.
    if (!state.currentState && !state.targetState && state.isIdle) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // The window draws edge to edge and resizes for the keyboard; the scrim below replaces its dim.
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            window.setDimAmount(0f)
            @Suppress("DEPRECATION") // Still how a dialog window opts into keyboard resizing.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visibleState = state, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
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
                visibleState = state,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                DraggableSheet(onDismissRequest, content)
            }
        }
    }
}

/** Follows the finger downward (never up); closes past [DismissFraction] or on a fast fling, else springs back. */
@Composable
private fun DraggableSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var height by remember { mutableIntStateOf(0) }
    val dismissVelocity = with(LocalDensity.current) { DismissVelocity.toPx() }
    SheetSurface(
        modifier = Modifier
            .onSizeChanged { height = it.height }
            .offset { IntOffset(0, offset.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { offset.snapTo((offset.value + delta).coerceAtLeast(0f)) }
                },
                onDragStopped = { velocity ->
                    if (offset.value > height * DismissFraction || velocity > dismissVelocity) onDismissRequest()
                    else offset.animateTo(0f)
                },
            ),
        content = content,
    )
}

/** The sheet itself, without its window: also what previews draw. */
@Composable
internal fun SheetSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LauWellTheme.colors
    val shape = LauWellTheme.shapes.sheet
    val spacing = LauWellTheme.spacing
    // The ink edge: an ink layer showing above the page-colored one, thinning out along the
    // corners like the design's CSS border-top.
    Box(
        modifier
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
                // Above the navigation bar, or above the keyboard when it is open (whichever is taller).
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
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
