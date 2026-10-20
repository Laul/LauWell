package com.laul.lauwell.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.catalog.MetricId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Today's pinned grid holds at most this many tiles (3 × 2). */
const val MaxPinned = 6

/**
 * The default pinned set (PLAN.md → Decisions → App navigation): steps, resting heart rate, sleep,
 * fatigue, blood pressure, ostomy output. IDs of metrics not in the registry yet are kept, so their
 * tile appears on its own once the metric lands. A metric ID is the snake_case of its Notion name.
 */
val DefaultPinned: List<MetricId> = listOf(
    "steps", "resting_heart_rate", "sleep_duration", "fatigue", "blood_pressure", "ostomy_output_volume",
).map(::MetricId)

/**
 * The metrics Today shows as pinned tiles, in the user's order: those in [stored] that exist in
 * [visible] and [MetricDefinition.canPin], each once, at most [MaxPinned].
 */
fun pinnedTiles(stored: List<MetricId>, visible: List<MetricDefinition>): List<MetricDefinition> {
    val byId = visible.associateBy { it.id }
    return stored.distinct().mapNotNull { byId[it] }.filter { it.canPin }.take(MaxPinned)
}

/** The ordered metric IDs pinned on Today. A user list, not health data. */
interface PinnedMetrics {
    val pinned: Flow<List<MetricId>>
    suspend fun setPinned(ids: List<MetricId>)
}

/**
 * [PinnedMetrics] in the app's Preferences DataStore, as one comma-separated string. Nothing stored
 * means [DefaultPinned]; an empty string is a deliberately empty grid. Unreadable IDs are skipped.
 */
class DataStorePinnedMetrics(private val dataStore: DataStore<Preferences>) : PinnedMetrics {

    override val pinned: Flow<List<MetricId>> = dataStore.data.map { decodePinned(it[PinnedKey]) }

    override suspend fun setPinned(ids: List<MetricId>) {
        dataStore.edit { it[PinnedKey] = encodePinned(ids) }
    }

    private companion object {
        val PinnedKey = stringPreferencesKey("today_pinned")
    }
}

internal fun encodePinned(ids: List<MetricId>): String = ids.joinToString(",") { it.value }

internal fun decodePinned(stored: String?): List<MetricId> = when (stored) {
    null -> DefaultPinned
    "" -> emptyList()
    else -> stored.split(",").mapNotNull { runCatching { MetricId(it.trim()) }.getOrNull() }
}
