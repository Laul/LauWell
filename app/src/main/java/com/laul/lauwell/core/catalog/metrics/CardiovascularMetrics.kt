package com.laul.lauwell.core.catalog.metrics

import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.DataSource
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory.VitalsCardiovascular

/**
 * Vitals → Cardiovascular. Normal ranges are generic adult defaults, not personal targets.
 * No terminology codes yet: standardization comes later (PLAN.md → Decisions → Terminology codes).
 */
object CardiovascularMetrics {

    val HeartRate = MetricDefinition(
        id = MetricId("heart_rate"),
        label = "Heart rate",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_favorite,
        sources = setOf(DataSource.Wearable),
        quantities = listOf(
            Quantity(
                key = "value",
                label = "Heart rate",
                unit = "bpm",
                normalRange = 60.0..100.0, // adult at rest
            ),
        ),
        aggregation = Aggregation.MinAverageMax,
        chartKind = ChartKind.RangeBar,
    )

    val BloodPressure = MetricDefinition(
        id = MetricId("blood_pressure"),
        label = "Blood pressure",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Measurement,
        icon = R.drawable.ic_blood_pressure,
        sources = setOf(DataSource.Wearable, DataSource.Manual),
        quantities = listOf(
            Quantity(
                key = "systolic",
                label = "Systolic",
                unit = "mmHg",
                normalRange = 90.0..120.0,
            ),
            Quantity(
                key = "diastolic",
                label = "Diastolic",
                unit = "mmHg",
                normalRange = 60.0..80.0,
            ),
        ),
        aggregation = Aggregation.Average,
        chartKind = ChartKind.RangeBar, // one bar from diastolic to systolic
    )

    /** A recorded waveform stored as a file: no number to summarise or chart. */
    val EcgTrace = MetricDefinition(
        id = MetricId("ecg_trace"),
        label = "ECG trace",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Document,
        icon = R.drawable.ic_ecg_heart,
        sources = setOf(DataSource.Wearable),
    )

    val all: List<MetricDefinition> = listOf(HeartRate, BloodPressure, EcgTrace)
}
