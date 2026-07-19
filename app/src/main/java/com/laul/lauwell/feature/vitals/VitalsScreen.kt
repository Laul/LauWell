package com.laul.lauwell.feature.vitals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Vitals — blood pressure, heart rate, temperature, weight, etc.
 *
 * Placeholder entry point. As this module grows, keep it self-contained under this package:
 * feature.vitals.data (entities/DAO), feature.vitals.domain (repository), feature.vitals.ui
 * (this screen + ViewModel + state) — mirroring the repository-pattern decision in PLAN.md.
 */
@Composable
fun VitalsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Vitals",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
