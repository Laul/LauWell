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
)

/**
 * Settings (design 20b): the Health Connect status, the appearance choice, how many metrics are
 * shown, and the signed-in account.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val displayPreferences: DisplayPreferences,
    private val toggles: ToggleRepository,
    private val source: HealthDataSource,
    private val store: SummaryStore,
) : ViewModel() {

    private val healthConnect = MutableStateFlow<HealthConnectStatus?>(null)

    private val visible = toggles.switchedOff.map { MetricRegistry.visible(it) }

    private val lastSyncedAt: Flow<Instant?> = visible.flatMapLatest { metrics ->
        val synced = metrics.filter { it.healthConnectTypes.isNotEmpty() }
        if (synced.isEmpty()) flowOf(null)
        else combine(synced.map { store.lastSyncedAt(it.id) }) { times -> times.filterNotNull().maxOrNull() }
    }

    val state: StateFlow<SettingsState> = combine(
        healthConnect,
        lastSyncedAt,
        displayPreferences.themeMode,
        visible,
        authRepository.session,
    ) { status, syncedAt, theme, metrics, session ->
        SettingsState(status, syncedAt, theme, metrics.size, session?.email)
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

    /** The session ends and [com.laul.lauwell.core.auth.ui.AuthGate] shows sign-in on its own. */
    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
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
                if (lastSyncedAt != null) {
                    val now = ZonedDateTime.now(clock)
                    val synced = lastSyncedAt.atZone(clock.zone)
                    val format = if (synced.toLocalDate() == now.toLocalDate()) TimeFormat else DayFormat
                    append(" · synced ${synced.format(format)}")
                }
            }
        }
    }

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
// English month names: the line around them is English (as in DayList).
private val DayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
