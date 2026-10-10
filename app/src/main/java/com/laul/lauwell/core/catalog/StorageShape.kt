package com.laul.lauwell.core.catalog

/**
 * The seven storage shapes that carry every data point (PLAN.md → Decisions → Storage shapes).
 * Category and sub-category are facets on the metric definition; the shape decides how a record
 * is stored. The record types themselves come with the M2 "domain model" task.
 */
enum class StorageShape(val label: String) {
    /** value · unit · instant */
    Measurement("Measurement"),

    /** instant + optional duration */
    Event("Event"),

    /** start · end · status */
    Interval("Interval"),

    /** no instant: blood type, family history */
    StandingFact("Standing fact"),

    /** a thing with attributes: a medication, an ostomy product */
    CatalogueItem("Catalogue"),

    /** a file with a date */
    Document("Document"),

    /** computed from other records, e.g. wear time from consecutive appliance changes */
    Derived("Derived"),
}
