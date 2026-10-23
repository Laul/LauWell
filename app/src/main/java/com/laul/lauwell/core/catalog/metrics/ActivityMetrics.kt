package com.laul.lauwell.core.catalog.metrics

import androidx.health.connect.client.records.StepsRecord
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
import com.laul.lauwell.core.catalog.SubCategory.LifestyleActivity

/** Lifestyle → Activity. Goals are defaults until they can be set by the user. */
object ActivityMetrics {

    /**
     * The day's total. Garmin, Samsung Health and Google may all write the same walk, so the total is
     * Health Connect's aggregation, which applies the apps' priority; records are never added up.
     */
    val Steps = MetricDefinition(
        id = MetricId("steps"),
        label = "Steps",
        subCategory = LifestyleActivity,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_footprint,
        sources = setOf(DataSource.Wearable),
        screens = setOf(NavScreen.Tracker),
        quantities = listOf(Quantity(key = "value", label = "Steps", unit = "steps")),
        aggregation = Aggregation.Sum,
        chartKind = ChartKind.Bar,
        miniChart = MiniChartKind.Bars,
        goal = Goal(8000.0, Goal.Direction.AtLeast), // the design's default
        healthConnectTypes = setOf(StepsRecord::class),
    )

    val all: List<MetricDefinition> = listOf(Steps)
}
