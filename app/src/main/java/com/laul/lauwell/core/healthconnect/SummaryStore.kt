package com.laul.lauwell.core.healthconnect

import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.record.DailySummary
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * The local copy of synced daily summaries that the screens read, kept up to date by [HealthSync].
 * Implementation: `core.data.RoomSummaryStore`. Health Connect stays the source of truth, so the
 * store can be wiped and rebuilt at any time.
 */
interface SummaryStore {

    /** The stored summaries of [metric] for [days], oldest first; updates whenever a sync writes. */
    fun summaries(metric: MetricId, days: ClosedRange<LocalDate>): Flow<List<DailySummary>>

    /** Replaces every stored day of [metric] in [days] with [summaries] (a day not in them is now empty). */
    suspend fun replace(metric: MetricId, days: ClosedRange<LocalDate>, summaries: List<DailySummary>)

    suspend fun syncState(metric: MetricId): SyncState?

    suspend fun saveSyncState(metric: MetricId, state: SyncState)

    /** When [metric] last synced, for the source line ("Synced 5 min ago"); null before the first sync. */
    fun lastSyncedAt(metric: MetricId): Flow<Instant?>
}

/**
 * Where a metric's sync stands: the Health Connect changes token to continue from, when it last
 * ran, and whether the window was filled [withHistory] (reading past data allowed). When that
 * permission changes, older days become readable or not with no change event, so the next sync
 * refreshes the whole window.
 */
data class SyncState(val changesToken: String, val syncedAt: Instant, val withHistory: Boolean = false)
