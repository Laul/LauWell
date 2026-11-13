package com.laul.lauwell.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * The bottom bar's place on a wide window (the Fold open, or a phone on its side): the four tabs down
 * the left edge (design round 18: 80 dp wide, 72 × 62 items, 24 dp icons). Same tabs and behaviour as
 * [BottomNavBar]; search and the gear stay in each screen's header (Notion: *Header + gear on the Fold
 * inner screen*). Pads for the status bar, and for a navigation bar or cutout on its side.
 */
@Composable
fun NavRail(
    selected: TopLevelTab?,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(LauWellTheme.colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .width(80.dp)
            .padding(top = 14.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TopLevelTab.entries.forEach { tab ->
            TabItem(
                tab = tab,
                active = tab == selected,
                onSelect = onSelect,
                iconSize = 24.dp,
                modifier = Modifier.size(width = 72.dp, height = 62.dp),
            )
        }
    }
}
