package com.laul.lauwell.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * The bottom bar as designed: on the page background with a soft top edge; the active tab is a
 * filled icon and a bold ink label, no pill.
 */
@Composable
fun BottomNavBar(
    selected: TopLevelTab?,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauWellTheme.colors
    val hairline = LauWellTheme.strokes.hairline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .drawBehind {
                drawLine(colors.lineSoft, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = hairline.toPx())
            }
            .navigationBarsPadding()
            .height(68.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelTab.entries.forEach { tab ->
            val active = tab == selected
            val color = if (active) colors.ink else colors.ink2
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs, Alignment.CenterVertically),
            ) {
                Box(Modifier.size(width = 60.dp, height = 30.dp), contentAlignment = Alignment.Center) {
                    LauWellIcon(
                        icon = if (active) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = color,
                        size = 22.dp,
                    )
                }
                BasicText(
                    text = tab.label,
                    style = LauWellTheme.typography.caption.copy(
                        color = color,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
