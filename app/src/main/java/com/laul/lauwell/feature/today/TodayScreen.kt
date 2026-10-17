package com.laul.lauwell.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laul.lauwell.R
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Today, the home tab. For now the header only: date, greeting and the avatar that opens Settings.
 * Quick log and pinned tiles come with the Today lists task (M2); medications, the symptom check
 * and upcoming appointments with M5–M6.
 */
@Composable
fun TodayScreen(onOpenSettings: () -> Unit) {
    val now = remember { LocalDateTime.now() }
    val colors = LauWellTheme.colors

    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
                BasicText(
                    text = now.format(DateFormat).uppercase(),
                    style = LauWellTheme.typography.label.copy(color = colors.ink2),
                )
                BasicText(
                    text = greeting(now.hour),
                    style = LauWellTheme.typography.headline.copy(color = colors.ink),
                    modifier = Modifier.semantics { heading() },
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(LauWellTheme.shapes.tile)
                    .background(colors.surface2)
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                LauWellIcon(R.drawable.ic_person, contentDescription = "Settings", tint = colors.ink, size = 22.dp)
            }
        }
        BasicText(
            text = "Quick log and pinned metrics will appear here.",
            style = LauWellTheme.typography.body.copy(color = colors.ink2),
        )
    }
}

private val DateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")

internal fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
