package com.laul.lauwell.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Home / Dashboard — unified overview across whichever modules the user has enabled.
 * Placeholder until the Settings module (module enable/disable) exists to drive this.
 */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicText(
            text = "Home / Dashboard",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
        // Temporary entry point until Home gets real navigation (M2).
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = "Settings",
                style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.primary),
            )
        }
    }
}
