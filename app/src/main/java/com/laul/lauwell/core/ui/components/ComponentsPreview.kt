package com.laul.lauwell.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.ui.theme.LauWellTheme

// Design reference: the Today, Tracker, quick-log and Steps screens of the locked design (13b).
// Sample values only; nothing here is wired to data.

@Preview(name = "Light", widthDp = 360, heightDp = 1400)
@Preview(name = "Dark", widthDp = 360, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
            Section("Mini charts", category = Category.Lifestyle) {
                Panel {
                    MetricRow(R.drawable.ic_footprint, "Steps", "60% of goal", "4,812", Category.Lifestyle, SampleSteps, onClick = {})
                    PanelDivider()
                    MetricRow(R.drawable.ic_mood, "Mood", "1 low day", "Good", Category.Lifestyle, SampleMood, onClick = {})
                    PanelDivider()
                    MetricRow(R.drawable.ic_psychology, "Brain fog", "4 episodes", "Mild", Category.Symptoms, SampleBrainFog, onClick = {})
                    PanelDivider()
                    MetricRow(R.drawable.ic_menstrual_health, "Menstrual cycle", "Fertile window", "Day 14", Category.Vitals, SampleCycle, onClick = {})
                    PanelDivider()
                    MetricRow(R.drawable.ic_footprint, "Empty bars", "No data yet", null, Category.Lifestyle, MiniChartData.empty(MiniChartKind.Bars), onClick = {})
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    MetricWidget(R.drawable.ic_footprint, "Steps", "60% of goal", "4,812", "steps", Category.Lifestyle, SampleSteps, onClick = {}, modifier = Modifier.weight(1f).fillMaxHeight())
                    MetricWidget(R.drawable.ic_mood, "Mood", "No data yet", null, null, Category.Lifestyle, MiniChartData.empty(MiniChartKind.Line), onClick = {}, modifier = Modifier.weight(1f).fillMaxHeight())
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

// The design's sample week (UI-spec, mini-chart rules).
private val SampleSteps = MiniChartData.Bars(listOf(7420.0, 9104.0, 5880.0, null, 8260.0, 6530.0, 4812.0), max = 10000.0, goal = 8000.0)
private val SampleMood = MiniChartData.Line(listOf(3.0, 4.0, 4.0, 2.0, 3.0, 4.0, null), min = 1.0, max = 5.0)
private val SampleBrainFog = MiniChartData.SeverityDots(listOf(0, 1, 2, 1, 1, null, null), levels = 4)
private val SampleCycle = MiniChartData.CycleStrip(day = 14, period = 1..5, fertile = 12..16)
