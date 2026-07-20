package com.laul.lauwell.feature.health

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Health — vitals (blood pressure, heart rate, temperature, weight), activity (steps, exercise,
 * daily movement), and sleep (duration, quality, patterns).
 *
 * Placeholder entry point. As this module grows, keep it self-contained under this package:
 * feature.health.data (entities/DAO), feature.health.domain (repository), feature.health.ui
 * (this screen + ViewModel + state) — mirroring the repository-pattern decision in PLAN.md.
 */
@Composable
fun HealthScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Health",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
