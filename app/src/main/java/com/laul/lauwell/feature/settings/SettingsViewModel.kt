package com.laul.lauwell.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.auth.AuthRepository
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.catalog.ToggleRepository
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.readPermissions
import com.laul.lauwell.core.settings.DisplayPreferences
import com.laul.lauwell.core.settings.ThemeMode
import com.laul.lauwell.core.sync.EntrySync
import com.laul.lauwell.core.sync.EntrySyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Whether Health Connect can be read here, and for how many of the visible synced metrics. */
sealed interface HealthConnectStatus {
    data class Unavailable(val updateRequired: Boolean) : HealthConnectStatus
    data class Available(val grantedMetrics: Int) : HealthConnectStatus
}

data class SettingsState(
    /** Null while checking. */
    val healthConnect: HealthConnectStatus? = null,
    /** The latest sync of any visible synced metric. */
    val lastSyncedAt: Instant? = null,
    /** Null until the stored choice is read. */
    val themeMode: ThemeMode? = null,
    val visibleMetrics: Int = 0,
    val email: String? = null,
    /** Where your logged entries stand with the server; null while reading. */
    val entries: EntrySyncStatus? = null,
    /** Set while signing out would lose this many changes not on the server yet: ask first. */
    val unsentOnSignOut: Int? = null,
)

/**
 * Settings (design 20b): the Health Connect status, the appearance choice, how many metrics are
 * shown, the signed-in account and where its entries stand with the server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val displayPreferences: DisplayPreferences,
    private val toggles: ToggleRepository,
    private val source: HealthDataSource,
    private val store: SummaryStore,
    private val entrySync: EntrySync,
) : ViewModel() {

    private val healthConnect = MutableStateFlow<HealthConnectStatus?>(null)

    private val unsentOnSignOut = MutableStateFlow<Int?>(null)

    private val visible = toggles.switchedOff.map { MetricRegistry.visible(it) }

    private val lastSyncedAt: Flow<Instant?> = visible.flatMapLatest { metrics ->
        val synced = metrics.filter { it.healthConnectTypes.isNotEmpty() }
        if (synced.isEmpty()) flowOf(null)
        else combine(synced.map { store.lastSyncedAt(it.id) }) { times -> times.filterNotNull().maxOrNull() }
    }

    val state: StateFlow<SettingsState> = combine(
        combine(healthConnect, lastSyncedAt, displayPreferences.themeMode, visible, ::Quad),
        authRepository.session,
        entrySync.status(),
        unsentOnSignOut,
    ) { (status, syncedAt, theme, metrics), session, entries, unsent ->
        SettingsState(status, syncedAt, theme, metrics.size, session?.email, entries, unsent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** Re-reads Health Connect's availability and grants: on start, and each time the screen resumes. */
    fun refresh() {
        viewModelScope.launch {
            healthConnect.value = when (source.availability()) {
                HealthConnectAvailability.Unavailable -> HealthConnectStatus.Unavailable(updateRequired = false)
                HealthConnectAvailability.UpdateRequired -> HealthConnectStatus.Unavailable(updateRequired = true)
                HealthConnectAvailability.Available -> {
                    val granted = source.grantedPermissions()
                    val synced = visible.first().filter { it.healthConnectTypes.isNotEmpty() }
                    HealthConnectStatus.Available(synced.count { granted.containsAll(readPermissions(listOf(it))) })
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { displayPreferences.setThemeMode(mode) }
    }

    /**
     * Signs out, then clears this phone's entries: they are in your account, and the next sign-in pulls
     * them back. When some changes haven't reached the server, asks first ([SettingsState.unsentOnSignOut]).
     */
    fun signOut() {
        viewModelScope.launch {
            val unsent = entrySync.unsent()
            if (unsent > 0) unsentOnSignOut.value = unsent else finishSignOut()
        }
    }

    /** Signs out after the warning, losing the changes not on the server. */
    fun signOutAnyway() {
        unsentOnSignOut.value = null
        viewModelScope.launch { finishSignOut() }
    }

    fun staySignedIn() {
        unsentOnSignOut.value = null
    }

    /** The session ends first, so a sync in flight writes nothing; [com.laul.lauwell.core.auth.ui.AuthGate] then shows sign-in. */
    private suspend fun finishSignOut() {
        authRepository.signOut()
        entrySync.forgetLocalEntries()
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/**
 * The "Your entries" row's detail line: "All synced · 09:12", "2 entries waiting to sync · last synced
 * 09:12", "1 entry couldn't be synced", or "Not synced yet".
 */
internal fun entrySyncDetail(status: EntrySyncStatus?, clock: Clock): String {
    if (status == null) return "Checking…"
    val parts = buildList {
        if (status.waiting > 0) add("${entries(status.waiting)} waiting to sync")
        if (status.refused > 0) add("${entries(status.refused)} couldn't be synced")
        if (isEmpty()) add(if (status.syncedAt != null) "All synced" else "Not synced yet")
    }
    val synced = status.syncedAt?.let { syncedLabel(it, clock) }
    return when {
        synced == null -> parts.joinToString(" · ")
        status.waiting + status.refused == 0 -> "${parts.single()} · $synced"
        else -> (parts + "last synced $synced").joinToString(" · ")
    }
}

private fun entries(count: Int) = if (count == 1) "1 entry" else "$count entries"

/** "09:12" today, "23 Oct" on an earlier day. */
private fun syncedLabel(at: Instant, clock: Clock): String {
    val synced = at.atZone(clock.zone)
    val format = if (synced.toLocalDate() == ZonedDateTime.now(clock).toLocalDate()) TimeFormat else DayFormat
    return synced.format(format)
}

/**
 * The Health Connect row's detail line: "Connected · 5 metrics · synced 09:12" (an earlier day reads
 * "synced 23 Oct"), "Not connected", or why it can't be used.
 */
internal fun healthConnectDetail(status: HealthConnectStatus?, lastSyncedAt: Instant?, clock: Clock): String =
    when (status) {
        null -> "Checking…"
        is HealthConnectStatus.Unavailable ->
            if (status.updateRequired) "Needs to be installed or updated" else "Not available on this phone"
        is HealthConnectStatus.Available -> when (status.grantedMetrics) {
            0 -> "Not connected"
            else -> buildString {
                append("Connected · ${status.grantedMetrics} ${if (status.grantedMetrics == 1) "metric" else "metrics"}")
                if (lastSyncedAt != null) append(" · synced ${syncedLabel(lastSyncedAt, clock)}")
            }
        }
    }

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
// English month names: the line around them is English (as in DayList).
private val DayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
