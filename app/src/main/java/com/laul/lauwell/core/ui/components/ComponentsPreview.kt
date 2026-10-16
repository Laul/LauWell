package com.laul.lauwell.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Category
import com.laul.lauwell.core.ui.theme.LauWellTheme

// Design reference: the Today, Tracker, quick-log and Steps screens of the locked design (13b).
// Sample values only; nothing here is wired to data.

@Preview(name = "Light", widthDp = 360, heightDp = 1100)
@Preview(name = "Dark", widthDp = 360, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsPreview() {
    LauWellTheme {
        val spacing = LauWellTheme.spacing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LauWellTheme.colors.bg)
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Section("Quick log") {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    QuickLogChip(R.drawable.ic_battery_3_bar, "Fatigue", Category.Symptoms, onClick = {})
                    QuickLogChip(R.drawable.ic_blood_pressure, "Blood pressure", Category.Vitals, onClick = {})
                }
            }
            Section(
                "Pinned",
                trailing = {
                    LauWellIcon(R.drawable.ic_edit_pinned, "Edit pinned", LauWellTheme.colors.ink2, size = spacing.xl)
                },
            ) {
                Panel(contentPadding = PaddingValues(spacing.panel)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        PinnedTile(R.drawable.ic_footprint, "Steps", "4,812", "", "of 8,000", Category.Lifestyle, 0.6f, Modifier.weight(1f))
                        PinnedTile(R.drawable.ic_favorite, "Resting HR", "64", "bpm", "in range", Category.Vitals, null, Modifier.weight(1f))
                        PinnedTile(R.drawable.ic_water_drop, "Output", null, "L", "no entry yet", Category.Vitals, null, Modifier.weight(1f))
                    }
                }
            }
            Section("Vitals", category = Category.Vitals) {
                Panel {
                    MetricRow(R.drawable.ic_favorite, "Heart rate", "Resting · Health Connect", "64", Category.Vitals, MetricSource.Synced, onClick = {})
                    PanelDivider()
                    MetricRow(R.drawable.ic_blood_pressure, "Blood pressure", "3 days ago · mmHg", "118/76", Category.Vitals, MetricSource.Manual, onClick = {})
                }
            }
            var range by remember { mutableIntStateOf(0) }
            SegmentedSelector(listOf("Week", "Month", "3 months"), range, onSelect = { range = it })
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                StrokeButton("Log", onClick = {})
                StrokeButton("Disabled", onClick = {}, enabled = false)
            }
            FilledButton("Allow 3 permissions", onClick = {})
            QuietButton("Not now", onClick = {})
        }
    }
}

@Preview(name = "Sheet light", widthDp = 360, heightDp = 640)
@Preview(name = "Sheet dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickLogSheetPreview() {
    LauWellTheme {
        var rating by remember { mutableStateOf<Int?>(6) }
        Box(Modifier.fillMaxSize().background(LauWellTheme.colors.bg)) {
            LauWellBottomSheet(visible = true, onDismissRequest = {}) {
                Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
                    CategoryLine(Category.Symptoms, "Symptoms · Systemic")
                    BasicText("Fatigue", style = LauWellTheme.typography.headline.copy(color = LauWellTheme.colors.ink))
                    BasicText(
                        "How tired do you feel right now?",
                        style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.ink2),
                    )
                }
                PipRating(rating, { rating = it }, Category.Symptoms, "1 · Rested", "10 · Exhausted")
                FilledButton("Save · ${rating ?: "–"}/10", onClick = {}, enabled = rating != null)
            }
        }
    }
}
