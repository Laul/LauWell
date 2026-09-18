package com.laul.lauwell.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Settings and Notifications — module enable/disable, units/thresholds, shared reminder engine.
 * This is the screen that will eventually decide which feature screens Home surfaces.
 */
@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Settings",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
