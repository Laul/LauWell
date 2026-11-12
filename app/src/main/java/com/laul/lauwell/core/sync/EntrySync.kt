package com.laul.lauwell.core.sync

import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.data.RecordCursor
import com.laul.lauwell.core.data.RecordDao
import com.laul.lauwell.core.data.RecordRows
import com.laul.lauwell.core.data.SyncCursorEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps what you log in step with Postgres (decision "Sync: our own"): sends the outbox, then pulls what
 * changed on the server since the last pull. The server settles conflicts (latest edit wins).
 *
 * The local tables belong to the account that first synced them ([SyncCursorEntity.uid]); while another
 * account is signed in, nothing is sent or fetched, so one person's entries never reach another's account.
 */
class EntrySync(
    private val dao: RecordDao,
    private val api: SyncApi,
    private val session: StateFlow<Session?>,
) {

    /** What one run did, or why it didn't run. */
    sealed interface Outcome {
        data class Synced(val sent: Int, val refused: Int, val received: Int) : Outcome
        data object SignedOut : Outcome
        data object OtherAccount : Outcome
    }

    /** One run. Throws an [java.io.IOException] when the network or the server fails, for the worker to retry. */
    suspend fun run(): Outcome {
        val uid = session.value?.uid ?: return Outcome.SignedOut
        val cursor = dao.cursor() ?: SyncCursorEntity(RecordCursor, uid, changeSeq = 0)
        if (cursor.uid != uid) return Outcome.OtherAccount
        val (sent, refused) = push()
        val received = pull(cursor)
        return Outcome.Synced(sent, refused, received)
    }

    /** Sends the outbox in batches. Returns how many were taken and how many refused for good. */
    private suspend fun push(): Pair<Int, Int> {
        var sent = 0
        var refused = 0
        for (batch in dao.queued().chunked(MaxBatch)) {
            val versions = batch.associate { it.recordId to it.version }
            // The server only takes UUIDs; an id it couldn't read would come back without one.
            val (sendable, unreadable) = versions.keys.partition(::isUuid)
            unreadable.forEach { dao.refused(it, versions.getValue(it), Invalid) }
            refused += unreadable.size
            if (sendable.isEmpty()) continue

            // In the order queued, so a record usually reaches the server before the ones that link to it.
            val order = sendable.withIndex().associate { (i, id) -> id to i }
            val result = api.push(dao.rows(sendable).sortedBy { order.getValue(it.record.id) })
            result.accepted.forEach { id -> versions[id]?.let { dao.sent(id, it) } }
            sent += result.accepted.size
            for (refusal in result.refused) {
                // A missing link stays queued, to pass once the record it points to is sent.
                if (refusal.reason == MissingLink) continue
                val id = refusal.id ?: continue
                versions[id]?.let { dao.refused(id, it, refusal.reason) }
                refused++
            }
        }
        return sent to refused
    }

    /** Fetches every page, then writes them in one transaction with the new cursor. Returns how many came. */
    private suspend fun pull(cursor: SyncCursorEntity): Int {
        val pulled = mutableListOf<RecordRows>()
        var after = cursor.changeSeq
        do {
            val page = api.pull(after, MaxBatch)
            pulled += page.records
            after = page.next
        } while (page.more)
        if (pulled.isNotEmpty() || dao.cursor() == null) dao.applyPulled(pulled, cursor.copy(changeSeq = after))
        return pulled.size
    }

    private companion object {
        /** The server's limit per push and per pull page (backend/api/sync.js). */
        const val MaxBatch = 500
        const val Invalid = "invalid"
        val Uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun isUuid(id: String) = Uuid.matches(id)
    }
}
