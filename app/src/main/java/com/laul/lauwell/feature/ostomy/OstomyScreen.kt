package com.laul.lauwell.feature.ostomy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Ostomy Management — device installation, changes, wear time, issues. Brand-agnostic by design
 * (see CLAUDE.md) — no dependency on specific medical device brands anywhere in this module.
 *
 * Placeholder entry point. Same internal-package convention as feature.vitals applies here:
 * data / domain / ui subpackages once real functionality is added.
 */
@Composable
fun OstomyScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Ostomy",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
