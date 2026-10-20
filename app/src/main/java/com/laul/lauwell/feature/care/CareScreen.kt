package com.laul.lauwell.feature.care

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.ScreenHeader
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** The Care tab. Placeholder until it is designed in the locked system (PLAN.md → M6). */
@Composable
fun CareScreen() {
    ScreenColumn {
        ScreenHeader("Treatments & Appliances")
        BasicText(
            text = "Medications, the ostomy module (products, changes, wear time and skin) and appointments. Not designed yet.",
            style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
        )
    }
}
