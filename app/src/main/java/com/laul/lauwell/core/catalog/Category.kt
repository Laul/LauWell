package com.laul.lauwell.core.catalog

/**
 * The seven patient-facing categories: the top level of the data hierarchy
 * (`docs/data-structure/health-data-model/Patient Data Categories`). Each comes with the test that decides whether a
 * new data point belongs in it.
 *
 * [key] is stable: it ends up in stored settings and must never change once shipped.
 */
enum class Category(val key: String, val label: String) {
    /** A measured value read against a clinical normal range. */
    Vitals(key = "vitals", label = "Vitals"),

    /** A result from a laboratory report, read against the range printed on it. */
    Labs(key = "labs", label = "Labs"),

    /** A fact with a start date rather than a timestamp, a document, or the record of an encounter. */
    MedicalRecords(key = "medical_records", label = "Medical Records"),

    /** A daily log measured against a goal you set. */
    Lifestyle(key = "lifestyle", label = "Lifestyle"),

    /** Self-reported, no instrument involved. */
    Symptoms(key = "symptoms", label = "Symptoms"),

    /** What you take or use, the products behind it, and the care you have planned. */
    TreatmentsAndAppliances(key = "treatments_appliances", label = "Treatments & Appliances"),

    /** Everything about the stoma: its output, the skin around it, the appliance on it. */
    Ostomy(key = "ostomy", label = "Ostomy"),
}
