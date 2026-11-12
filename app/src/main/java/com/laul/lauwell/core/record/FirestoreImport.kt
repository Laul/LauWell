package com.laul.lauwell.core.record

import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.data.RecordDao
import com.laul.lauwell.core.data.SyncCursorEntity
import com.laul.lauwell.core.data.toRows
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * The one-time copy of what you logged in Firestore (Mood) into Room, from where the sync sends it to
 * Postgres. Runs before each sync until it has worked once for the signed-in account; removed with
 * Firestore (task "Move Mood off Firestore", stage 2).
 *
 * Firestore ids are not UUIDs, which the server requires: each gets a UUID derived from it ([postgresId]),
 * links included, so running the copy again lands on the same records instead of duplicates.
 */
class FirestoreImport(
    private val dao: RecordDao,
    private val session: StateFlow<Session?>,
    private val fetch: suspend (uid: String) -> List<Measurement>,
) {

    /** Copies once per account; nothing when signed out or when the local tables belong to another account. */
    suspend fun run() {
        val uid = session.value?.uid ?: return
        val owner = dao.cursor()?.uid
        if (owner != null && owner != uid) return
        if (dao.cursor(Done)?.uid == uid) return

        val entries = fetch(uid).map { it.withPostgresIds() }
        // Records pointed at go first, so a link finds its target.
        entries.sortedBy { it.links.isNotEmpty() }.forEach { dao.save(toRows(it)) }
        dao.setCursor(SyncCursorEntity(Done, uid, changeSeq = entries.size.toLong()))
    }

    private companion object {
        /** Marks the copy done for an account, in `sync_cursor`; its change number holds how many were copied. */
        const val Done = "firestore-import"
    }
}

/** The UUID a Firestore record id becomes in Room and Postgres, always the same one. */
internal fun postgresId(firestoreId: String): String =
    UUID.nameUUIDFromBytes("firestore:$firestoreId".toByteArray()).toString()

private fun Measurement.withPostgresIds(): Measurement = copy(
    id = RecordId(postgresId(id.value)),
    links = links.map { it.copy(to = RecordId(postgresId(it.to.value))) },
)
