package com.laul.lauwell.core.catalog

/**
 * The five patient-facing categories: the top level of the data hierarchy
 * (`docs/data-structure/Patient Data Categories`). Each comes with the test that decides whether a
 * new data point belongs in it.
 *
 * [key] is stable: it ends up in stored settings and must never change once shipped.
 */
enum class Category(val key: String, val label: String) {
    /** A measured value read against a clinical normal range. */
    Vitals(key = "vitals", label = "Vitals"),

    /** A fact with a start date rather than a timestamp, or the record of an encounter. */
    MedicalRecords(key = "medical_records", label = "Medical Records"),

    /** A daily log measured against a goal you set. */
    Lifestyle(key = "lifestyle", label = "Lifestyle"),

    /** Self-reported, no instrument involved. */
    Symptoms(key = "symptoms", label = "Symptoms"),

    /** What you take or use, and the products behind it. */
    TreatmentsAndAppliances(key = "treatments_appliances", label = "Treatments & appliances"),
}
