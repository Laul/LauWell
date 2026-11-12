package com.laul.lauwell.core.record

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The records you enter yourself (mood now; fatigue, blood pressure, medication… later), kept in
 * your account. Health Connect data never goes through here (Notion → Decisions Tracker: *Data
 * placement*). Implementation: `RoomRecordRepository` (core.data), synced to Postgres.
 */
interface RecordRepository {

    /** A fresh id for a record about to be [save]d. */
    fun newId(): RecordId

    /**
     * Every [metric] record, oldest first, updated live (including your own writes made offline).
     * Empty while signed out.
     */
    fun measurements(metric: MetricId): Flow<AppResult<List<Measurement>>>

    /** Creates or replaces [record]. Returns at once: the write lands on the phone, then syncs when online. */
    fun save(record: Measurement): AppResult<Unit>

    fun delete(id: RecordId): AppResult<Unit>
}
