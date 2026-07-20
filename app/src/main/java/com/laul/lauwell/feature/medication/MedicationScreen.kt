package com.laul.lauwell.feature.medication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Medication Tracking — take and renewal reminders.
 *
 * Placeholder entry point. Same internal-package convention as feature.health applies here:
 * data / domain / ui subpackages once real functionality is added. Will need the shared
 * reminder engine described under Settings and Notifications in PLAN.md.
 */
@Composable
fun MedicationScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Medication",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
