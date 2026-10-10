package com.laul.lauwell.core.catalog.metrics

import com.laul.lauwell.core.catalog.Aggregation
import com.laul.lauwell.core.catalog.ChartKind
import com.laul.lauwell.core.catalog.Coding
import com.laul.lauwell.core.catalog.Coding.Role
import com.laul.lauwell.core.catalog.Coding.System.Loinc
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.Quantity
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.catalog.SubCategory.VitalsCardiovascular

/**
 * Vitals → Cardiovascular. Normal ranges are generic adult defaults, not personal targets.
 * Codes are copied from `docs/data-structure/Patient Data Codings.csv`.
 */
object CardiovascularMetrics {

    val HeartRate = MetricDefinition(
        id = MetricId("heart_rate"),
        label = "Heart rate",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Measurement,
        quantities = listOf(
            Quantity(
                key = "value",
                label = "Heart rate",
                unit = "/min",
                displayUnit = "bpm",
                normalRange = 60.0..100.0, // adult at rest
            ),
        ),
        aggregation = Aggregation.MinAverageMax,
        chartKind = ChartKind.RangeBar,
        codings = listOf(
            Coding(Loinc, "8867-4", "Heart rate", Role.Primary, verified = true),
            Coding(Loinc, "40443-4", "Heart rate --resting", Role.Variant, verified = true),
        ),
    )

    /**
     * RMSSD, because that is what Health Connect provides. No code yet: the drafted LOINC code
     * (80404-7) is for SDNN, which is not interchangeable with RMSSD. No normal range either:
     * HRV is read against your own baseline.
     */
    val HeartRateVariability = MetricDefinition(
        id = MetricId("heart_rate_variability"),
        label = "Heart rate variability",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Measurement,
        quantities = listOf(
            Quantity(key = "rmssd", label = "HRV (RMSSD)", unit = "ms", displayUnit = "ms"),
        ),
        aggregation = Aggregation.Average,
        chartKind = ChartKind.Line,
    )

    val BloodPressure = MetricDefinition(
        id = MetricId("blood_pressure"),
        label = "Blood pressure",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Measurement,
        quantities = listOf(
            Quantity(
                key = "systolic",
                label = "Systolic",
                unit = "mm[Hg]",
                displayUnit = "mmHg",
                normalRange = 90.0..120.0,
                coding = Coding(Loinc, "8480-6", "Systolic blood pressure", Role.Component, verified = true),
            ),
            Quantity(
                key = "diastolic",
                label = "Diastolic",
                unit = "mm[Hg]",
                displayUnit = "mmHg",
                normalRange = 60.0..80.0,
                coding = Coding(Loinc, "8462-4", "Diastolic blood pressure", Role.Component, verified = false),
            ),
        ),
        aggregation = Aggregation.Average,
        chartKind = ChartKind.RangeBar, // one bar from diastolic to systolic
        codings = listOf(
            Coding(Loinc, "85354-9", "Blood pressure panel with all children optional", Role.Primary, verified = true),
        ),
    )

    /** A recorded waveform stored as a file: no number to summarise or chart. */
    val EcgTrace = MetricDefinition(
        id = MetricId("ecg_trace"),
        label = "ECG trace",
        subCategory = VitalsCardiovascular,
        shape = StorageShape.Document,
    )

    val all: List<MetricDefinition> = listOf(HeartRate, HeartRateVariability, BloodPressure, EcgTrace)
}
