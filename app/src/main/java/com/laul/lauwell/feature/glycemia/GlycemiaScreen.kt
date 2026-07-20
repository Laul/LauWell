package com.laul.lauwell.feature.glycemia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * Glycemia Monitoring — blood sugar readings, hypo/hyper alerts with configurable thresholds.
 *
 * Placeholder entry point. Same internal-package convention as feature.health applies here:
 * feature.glycemia.data, feature.glycemia.domain, feature.glycemia.ui.
 */
@Composable
fun GlycemiaScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Glycemia",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
    }
}
