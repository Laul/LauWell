package com.laul.lauwell.core.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.catalog.StorageShape
import com.laul.lauwell.core.record.LinkRelation
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.Provenance
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordLink
import com.laul.lauwell.core.record.RecordingMethod
import com.laul.lauwell.core.record.Source
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset

// What you log, every kind of act (docs/architecture/backend/shared-foundation.md → 2 and 3): one row in
// `record`, its numbers in `record_value`, its context links in `record_link`. A kind with fields of its own
// adds a detail table keyed by `record_id`, with its feature. Same tables and column names as in Postgres
// (db-schema/schema-tables.md → 3), so the sync copies rows one-to-one; Room has no `user_id`.
// Unlike the Health Connect cache, these rows are typed by the user: every schema change needs a migration.

/**
 * One act. [shape] stores the [StorageShape]'s name, [source] its [Source.key]: never rename either once
 * shipped. Times are epoch milliseconds. A deleted act keeps its row with [deletedAt] set, so the
 * deletion reaches Postgres.
 */
@Entity(tableName = "record", indices = [Index("metric_id", "time_utc")])
data class RecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "metric_id") val metricId: String,
    val shape: String,
    @ColumnInfo(name = "time_utc") val timeUtc: Long,
    /** Interval only, and optional: an ongoing course has none. */
    @ColumnInfo(name = "end_time_utc") val endTimeUtc: Long? = null,
    @ColumnInfo(name = "zone_offset_s") val zoneOffsetS: Int? = null,
    val source: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    @ColumnInfo(name = "edited_at") val editedAt: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    val note: String? = null,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
)

/** One number of a simple measurement (mood 7), keyed by the metric's `Quantity.key`. */
@Entity(
    tableName = "record_value",
    primaryKeys = ["record_id", "quantity_key"],
    foreignKeys = [ForeignKey(RecordEntity::class, ["id"], ["record_id"], onDelete = ForeignKey.CASCADE)],
)
data class RecordValueEntity(
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "quantity_key") val quantityKey: String,
    val value: Double,
)

/**
 * A context link, stored once on the record that points. [relation] stores the [LinkRelation.key].
 * Links that define a record (a dose → its medication) are named columns on detail tables instead.
 */
