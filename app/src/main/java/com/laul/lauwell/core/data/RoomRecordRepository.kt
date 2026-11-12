package com.laul.lauwell.core.data

import com.laul.lauwell.core.auth.Session
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.RecordId
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.sync.EntrySyncSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID

/**
 * [RecordRepository] on the app's Room database: what you log is written on the phone first, then sent
 * to Postgres by [com.laul.lauwell.core.sync.EntrySync] ([sync] asks for a run after each change).
 *
 * Writes return at once and run on [scope]: logging never waits, even offline. The live [measurements]
 * see them as soon as they land. Shown only while signed in, and only to the account the local tables
 * belong to ([SyncCursorEntity.uid]). Never logs record contents.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomRecordRepository(
    private val dao: RecordDao,
    private val session: StateFlow<Session?>,
    private val scope: CoroutineScope,
    private val sync: EntrySyncSchedule,
    private val clock: Clock,
) : RecordRepository {

    /** A UUID, made on the phone so an entry can be created offline; the server only takes UUIDs. */
    override fun newId(): RecordId = RecordId(UUID.randomUUID().toString())

    override fun measurements(metric: MetricId): Flow<AppResult<List<Measurement>>> =
        combine(session, dao.cursorUpdates()) { current, cursor ->
            current?.uid?.takeIf { cursor == null || cursor.uid == it }
        }
            .distinctUntilChanged()
            .flatMapLatest { owner ->
                if (owner == null) flowOf(AppResult.Success(emptyList()))
                else dao.records(metric.value, Long.MIN_VALUE, Long.MAX_VALUE)
                    .map<List<RecordRows>, AppResult<List<Measurement>>> { rows -> AppResult.Success(rows.mapNotNull(::measurementFrom)) }
                    .catch { emit(AppResult.Error(it, "Couldn't read your ${metric.value} entries")) }
            }

    override fun save(record: Measurement): AppResult<Unit> = write { dao.save(toRows(record)) }

    override fun delete(id: RecordId): AppResult<Unit> = write { dao.markDeleted(id.value, clock.millis()) }

    private fun write(change: suspend () -> Unit): AppResult<Unit> {
        if (session.value == null) return AppResult.Error(IllegalStateException("Signed out"), "You're signed out")
        scope.launch {
            change()
            sync.request()
        }
        return AppResult.Success(Unit)
    }
}
