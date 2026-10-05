package com.laul.lauwell.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Settings and Notifications — module enable/disable, units/thresholds, shared reminder engine.
 * This is the screen that will eventually decide which feature screens Home surfaces.
 * For now: sign out.
 */
@Composable
fun SettingsScreen() {
    val viewModel = appViewModel { SettingsViewModel(authRepository) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicText(
            text = "Settings",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = viewModel::signOut),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = "Sign out",
                style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.primary),
            )
        }
    }
}
