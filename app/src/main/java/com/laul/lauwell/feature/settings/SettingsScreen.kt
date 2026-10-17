package com.laul.lauwell.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.StrokeButton
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Settings, opened from the avatar on Today. Will hold the visibility toggles, units and
 * reminders (PLAN.md → backlog). For now: sign out.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(authRepository) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Settings", onBack = onBack, modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs))
        ScreenColumn {
            StrokeButton(text = "Sign out", onClick = viewModel::signOut)
        }
    }
}
