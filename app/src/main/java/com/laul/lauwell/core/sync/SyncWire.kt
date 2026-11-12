package com.laul.lauwell.core.sync

import com.laul.lauwell.core.data.RecordEntity
import com.laul.lauwell.core.data.RecordLinkEntity
import com.laul.lauwell.core.data.RecordRows
import com.laul.lauwell.core.data.RecordValueEntity
import org.json.JSONArray
import org.json.JSONObject

// The sync API's wire format (backend/api/sync.js): a record with the Room / Postgres column names, times in
// epoch milliseconds, its numbers as { quantity_key: value } and its links as [{ to_id, relation }].

internal fun RecordRows.toWire(): JSONObject = JSONObject()
    .put("id", record.id)
    .put("metric_id", record.metricId)
    .put("shape", record.shape)
    .put("time_utc", record.timeUtc)
    .put("end_time_utc", record.endTimeUtc ?: JSONObject.NULL)
    .put("zone_offset_s", record.zoneOffsetS ?: JSONObject.NULL)
    .put("source", record.source)
    .put("recorded_at", record.recordedAt)
    .put("edited_at", record.editedAt ?: JSONObject.NULL)
    .put("deleted_at", record.deletedAt ?: JSONObject.NULL)
    .put("note", record.note ?: JSONObject.NULL)
    .put("schema_version", record.schemaVersion)
    .put("values", JSONObject().apply { values.forEach { put(it.quantityKey, it.value) } })
    .put("links", JSONArray(links.map { JSONObject().put("to_id", it.toId).put("relation", it.relation) }))

/** A pulled record as rows. Throws [org.json.JSONException] on a malformed one. */
internal fun recordRowsFromWire(json: JSONObject): RecordRows {
    val id = json.getString("id")
    val values = json.optJSONObject("values") ?: JSONObject()
    val links = json.optJSONArray("links") ?: JSONArray()
    return RecordRows(
        record = RecordEntity(
            id = id,
            metricId = json.getString("metric_id"),
            shape = json.getString("shape"),
            timeUtc = json.getLong("time_utc"),
            endTimeUtc = json.longOrNull("end_time_utc"),
            zoneOffsetS = json.longOrNull("zone_offset_s")?.toInt(),
            source = json.getString("source"),
            recordedAt = json.getLong("recorded_at"),
            editedAt = json.longOrNull("edited_at"),
            deletedAt = json.longOrNull("deleted_at"),
            note = if (json.isNull("note")) null else json.getString("note"),
            schemaVersion = json.getInt("schema_version"),
        ),
        values = values.keys().asSequence().map { RecordValueEntity(id, it, values.getDouble(it)) }.toList(),
        links = (0 until links.length()).map {
            val link = links.getJSONObject(it)
            RecordLinkEntity(id, link.getString("to_id"), link.getString("relation"))
        },
    )
}

private fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else getLong(name)
