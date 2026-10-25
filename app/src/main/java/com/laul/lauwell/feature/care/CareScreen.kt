package com.laul.lauwell.feature.care

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.components.AppHeader
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** The Care tab. Placeholder under the shared header until it is designed in the locked system (Notion: M6). */
@Composable
fun CareScreen(onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "Care", onOpenSettings = onOpenSettings)
        ScreenColumn(Modifier.weight(1f)) {
            BasicText(
                text = "Medications, the ostomy module (products, changes, wear time and skin) and appointments. Not designed yet.",
                style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
            )
        }
    }
}
