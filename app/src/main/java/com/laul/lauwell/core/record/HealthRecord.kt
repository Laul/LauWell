package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.MetricId
import java.time.Instant
import java.time.ZoneOffset

/**
 * Identifier of one record. Health Connect records keep Health Connect's id (one sample of a
 * series gets the series id plus its index); our own records get a Firestore document id.
 */
@JvmInline
value class RecordId(val value: String) {
    init {
        require(value.isNotBlank()) { "RecordId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * The fields every record carries, whatever its storage shape (PLAN.md → Decisions → *Records →
 * History*): History, link lookups and export only need these.
 *
 * Type is the metric plus the record's class (its shape); category comes from the metric's
 * definition, never stored twice. The one-line summary is computed for display ([summaryLine]).
 */
sealed interface HealthRecord {
    val id: RecordId
    val metric: MetricId

    /** When it happened, or when it started for a record with a duration. */
    val time: Instant

    /** The offset where it happened, to place it on the right local day; null when unknown. */
    val zoneOffset: ZoneOffset?

    val provenance: Provenance

    /** Context links to other records, stored on the record that points (never copied both ways). */
    val links: List<RecordLink>
}

/** "Recorded during" an encounter, "about" a skin rating … Shown generically ("Linked to: …"). */
data class RecordLink(val to: RecordId, val relation: LinkRelation)

/**
 * Why one record points at another. [key] is what is stored: never change it once shipped.
 * Links that define a record (a dose → its medication) are named fields instead, on the models
 * that need them (PLAN.md → Decisions → *Relationships between records*).
 */
enum class LinkRelation(val key: String) {
    RecordedDuring("recorded_during"),
    About("about"),
}
