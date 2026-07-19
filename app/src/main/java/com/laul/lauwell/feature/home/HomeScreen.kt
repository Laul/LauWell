package com.laul.lauwell.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Home / Dashboard — unified overview across whichever modules the user has enabled.
 * Placeholder until the Settings module (module enable/disable) exists to drive this.
 */
@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Home / Dashboard",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
