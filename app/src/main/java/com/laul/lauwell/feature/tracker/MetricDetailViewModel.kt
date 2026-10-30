package com.laul.lauwell.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.catalog.MetricDefinition
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.healthconnect.HealthDataSource
import com.laul.lauwell.core.healthconnect.HealthSync
import com.laul.lauwell.core.healthconnect.SummaryStore
import com.laul.lauwell.core.healthconnect.dayBounds
import com.laul.lauwell.core.healthconnect.readPermissions
import com.laul.lauwell.core.record.DailySummary
import com.laul.lauwell.core.record.DataOrigin
import com.laul.lauwell.core.record.PeriodStats
import com.laul.lauwell.core.record.display
import com.laul.lauwell.core.common.AppResult
import com.laul.lauwell.core.record.Measurement
import com.laul.lauwell.core.record.RecordRepository
import com.laul.lauwell.core.healthconnect.SyncWindowDays
import com.laul.lauwell.core.summary.DayHistory
import com.laul.lauwell.feature.log.dayLabel
import com.laul.lauwell.core.summary.ago
import com.laul.lauwell.core.record.periodStats
import com.laul.lauwell.core.ui.chart.ChartSpec
import com.laul.lauwell.core.ui.chart.ChartPeriod
import com.laul.lauwell.core.ui.chart.chartSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Whether the detail screen can read the metric from Health Connect. */
sealed interface HealthAccess {
    data object Checking : HealthAccess

    /** The metric isn't read from Health Connect (logged in the app, or a document). */
    data object NotSynced : HealthAccess

    data class Unavailable(val updateRequired: Boolean) : HealthAccess

    /** [permissions] are what the grant prompt asks for. */
    data class NeedsPermission(val permissions: Set<String>) : HealthAccess

    data object Granted : HealthAccess
}

/**
 * The detail screen of a metric: its latest value, and a chart you scroll back through, grouped as
 * the selected period says (days, or weeks), with the stats, per-day list and entries of what is
 * on screen.
 */
data class MetricDetailState(
    val access: HealthAccess = HealthAccess.Checking,
    val period: ChartPeriod = ChartPeriod.Week,
    /** The latest day with data, up to today; its value is the screen's headline. */
    val latest: DailySummary? = null,
    val latestWhen: String? = null,
    val chart: ChartSpec? = null,
    val stats: PeriodStats? = null,
    /** The period's days, newest first. */
    val days: List<DayRow> = emptyList(),
    /** [latest]'s breakdown (a night's stages); empty without one. */
    val breakdown: List<BreakdownRow> = emptyList(),
    /** Where [latest]'s records come from, read from them: the daily aggregation gives no origins. */
    val origins: Set<DataOrigin> = emptySet(),
    val lastSyncedAt: Instant? = null,
    /** A logged metric's entries in the period, newest first (time, value, note); empty for a synced one. */
    val entries: List<Measurement> = emptyList(),
    /** The days on screen: what the stats, the day list and the entries cover. */
    val shownDays: ClosedRange<LocalDate>? = null,
)

/** How far back the chart goes ([from]), its grouping, and its first slot on screen (null: opening on today). */
private data class ChartView(val period: ChartPeriod, val from: LocalDate, val firstVisible: Int?)

/** Older days are read this many at a time as the chart scrolls back (one ~0.4 s Health Connect read for heart rate). */
private const val ChunkDays = 91L

/** How often the open screen asks Health Connect for changes. */
private const val RefreshMillis = 60_000L

