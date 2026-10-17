package com.laul.lauwell.feature.records

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.ScreenHeader
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** The Records tab. Placeholder until it is designed in the locked system (PLAN.md → M6). */
@Composable
fun RecordsScreen() {
    ScreenColumn {
        ScreenHeader("Records")
        BasicText(
            text = "Labs, documents, visit notes and medical history. Not designed yet.",
            style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
        )
    }
}
