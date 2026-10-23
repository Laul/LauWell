package com.laul.lauwell.core.catalog.metrics

import androidx.health.connect.client.records.SleepSessionRecord
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.Goal
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory.LifestyleSleep
import com.laul.lauwell.core.catalog.ValueFormat

/**
 * Lifestyle → Sleep. A provisional design (no design yet, PLAN.md → M3): one metric for the night,
 * its stages as a breakdown, as sleep trackers commonly show them.
 */
object SleepMetrics {

    /**
     * Time asleep in a night, in minutes, with the "Sleep stages" data point as its breakdown. The
     * night is the one session Health Connect's aggregation favours (the apps' priority); its stages
     * come from that same app, never mixed with another's.
     */
    val SleepDuration = MetricDefinition(
        id = MetricId("sleep_duration"),
        label = "Sleep duration",
        subCategory = LifestyleSleep,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_bedtime,
        sources = setOf(DataSource.Wearable),
        screens = setOf(NavScreen.Tracker),
        quantities = listOf(Quantity(key = "value", label = "Asleep", unit = "", format = ValueFormat.Duration)),
        // Stacked from the bottom: deep, REM, light; awake is time beside the sleep, not part of it.
        breakdown = listOf(
            Quantity(key = "deep", label = "Deep", unit = "", format = ValueFormat.Duration),
            Quantity(key = "rem", label = "REM", unit = "", format = ValueFormat.Duration),
            Quantity(key = "light", label = "Light", unit = "", format = ValueFormat.Duration),
            Quantity(key = "awake", label = "Awake", unit = "", format = ValueFormat.Duration, inTotal = false),
        ),
        aggregation = Aggregation.Sum,
        chartKind = ChartKind.StackedBar,
        miniChart = MiniChartKind.Bars,
        goal = Goal(8 * 60.0, Goal.Direction.AtLeast), // the design's default: 8 h
        healthConnectTypes = setOf(SleepSessionRecord::class),
        nightly = true,
    )

    val all: List<MetricDefinition> = listOf(SleepDuration)
}
