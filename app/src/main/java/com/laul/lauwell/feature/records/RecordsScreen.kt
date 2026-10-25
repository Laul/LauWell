package com.laul.lauwell.feature.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.components.AppHeader
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** The Records tab. Placeholder under the shared header until it is designed in the locked system (Notion: M6). */
@Composable
fun RecordsScreen(onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Records", onAvatarClick = onOpenSettings)
        ScreenColumn(Modifier.weight(1f)) {
            BasicText(
                text = "Labs, documents, visit notes and medical history. Not designed yet.",
                style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
            )
        }
    }
}
