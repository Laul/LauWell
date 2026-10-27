package com.laul.lauwell.core.catalog.metrics

import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.MiniChartKind
import com.laul.lauwell.core.catalog.NavScreen
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.RatingPrompt
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory.LifestyleMindAndMood

/** Lifestyle → Mind and mood: what you log yourself, kept in your account (M4). */
object MindMetrics {

    /**
     * A self-rating from 1 (awful) to 5 (great), logged as often as you like; a day reads the mean
     * of its entries. The first manual entry (Notion: *Mood end-to-end*).
     */
    val Mood = MetricDefinition(
        id = MetricId("mood"),
        label = "Mood",
        subCategory = LifestyleMindAndMood,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_mood,
        sources = setOf(DataSource.Manual),
        screens = setOf(NavScreen.Tracker),
        quantities = listOf(Quantity(key = "value", label = "Mood", unit = "/5", scale = 1..5)),
        aggregation = Aggregation.Average,
        chartKind = ChartKind.Line,
        miniChart = MiniChartKind.Line,
        prompt = RatingPrompt("How do you feel right now?", lowLabel = "1 · Awful", highLabel = "5 · Great"),
    )

    val all: List<MetricDefinition> = listOf(Mood)
}
