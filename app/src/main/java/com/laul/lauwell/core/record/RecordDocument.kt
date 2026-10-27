package com.laul.lauwell.core.record

import com.google.firebase.Timestamp
import com.laul.lauwell.core.catalog.MetricId
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

// How a record is stored in Firestore: one document per record in users/{uid}/records (Notion →
// Decisions Tracker: *Firestore layout*), the same common fields for every type, then the type's
// own. Pure functions over plain maps, so they are unit-tested without Firebase. Field names and
// stored keys never change once shipped; a breaking change bumps [RecordSchemaVersion].
//
//   type: "measurement"   metric: "mood"   schemaVersion: 1
//   time: <timestamp>     zoneOffset: -14400 (seconds, or absent)
//   values: { value: 7.0 }                                  ← measurement only
//   provenance: { source: "manual", recordedAt: <timestamp>, editedAt: <timestamp> | absent }
//   links: [ { to: "<record id>", relation: "about" } ]
//   note: "after a bad night" | absent
//
// Category is not stored: it comes from the metric's definition.

internal const val RecordSchemaVersion = 1L

internal object RecordFields {
    const val Type = "type"
    const val Metric = "metric"
    const val SchemaVersion = "schemaVersion"
    const val Time = "time"
    const val ZoneOffset = "zoneOffset"
    const val Values = "values"
    const val Provenance = "provenance"
    const val Source = "source"
    const val RecordedAt = "recordedAt"
    const val EditedAt = "editedAt"
    const val Links = "links"
    const val To = "to"
    const val Relation = "relation"
    const val Note = "note"
}

/** [RecordFields.Type] values. */
internal object RecordTypes {
    const val Measurement = "measurement"
}

/** [record] as a Firestore document. Instants become `Date`, which Firestore stores as a timestamp. */
internal fun Measurement.toDocument(): Map<String, Any> = buildMap {
    put(RecordFields.Type, RecordTypes.Measurement)
    put(RecordFields.Metric, metric.value)
    put(RecordFields.SchemaVersion, RecordSchemaVersion)
    put(RecordFields.Time, Date.from(time))
    zoneOffset?.let { put(RecordFields.ZoneOffset, it.totalSeconds.toLong()) }
    // Qualified: inside buildMap, a bare `values` is the map being built.
    put(RecordFields.Values, this@toDocument.values)
    put(
        RecordFields.Provenance,
        buildMap {
            put(RecordFields.Source, provenance.source.key)
            put(RecordFields.RecordedAt, Date.from(provenance.recordedAt))
            provenance.editedAt?.let { put(RecordFields.EditedAt, Date.from(it)) }
        },
    )
    put(RecordFields.Links, links.map { mapOf(RecordFields.To to it.to.value, RecordFields.Relation to it.relation.key) })
    note?.let { put(RecordFields.Note, it) }
}

/**
 * The [Measurement] stored in document [id], or null when it is another type, from a newer schema,
 * or unreadable (a malformed document is skipped, never a crash).
 */
internal fun measurementFrom(id: String, data: Map<String, Any?>): Measurement? = runCatching {
    if (data[RecordFields.Type] != RecordTypes.Measurement) return null
    val version = (data[RecordFields.SchemaVersion] as? Number)?.toLong() ?: return null
    if (version > RecordSchemaVersion) return null
    val provenance = data[RecordFields.Provenance] as? Map<*, *> ?: return null
    val source = Source.entries.firstOrNull { it.key == provenance[RecordFields.Source] } ?: return null
    Measurement(
        id = RecordId(id),
        metric = MetricId(data[RecordFields.Metric] as? String ?: return null),
        time = instantOf(data[RecordFields.Time]) ?: return null,
        zoneOffset = (data[RecordFields.ZoneOffset] as? Number)?.let { ZoneOffset.ofTotalSeconds(it.toInt()) },
        values = (data[RecordFields.Values] as? Map<*, *> ?: return null)
            .entries.associate { (key, value) -> key as String to (value as Number).toDouble() },
        provenance = Provenance(
            source = source,
            recordingMethod = if (source == Source.Manual) RecordingMethod.Manual else RecordingMethod.Unknown,
            recordedAt = instantOf(provenance[RecordFields.RecordedAt]) ?: return null,
            editedAt = instantOf(provenance[RecordFields.EditedAt]),
        ),
        links = (data[RecordFields.Links] as? List<*>).orEmpty().mapNotNull { link ->
            val map = link as? Map<*, *> ?: return@mapNotNull null
            val relation = LinkRelation.entries.firstOrNull { it.key == map[RecordFields.Relation] } ?: return@mapNotNull null
            RecordLink(RecordId(map[RecordFields.To] as String), relation)
        },
        note = data[RecordFields.Note] as? String,
    )
}.getOrNull()

/** Firestore hands back a [Timestamp]; a `Date` is what we wrote, and what unit tests pass. */
private fun instantOf(value: Any?): Instant? = when (value) {
    null -> null
    is Date -> value.toInstant()
    is Timestamp -> value.toDate().toInstant()
    else -> null
}