@Entity(
    tableName = "record_link",
    primaryKeys = ["from_id", "to_id", "relation"],
    foreignKeys = [
        ForeignKey(RecordEntity::class, ["id"], ["from_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(RecordEntity::class, ["id"], ["to_id"], onDelete = ForeignKey.CASCADE),
    ],
    // "What is linked to this encounter?"
    indices = [Index("to_id")],
)
data class RecordLinkEntity(
    @ColumnInfo(name = "from_id") val fromId: String,
    @ColumnInfo(name = "to_id") val toId: String,
    val relation: String,
)

/** A record with its values and the links it holds. */
data class RecordRows(
    @Embedded val record: RecordEntity,
    @Relation(parentColumn = "id", entityColumn = "record_id")
    val values: List<RecordValueEntity>,
    @Relation(parentColumn = "id", entityColumn = "from_id")
    val links: List<RecordLinkEntity>,
)

@Dao
abstract class RecordDao {

    /** A metric's acts from [from] to [to] (epoch ms, inclusive), oldest first; deleted ones left out. */
    @Transaction
    @Query("SELECT * FROM record WHERE metric_id = :metric AND time_utc BETWEEN :from AND :to AND deleted_at IS NULL ORDER BY time_utc")
    abstract fun records(metric: String, from: Long, to: Long): Flow<List<RecordRows>>

    /** The acts that point at [id] (an encounter's readings, a rating's photos), deleted ones left out. */
    @Transaction
    @Query(
        "SELECT record.* FROM record JOIN record_link ON record_link.from_id = record.id " +
            "WHERE record_link.to_id = :id AND record.deleted_at IS NULL ORDER BY record.time_utc",
    )
    abstract fun linkedTo(id: String): Flow<List<RecordRows>>

    @Upsert
    protected abstract suspend fun upsertRecord(record: RecordEntity)

    @Query("DELETE FROM record_value WHERE record_id = :id")
    protected abstract suspend fun deleteValues(id: String)

    @Query("DELETE FROM record_link WHERE from_id = :id")
    protected abstract suspend fun deleteLinks(id: String)

    @Insert
    protected abstract suspend fun insertValues(values: List<RecordValueEntity>)

    @Insert
    protected abstract suspend fun insertLinks(links: List<RecordLinkEntity>)

    /**
     * Writes [rows] whole, new or edited, in one transaction: the record, then its values and links in
     * place of the old ones, and queues it for the sync. An upsert, not a replace, so the links other
     * records hold to it stay.
     */
    @Transaction
    open suspend fun save(rows: RecordRows) {
        require(rows.values.all { it.recordId == rows.record.id }) { "values of another record" }
        require(rows.links.all { it.fromId == rows.record.id }) { "links held by another record" }
        upsertRecord(rows.record)
        replaceChildren(rows)
        enqueue(rows.record.id)
    }

    /** Marks the record deleted at [at] (epoch ms), keeping the row so the deletion syncs. */
    @Transaction
    open suspend fun markDeleted(id: String, at: Long) {
        setDeleted(id, at)
        enqueue(id)
    }

    @Query("UPDATE record SET deleted_at = :at, edited_at = :at WHERE id = :id")
    protected abstract suspend fun setDeleted(id: String, at: Long)

    private suspend fun replaceChildren(rows: RecordRows) {
        deleteValues(rows.record.id)
        deleteLinks(rows.record.id)
        insertValues(rows.values)
        insertLinks(rows.links)
    }

    // Sync (EntrySync) ------------------------------------------------------------------------------------

    @Query("INSERT OR IGNORE INTO outbox (record_id, version) VALUES (:id, 0)")
    protected abstract suspend fun addToOutbox(id: String)

    // Two statements, not an UPSERT: Android 8-10 ship SQLite older than 3.24.
    @Query("UPDATE outbox SET version = version + 1, refused = NULL WHERE record_id = :id")
    protected abstract suspend fun bumpOutbox(id: String)

    /** Queues [id] to be sent, or marks it changed again if it already waits; an edit clears a refusal. */
    private suspend fun enqueue(id: String) {
        addToOutbox(id)
        bumpOutbox(id)
    }

    /** What waits to be sent, in the order first queued; refused ones left out until edited again. */
    @Query("SELECT * FROM outbox WHERE refused IS NULL ORDER BY rowid")
    abstract suspend fun queued(): List<OutboxEntity>

    @Transaction
    @Query("SELECT * FROM record WHERE id IN (:ids)")
    abstract suspend fun rows(ids: List<String>): List<RecordRows>

    /** The server has [id] as of [version]; a later edit, queued meanwhile, stays. */
    @Query("DELETE FROM outbox WHERE record_id = :id AND version = :version")
    abstract suspend fun sent(id: String, version: Int)

    /** The server refused [id] as of [version] for [reason]; kept, not resent until edited again. */
    @Query("UPDATE outbox SET refused = :reason WHERE record_id = :id AND version = :version")
    abstract suspend fun refused(id: String, version: Int, reason: String)

    @Query("SELECT record_id FROM outbox")
    protected abstract suspend fun outboxIds(): List<String>

    /** Changes waiting to be sent (refused ones left out). */
    @Query("SELECT count(*) FROM outbox WHERE refused IS NULL")
    abstract fun waitingCount(): Flow<Int>

    /** Changes the server refused, kept until edited again. */
    @Query("SELECT count(*) FROM outbox WHERE refused IS NOT NULL")
    abstract fun refusedCount(): Flow<Int>

    /** Changes not on the server, waiting or refused: lost if the local tables are cleared now. */
    @Query("SELECT count(*) FROM outbox")
    abstract suspend fun unsentCount(): Int

    @Query("DELETE FROM record_link")
    protected abstract suspend fun clearLinks()

    @Query("DELETE FROM record_value")
    protected abstract suspend fun clearValues()

    @Query("DELETE FROM record")
    protected abstract suspend fun clearRecords()

    @Query("DELETE FROM outbox")
    protected abstract suspend fun clearOutbox()

    @Query("DELETE FROM sync_cursor")
    protected abstract suspend fun clearCursors()

    /**
     * Empties every table of what you log, and the sync's state, in one transaction: on sign-out, so the
     * next account starts clean and pulls its own entries from the server.
     */
    @Transaction
    open suspend fun clearUserData() {
        clearLinks()
        clearValues()
        clearRecords()
        clearOutbox()
        clearCursors()
    }

    @Query("SELECT * FROM sync_cursor WHERE name = :name")
    abstract suspend fun cursor(name: String = RecordCursor): SyncCursorEntity?

    @Query("SELECT * FROM sync_cursor WHERE name = :name")
    abstract fun cursorUpdates(name: String = RecordCursor): Flow<SyncCursorEntity?>

    @Upsert
    abstract suspend fun setCursor(cursor: SyncCursorEntity)

    /**
     * Writes records pulled from the server, all in one transaction with the new [cursor]: every record
     * first, then values and links, so a link to a record later in the pull finds it. A record that still
     * waits in the outbox keeps the local edit; the server settles it when that edit is sent.
     */
    @Transaction
    open suspend fun applyPulled(pulled: List<RecordRows>, cursor: SyncCursorEntity) {
        val waiting = outboxIds().toSet()
        val incoming = pulled.filter { it.record.id !in waiting }
        incoming.forEach { upsertRecord(it.record) }
        incoming.forEach { replaceChildren(it) }
        setCursor(cursor)
    }
}

/**
 * A record waiting to be sent. [version] counts local changes, so a change made while a push is in
 * flight is not lost when that push is confirmed. [refused] holds the server's reason (`invalid`,
 * `not_yours`) for a change that will never pass as it is.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey @ColumnInfo(name = "record_id") val recordId: String,
    val version: Int,
    val refused: String? = null,
)

/**
 * How far the pull got ([changeSeq], the server's change number) and for whom ([uid], the Firebase
 * uid): the local tables belong to that account, and are never sent to another one. [syncedAt] is the
 * end of the last full run (epoch ms), null before the first.
 */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(
    @PrimaryKey val name: String,
    val uid: String,
    @ColumnInfo(name = "change_seq") val changeSeq: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
)

/** [SyncCursorEntity.name] of the records' pull. */
const val RecordCursor = "record"

/** Bumped on a breaking change to how a record is stored in these tables. */
internal const val RecordRowsSchemaVersion = 1

/** [measurement] as rows. */
internal fun toRows(measurement: Measurement): RecordRows {
    val id = measurement.id.value
    return RecordRows(
        record = RecordEntity(
            id = id,
            metricId = measurement.metric.value,
            shape = StorageShape.Measurement.name,
            timeUtc = measurement.time.toEpochMilli(),
            zoneOffsetS = measurement.zoneOffset?.totalSeconds,
            source = measurement.provenance.source.key,
            recordedAt = measurement.provenance.recordedAt.toEpochMilli(),
            editedAt = measurement.provenance.editedAt?.toEpochMilli(),
            note = measurement.note,
            schemaVersion = RecordRowsSchemaVersion,
        ),
        values = measurement.values.map { (key, value) -> RecordValueEntity(id, key, value) },
        links = measurement.links.map { RecordLinkEntity(id, it.to.value, it.relation.key) },
    )
}

/**
 * The [Measurement] in [rows], or null when it is another shape, from a newer schema, or unreadable (a
 * bad row is skipped, never a crash). Values come back in the order they were stored.
 */
internal fun measurementFrom(rows: RecordRows): Measurement? = runCatching {
    val record = rows.record
    if (record.shape != StorageShape.Measurement.name || record.schemaVersion > RecordRowsSchemaVersion) return null
    val source = Source.entries.firstOrNull { it.key == record.source } ?: return null
    Measurement(
        id = RecordId(record.id),
        metric = MetricId(record.metricId),
        time = Instant.ofEpochMilli(record.timeUtc),
        zoneOffset = record.zoneOffsetS?.let(ZoneOffset::ofTotalSeconds),
        values = rows.values.associate { it.quantityKey to it.value },
        provenance = Provenance(
            source = source,
            recordingMethod = if (source == Source.Manual) RecordingMethod.Manual else RecordingMethod.Unknown,
            recordedAt = Instant.ofEpochMilli(record.recordedAt),
            editedAt = record.editedAt?.let(Instant::ofEpochMilli),
        ),
        links = rows.links.mapNotNull { link ->
            LinkRelation.entries.firstOrNull { it.key == link.relation }?.let { RecordLink(RecordId(link.toId), it) }
        },
        note = record.note,
    )
}.getOrNull()