class MetricDetailViewModel(
    private val metric: MetricDefinition,
    private val source: HealthDataSource,
    private val sync: HealthSync,
    store: SummaryStore,
    private val history: DayHistory,
    records: RecordRepository,
    clock: Clock,
) : ViewModel() {

    /** Your entries of a metric you log yourself; none for a synced one. */
    private val entries: Flow<List<Measurement>> =
        if (metric.isManual && metric.healthConnectTypes.isEmpty()) {
            records.measurements(metric.id).map { (it as? AppResult.Success)?.data.orEmpty() }
        } else {
            flowOf(emptyList())
        }

    private val access = MutableStateFlow<HealthAccess>(HealthAccess.Checking)

    private val period = MutableStateFlow(ChartPeriod.Week)

    /** The oldest day loaded so far: the cached sync window at first, older as the chart scrolls back. */
    private val from = MutableStateFlow(LocalDate.now(clock).minusDays(SyncWindowDays - 1))

    private val firstVisible = MutableStateFlow<Int?>(null)

    /** Where the data starts (null: none, or not readable); scrolling stops there. Read once. */
    private var firstDay: LocalDate? = null
    private var loadingOlder = false

    private val view = combine(period, from, firstVisible) { period, from, first -> ChartView(period, from, first) }

    /** Every day from [from] (plus one, in case the date changes while the screen is open). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val summaries = from.flatMapLatest { start ->
        history.days(metric, start..LocalDate.now(clock).plusDays(1))
    }

    /**
     * The origins of the latest day's records, read again only when that day's values change (a
     * sync that brought nothing new reads nothing). Empty without access, or if the read fails.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val origins = combine(access, summaries.map { latestOf(it, LocalDate.now(clock)) }) { access, latest ->
        latest.takeIf { access == HealthAccess.Granted }
    }.distinctUntilChanged().mapLatest { latest ->
        if (latest == null) return@mapLatest emptySet()
        val (from, until) = dayBounds(metric, latest.date, clock.zone)
        // A summary that names its apps (a night, read from one app) keeps to those apps' records.
        val apps = latest.origins.map { it.appPackage }.toSet()
        try {
            source.readMeasurements(metric, from, until)
                .mapNotNull { it.provenance.origin }
                .filter { apps.isEmpty() || it.appPackage in apps }
                .toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptySet<DataOrigin>()
        }
    }

    val state: StateFlow<MetricDetailState> = combine(
        access,
        view,
        summaries,
        store.lastSyncedAt(metric.id),
        origins,
    ) { access, view, summaries, syncedAt, origins ->
        val today = LocalDate.now(clock)
        val latest = latestOf(summaries, today)
        val chart = chartSpec(metric, summaries, view.period, today, from = view.from)
        val shown = chart?.visibleDays(view.firstVisible ?: chart.lastWindowStart) ?: view.period.days(today)
        MetricDetailState(
            access = access,
            period = view.period,
            latest = latest,
            latestWhen = latest?.let { ago(it.date, today, metric.nightly).replaceFirstChar { c -> c.uppercase() } },
            chart = chart,
            stats = metric.quantities.firstOrNull()?.let { periodStats(metric, summaries, shown) },
            days = dayRows(metric, summaries, view.period, today, shown),
            breakdown = latest?.let { breakdownRows(metric, it) }.orEmpty(),
            lastSyncedAt = syncedAt,
            origins = origins,
            shownDays = shown,
        )
    }.combine(entries) { state, entries ->
        val days = state.shownDays ?: state.period.days(LocalDate.now(clock))
        state.copy(entries = entries.filter { localDay(it, clock) in days }.sortedByDescending { it.time })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricDetailState())

    /** A new grouping opens on today again. */
    fun setPeriod(value: ChartPeriod) {
        period.value = value
        firstVisible.value = null
    }

    /** The chart scrolled: its first slot on screen is now [index]. Nearing the start loads older days. */
    fun onFirstVisibleChange(index: Int) {
        firstVisible.value = index
        if (index < period.value.visibleSlots) loadOlder()
    }

    /**
     * Reads the next [ChunkDays] older days, never before where the data starts. One read at a time;
     * a failed read (no access) leaves the chart as it is, and the next scroll tries again.
     */
    private fun loadOlder() {
        if (loadingOlder) return
        val start = from.value
        val first = firstDay ?: return
        if (!start.isAfter(first)) return
        loadingOlder = true
        viewModelScope.launch {
            try {
                val older = maxOf(start.minusDays(ChunkDays), first)
                if (history.load(metric, older) is AppResult.Success) from.value = older
            } finally {
                loadingOlder = false
            }
        }
    }

    init {
        viewModelScope.launch { firstDay = history.firstDay(metric) }
        refreshAccess()
        // While access is granted: sync now, then every minute. A change of access cancels the loop.
        viewModelScope.launch {
            access.collectLatest { current ->
                while (current == HealthAccess.Granted) {
                    sync.sync(listOf(metric))
                    delay(RefreshMillis)
                }
            }
        }
    }

    /** Re-reads availability and grants: on start, after the grant prompt, and when the screen resumes. */
    fun refreshAccess() {
        viewModelScope.launch {
            access.value = when {
                metric.healthConnectTypes.isEmpty() -> HealthAccess.NotSynced
                else -> when (source.availability()) {
                    HealthConnectAvailability.Unavailable -> HealthAccess.Unavailable(updateRequired = false)
                    HealthConnectAvailability.UpdateRequired -> HealthAccess.Unavailable(updateRequired = true)
                    HealthConnectAvailability.Available -> {
                        val needed = readPermissions(listOf(metric))
                        if (source.grantedPermissions().containsAll(needed)) HealthAccess.Granted
                        else HealthAccess.NeedsPermission(needed)
                    }
                }
            }
        }
    }
}

/**
 * The stats row: for a metric with a goal (steps), "Daily average · Best day · At goal" as in the
 * design ("Average · Longest · At goal" for a night); otherwise "Lowest · Average · Highest". A
 * value without data reads "—".
 */
internal fun statItems(metric: MetricDefinition, stats: PeriodStats): List<Pair<String, String>> {
    val quantity = metric.quantities.first()
    fun f(value: Double?) = value?.let { quantity.display(it) } ?: "—"
    return if (metric.goal != null) {
        listOf(
            (if (metric.nightly) "Average" else "Daily average") to f(stats.average),
            (if (metric.nightly) "Longest" else "Best day") to f(stats.highest?.value),
            "At goal" to "${stats.daysAtGoal ?: 0} of ${stats.daysWithData}",
        )
    } else {
        listOf("Lowest" to f(stats.lowest?.value), "Average" to f(stats.average), "Highest" to f(stats.highest?.value))
    }
}

/** The latest day with data, up to [today]. */
private fun latestOf(summaries: List<DailySummary>, today: LocalDate): DailySummary? =
    summaries.filter { it.date <= today }.maxByOrNull { it.date }

/** The local day [entry] was taken on: in its own offset, else the phone's. */
internal fun localDay(entry: Measurement, clock: Clock): LocalDate =
    entry.time.atZone(entry.zoneOffset ?: clock.zone).toLocalDate()

/** "Today · 21:00", "Yesterday · 08:15", "Mon 20 Oct · 21:00": when [entry] was taken, in its own offset. */
internal fun entryWhen(entry: Measurement, clock: Clock): String {
    val at = entry.time.atZone(entry.zoneOffset ?: clock.zone)
    return "${dayLabel(at.toLocalDate(), LocalDate.now(clock))} · ${at.format(EntryTime)}"
}

private val EntryTime = DateTimeFormatter.ofPattern("HH:mm")
